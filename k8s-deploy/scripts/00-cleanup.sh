#!/usr/bin/env bash
# ==============================================================================
# 00-cleanup.sh - 清理 K8s 集群中不需要的资源 (单体模式迁移)
# ==============================================================================
# [架构说明] 当前为单体模式: 后端 tns-mes-server (NodePort 31180) 统一提供前端页面与 API, 不再使用 Gateway/微服务/独立前端 Pod
# --------------------------------------------------------------------------------
# 用途: 在应用单体模式部署清单 (12-backend-deploy.yaml) 前, 清理旧的微服务架构遗留资源
#   1. 删除独立前端 Deployment / Service / PDB (tns-frontend*)
#   2. 删除 SpringCloud Gateway Deployment / Service / PDB (sc-gateway*)
#   3. 缩减 6 个微服务 Deployment 副本数到 0 (保留定义, 停止运行)
# 用法: bash scripts/00-cleanup.sh
# 注意: 本脚本仅清理 tns-mes 命名空间下的应用层资源, 不动中间件/可观测性资源
# ==============================================================================
set -euo pipefail

# 应用命名空间
NS_APP="tns-mes"

echo "============================================================"
echo " TNS-MES 微服务架构遗留资源清理 (命名空间: ${NS_APP})"
echo " 目标: 移除 Gateway / 独立前端, 缩减微服务到 0 副本"
echo "============================================================"

# ---------- 1. 删除独立前端资源 ----------
echo "[1/3] 删除独立前端 (tns-frontend) Deployment / Service / PDB ..."
kubectl -n "${NS_APP}" delete deployment tns-frontend --ignore-not-found
kubectl -n "${NS_APP}" delete service tns-frontend-svc --ignore-not-found
kubectl -n "${NS_APP}" delete pdb tns-frontend-pdb --ignore-not-found

# ---------- 2. 删除 SpringCloud Gateway 资源 ----------
echo "[2/3] 删除 SpringCloud Gateway (sc-gateway) Deployment / Service / PDB ..."
kubectl -n "${NS_APP}" delete deployment sc-gateway --ignore-not-found
kubectl -n "${NS_APP}" delete service sc-gateway-svc --ignore-not-found
kubectl -n "${NS_APP}" delete pdb sc-gateway-pdb --ignore-not-found

# ---------- 3. 缩减微服务副本数到 0 ----------
# 说明: 仅缩容到 0 副本以停止运行, 保留 Deployment 定义便于回滚;
#       如需彻底删除, 可改用 kubectl delete deployment <name>
echo "[3/3] 缩减 6 个微服务副本数到 0 ..."
for svc in tns-auth-service tns-product-service tns-workorder-service \
           tns-quality-service tns-integration-service tns-basic-data-service; do
  echo "  - scale ${svc} -> 0"
  kubectl -n "${NS_APP}" scale deployment "${svc}" --replicas=0 \
    || echo "    (警告: ${svc} 不存在或缩容失败, 已跳过)"
done

echo "============================================================"
echo " 清理完成。"
echo " 后续步骤:"
echo "   kubectl apply -f manifests/12-backend-deploy.yaml   # 部署单体后端"
echo "============================================================"
