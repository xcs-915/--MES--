#!/bin/bash
# =====================================================================
# TNS-MES Phase 3 高可用部署主脚本
# 服务器: 10.30.10.140 (CentOS 8, 16核, 48GB)
# 数据库: 10.30.10.141 (Windows Server 2019 + SQL Server 2019)
# =====================================================================
set -e

# ====== 颜色定义 ======
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

# ====== 路径变量 ======
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MANIFEST_DIR="${SCRIPT_DIR}/../manifests"
LOG_FILE="/var/log/tns-mes-deploy.log"

# ====== 日志函数 ======
log() { echo -e "${CYAN}[$(date '+%H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"; }
ok()  { echo -e "${GREEN}[✓]${NC} $1" | tee -a "$LOG_FILE"; }
warn(){ echo -e "${YELLOW}[!]${NC} $1" | tee -a "$LOG_FILE"; }
err() { echo -e "${RED}[✗]${NC} $1" | tee -a "$LOG_FILE"; exit 1; }

# ====== 检查 root 权限 ======
if [ "$EUID" -ne 0 ]; then
  err "请以 root 用户执行此脚本"
fi

echo ""
echo "================================================"
echo "  TNS-MES Phase 3 高可用 K8s 部署"
echo "  应用服务器: 10.30.10.140 (CentOS 8)"
echo "  数据库服务器: 10.30.10.141 (SQL Server 2019)"
echo "================================================"
echo ""

# ====== Step 0: 前置检查 ======
log "Step 0: 前置检查..."

# 检查 Docker
if ! command -v docker &>/dev/null; then
  err "Docker 未安装，请先执行: bash 00-system-prep.sh && reboot"
fi
ok "Docker 已安装: $(docker --version)"

# 检查 K8s
if ! command -v kubectl &>/dev/null; then
  err "kubectl 未安装，请先执行: bash 01-k8s-init.sh"
fi

# 检查 K8s 集群状态
if ! kubectl get nodes &>/dev/null; then
  err "K8s 集群未初始化，请先执行: bash 01-k8s-init.sh"
fi
ok "K8s 集群就绪: $(kubectl get nodes -o wide | tail -1)"

# 检查 SQL Server 连通性
if ! timeout 3 bash -c 'echo > /dev/tcp/10.30.10.141/1433' 2>/dev/null; then
  err "无法连接 SQL Server (10.30.10.141:1433)，请先在 Windows 服务器上执行 03-init-sqlserver.ps1"
fi
ok "SQL Server 可达: 10.30.10.141:1433"

echo ""
log "开始部署 K8s 资源..."
echo ""

# ====== Step 1: 创建命名空间 ======
log "Step 1: 创建命名空间..."
kubectl apply -f "${MANIFEST_DIR}/00-namespace.yaml"
ok "命名空间创建完成"
echo ""

# ====== Step 2: 创建配置和密钥 ======
log "Step 2: 创建 ConfigMap 和 Secret..."
kubectl apply -f "${MANIFEST_DIR}/01-config-secrets.yaml"
ok "配置和密钥创建完成"
echo ""

# ====== Step 3: 部署 Nacos 集群 ======
log "Step 3: 部署 Nacos 集群 (3 副本)..."
kubectl apply -f "${MANIFEST_DIR}/02-nacos-cluster.yaml"
log "等待 Nacos Pod 就绪..."
kubectl wait --for=condition=Ready pod -l app=nacos -n tns-mes-middleware --timeout=300s || warn "Nacos 启动较慢，请稍后检查"
ok "Nacos 集群部署完成"
echo ""

# ====== Step 4: 部署 Redis 集群 ======
log "Step 4: 部署 Redis 集群 (3主3从)..."
kubectl apply -f "${MANIFEST_DIR}/03-redis-cluster.yaml"
log "等待 Redis Pod 就绪..."
kubectl wait --for=condition=Ready pod -l app=redis-node -n tns-mes-middleware --timeout=180s || warn "Redis 启动中..."
ok "Redis 集群部署完成"
echo ""

# ====== Step 5: 部署 RocketMQ ======
log "Step 5: 部署 RocketMQ (2 NameServer + 2 Broker)..."
kubectl apply -f "${MANIFEST_DIR}/04-rocketmq.yaml"
log "等待 RocketMQ NameServer 就绪..."
kubectl wait --for=condition=Ready pod -l app=rocketmq-namesrv -n tns-mes-middleware --timeout=180s || warn "RocketMQ 启动中..."
ok "RocketMQ 部署完成"
echo ""

