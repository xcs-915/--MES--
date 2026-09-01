#!/bin/bash
# =====================================================================
# TNS-MES 部署更新脚本 (模块化单体 v2)
# 在 Linux 服务器 10.30.10.140 上执行
# 功能: 构建镜像 → 更新 ConfigMap → 更新 Deployment → 验证
# =====================================================================
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

DEPLOY_DIR="/opt/tns-mes-deploy"
log() { echo -e "${CYAN}[$(date '+%H:%M:%S')]${NC} $1"; }
ok()  { echo -e "${GREEN}[OK]${NC} $1"; }
warn(){ echo -e "${YELLOW}[!]${NC} $1"; }
err() { echo -e "${RED}[FAIL]${NC} $1"; exit 1; }

echo ""
echo "================================================"
echo "  TNS-MES 部署更新 (模块化单体 v2)"
echo "  服务器: 10.30.10.140 (k8s-master)"
echo "================================================"
echo ""

# ====== Step 1: 构建Docker镜像 ======
IMAGE_TAG="${IMAGE_TAG:-v23fix}"
log "Step 1: 构建 Docker 镜像 ${IMAGE_TAG}..."
cd ${DEPLOY_DIR}
docker build --no-cache -t tns-mes-server:${IMAGE_TAG} . 2>&1 | tail -5
ok "Docker 镜像构建完成: $(docker images tns-mes-server:${IMAGE_TAG} --format '{{.ID}} {{.Size}}')"
echo ""

# ====== Step 1.5: 导入镜像到 containerd ======
# 说明: K8s 使用 containerd 运行时, Docker 构建的镜像需要导入 containerd 的 k8s.io 命名空间
# 否则 Pod 会报 ErrImageNeverPull 错误
log "Step 1.5: 导入 Docker 镜像到 containerd..."
docker save tns-mes-server:${IMAGE_TAG} | ctr -n k8s.io images import - 2>&1
ok "镜像已导入 containerd k8s.io 命名空间"
echo ""

# ====== Step 2: 更新 ConfigMap + Secret ======
log "Step 2: 更新 ConfigMap 和 Secret..."
kubectl apply -f ${DEPLOY_DIR}/01-config-only.yaml
ok "ConfigMap 更新完成"
kubectl apply -f ${DEPLOY_DIR}/01-platform-secrets.yaml
ok "中间件和监控 Secret 更新完成"
# 同步应用 Secret (确保 jwt-secret, db-password, redis-password 等注入到 Pod)
kubectl apply -f ${DEPLOY_DIR}/01-config-secrets.yaml
ok "Secret 更新完成"
# 验证 Secret 是否包含必要 key
log "验证 Secret key..."
kubectl get secret tns-mes-secrets -n tns-mes -o jsonpath='{.data}' 2>/dev/null | grep -o '"[a-z-]*"' | head -10
echo ""
echo ""

# ====== Step 3: 更新 Deployment + HPA + PDB ======
log "Step 3: 应用更新后的 Deployment 清单..."
kubectl apply -f ${DEPLOY_DIR}/12-backend-deploy.yaml
ok "Deployment/Service/PDB/HPA 更新完成"
echo ""

# ====== Step 4: 触发滚动更新 ======
log "Step 4: 触发滚动更新 (rollout restart)..."
kubectl rollout restart deployment/tns-mes-backend -n tns-mes
log "等待滚动更新完成 (最多 5 分钟)..."
kubectl rollout status deployment/tns-mes-backend -n tns-mes --timeout=300s
ok "滚动更新完成"
echo ""

# ====== Step 5: 验证 Pod 状态 ======
log "Step 5: 验证 Pod 状态..."
kubectl get pods -n tns-mes -l app=tns-mes-backend -o wide
echo ""

# ====== Step 6: 验证 Flyway 迁移 ======
log "Step 6: 检查 Flyway 迁移日志..."
# 取最新的 Pod 名称
POD=$(kubectl get pods -n tns-mes -l app=tns-mes-backend -o jsonpath="{.items[0].metadata.name}")
log "检查 Pod: ${POD}"
echo "--- Flyway 迁移日志 ---"
kubectl logs ${POD} -n tns-mes 2>&1 | grep -i "flyway\|migration\|V23\|V22\|Successfully" | tail -15
echo ""

# ====== Step 7: 验证健康检查 ======
log "Step 7: 验证应用健康状态..."
sleep 5
HEALTH=$(kubectl exec ${POD} -n tns-mes -- wget -q -O - http://localhost:8080/tns-mes/actuator/health 2>/dev/null || echo "HEALTH_CHECK_FAILED")
echo "${HEALTH}" | head -5
echo ""

# ====== Step 8: 最终状态汇总 ======
echo "================================================"
echo -e "${GREEN}  TNS-MES 部署更新完成!${NC}"
echo "================================================"
echo ""
echo "访问地址:"
echo "  MES 系统:  http://10.30.10.140:31180/tns-mes/"
echo "  健康检查:  http://10.30.10.140:31180/tns-mes/actuator/health"
echo ""
echo "集群状态:"
kubectl get pods -n tns-mes -o wide
echo ""
kubectl get pods -n tns-mes-middleware -o wide
echo ""
echo "================================================"