# ====== Step 6: 部署 Sentinel + Seata ======
log "Step 6: 部署 Sentinel Dashboard + Seata TC Server..."
kubectl apply -f "${MANIFEST_DIR}/05-sentinel-seata.yaml"
kubectl wait --for=condition=Ready pod -l app=sentinel-dashboard -n tns-mes-middleware --timeout=120s || warn "Sentinel 启动中..."
kubectl wait --for=condition=Ready pod -l app=seata-server -n tns-mes-middleware --timeout=120s || warn "Seata 启动中..."
ok "Sentinel + Seata 部署完成"
echo ""

# ====== Step 7: 部署可观测层 ======
log "Step 7: 部署 SkyWalking + Prometheus + Grafana..."
kubectl apply -f "${MANIFEST_DIR}/10-skywalking.yaml"
kubectl apply -f "${MANIFEST_DIR}/11-prometheus-grafana.yaml"
kubectl wait --for=condition=Ready pod -l app=skywalking-oap -n tns-mes-observability --timeout=300s || warn "SkyWalking 启动中..."
ok "可观测层部署完成"
echo ""

# ====== Step 8: 部署前端 Nginx 配置 ======
log "Step 8: 部署前端 Nginx 配置..."
kubectl apply -f "${MANIFEST_DIR}/12-frontend-nginx-config.yaml"
ok "前端 Nginx 配置完成"
echo ""

# ====== Step 9: 部署微服务 ======
log "Step 9: 部署微服务 (6 服务 × 2 副本)..."
kubectl apply -f "${MANIFEST_DIR}/07-microservices.yaml"
log "等待微服务 Pod 就绪 (可能需要 2-5 分钟)..."
kubectl wait --for=condition=Ready pod -l app=tns-auth-service -n tns-mes --timeout=300s || warn "Auth Service 启动中..."
kubectl wait --for=condition=Ready pod -l app=tns-product-service -n tns-mes --timeout=300s || warn "Product Service 启动中..."
ok "微服务部署完成"
echo ""

# ====== Step 10: 部署网关 ======
log "Step 10: 部署 SpringCloud Gateway..."
kubectl apply -f "${MANIFEST_DIR}/06-gateway.yaml"
kubectl wait --for=condition=Ready pod -l app=sc-gateway -n tns-mes --timeout=180s || warn "Gateway 启动中..."
ok "网关部署完成"
echo ""

# ====== Step 11: 部署前端 ======
log "Step 11: 部署 Vue3 前端..."
kubectl apply -f "${MANIFEST_DIR}/08-frontend.yaml"
kubectl wait --for=condition=Ready pod -l app=tns-frontend -n tns-mes --timeout=120s || warn "Frontend 启动中..."
ok "前端部署完成"
echo ""

# ====== Step 12: 部署 Ingress ======
log "Step 12: 部署 Nginx Ingress 路由..."
kubectl apply -f "${MANIFEST_DIR}/09-ingress.yaml"
ok "Ingress 路由部署完成"
echo ""

# ====== 部署完成汇总 ======
echo ""
echo "================================================"
echo -e "${GREEN}  TNS-MES Phase 3 部署完成!${NC}"
echo "================================================"
echo ""
echo "访问地址:"
echo "  前端:        http://10.30.10.140/"
echo "  API 网关:    http://10.30.10.140:31080/"
echo "  Nacos:       http://10.30.10.140:8848/nacos"
echo "  Sentinel:    http://10.30.10.140:8080/"
echo "  SkyWalking:  http://10.30.10.140:8080/"
echo "  Grafana:     http://10.30.10.140:3000/ (admin / Taiking@5563)"
echo "  Prometheus:  http://10.30.10.140:9090/"
echo ""
echo "数据库:"
echo "  SQL Server:  10.30.10.141:1433"
echo "  数据库:      tns_mes"
echo "  用户:        tns_mes_user / Taiking@5563"
echo ""
echo "SAP 正式环境:"
echo "  Base URL:    https://my200725.s4hana.sapcloud.cn"
echo "  用户:        MES_P"
echo ""
echo "集群状态:"
kubectl get nodes -o wide
echo ""
kubectl get pods -A -o wide
echo ""
echo "日志文件: $LOG_FILE"
echo "================================================"
