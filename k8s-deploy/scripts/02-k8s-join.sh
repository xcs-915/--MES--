#!/bin/bash
# ==============================================================================
# 脚本名称: 02-k8s-join.sh
# 功能描述: K8s 工作节点加入脚本模板
#           用于后续添加新节点到已有 K8s 集群
# 适用系统: CentOS 8.x
# 执行用户: root
# 使用方法: bash 02-k8s-join.sh
# ==============================================================================
set -e

echo "=========================================="
echo "  K8s 工作节点加入集群"
echo "=========================================="

# ----------------------------------------------------------------------------
# 配置说明
# ----------------------------------------------------------------------------
# 使用前请确保:
#   1. 新节点已执行 00-system-prep.sh 完成系统准备 (firewalld 关闭、swap 关闭等)
#   2. 新节点已安装 Docker CE 和 kubeadm/kubelet/kubectl
#   3. 新节点与 Master 节点 (10.30.10.140) 网络互通
#   4. join-command.sh 文件存在且包含有效的 join 命令
#
# 获取最新 join 命令的方法 (在 Master 节点执行):
#   kubeadm token create --print-join-command
#
# 注意:
#   - join token 默认有效期为 24 小时，过期需在 Master 上重新生成
#   - 如果需要获取控制平面 (master) 节点的 join 命令，需在 Master 上执行:
#     kubeadm init phase upload-certs --upload-certs
#     然后使用 kubeadm join --control-plane 参数加入
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# 0. 前置检查
# ----------------------------------------------------------------------------
echo "[步骤 0] 前置检查..."

# 检查是否为 root 用户
if [ "$(id -u)" -ne 0 ]; then
    echo "错误: 请使用 root 用户执行此脚本"
    exit 1
fi

# 检查 Docker 是否运行
if ! systemctl is-active --quiet docker; then
    echo "错误: Docker 未运行，请先执行 00-system-prep.sh"
    exit 1
fi
echo "  Docker 运行中: 通过"

# 检查 kubelet 是否已安装
if ! command -v kubeadm &> /dev/null; then
    echo "错误: kubeadm 未安装，请先执行 00-system-prep.sh"
    exit 1
fi
echo "  kubeadm 已安装: 通过"

# 检查 swap 是否已关闭
if swapon --show | grep -q swap; then
    echo "警告: swap 未关闭，正在临时关闭..."
    swapoff -a
    sed -i '/swap/s/^/# /' /etc/fstab
    echo "  swap 已关闭"
fi

# ----------------------------------------------------------------------------
# 1. 获取 join 命令
# ----------------------------------------------------------------------------
echo "[步骤 1] 获取 join 命令..."

# join 命令文件路径
# 方式 1: 从同目录下的 join-command.sh 文件读取 (推荐)
# 方式 2: 直接在本脚本中内联 join 命令
# 方式 3: 通过参数传入 join 命令

JOIN_CMD_FILE="$(cd "$(dirname "$0")" && pwd)/join-command.sh"

# 定义 join 命令变量
# 当从 Master 节点获取后，请将 join 命令粘贴到下面的变量中
# 或者将 Master 节点生成的 join-command.sh 复制到本脚本同目录
JOIN_COMMAND=""

# 尝试从 join-command.sh 文件读取
if [ -f "${JOIN_CMD_FILE}" ]; then
    echo "  发现 join 命令文件: ${JOIN_CMD_FILE}"
    JOIN_COMMAND=$(cat "${JOIN_CMD_FILE}")
    echo "  join 命令已从文件读取"
elif [ -n "$1" ]; then
    # 通过命令行参数传入 join 命令
    JOIN_COMMAND="$1"
    echo "  join 命令已从参数读取"
else
    echo "  =================================================="
    echo "  未找到 join 命令!"
    echo "  =================================================="
    echo ""
    echo "  请使用以下任一方式提供 join 命令:"
    echo ""
    echo "  方式 1: 从 Master 节点复制 join 命令文件"
    echo "    在 Master 节点 (10.30.10.140) 执行:"
    echo "      scp /root/k8s-deploy/join-command.sh root@<新节点IP>:/root/k8s-deploy/scripts/"
    echo "    然后在本节点执行:"
    echo "      bash 02-k8s-join.sh"
    echo ""
    echo "  方式 2: 通过参数传入 join 命令"
    echo "    在 Master 节点获取 join 命令:"
    echo "      kubeadm token create --print-join-command"
    echo "    然后在本节点执行:"
    echo "      bash 02-k8s-join.sh 'kubeadm join 10.30.10.140:6443 --token xxx --discovery-token-ca-cert-hash xxx'"
    echo ""
    echo "  方式 3: 手动编辑本脚本，在下方 JOIN_COMMAND 变量中填入 join 命令"
    echo ""
    exit 1
fi

# 验证 join 命令不为空
if [ -z "${JOIN_COMMAND}" ]; then
    echo "错误: join 命令为空"
    exit 1
fi

echo "  join 命令: ${JOIN_COMMAND}"

# ----------------------------------------------------------------------------
# 2. 预拉取所需镜像
# ----------------------------------------------------------------------------
echo "[步骤 2] 预拉取 kube-proxy 和 pause 镜像..."

# 配置阿里云镜像源并拉取
docker pull registry.cn-hangzhou.aliyuncs.com/google_containers/kube-proxy:v1.28.0
docker pull registry.cn-hangzhou.aliyuncs.com/google_containers/pause:3.9

# 使用 kubeadm 配置拉取
cat > /tmp/kubeadm-join-images.yaml <<'EOF'
apiVersion: kubeadm.k8s.io/v1beta3
kind: JoinConfiguration
imageRepository: registry.cn-hangzhou.aliyuncs.com/google_containers
EOF

kubeadm config images pull --config /tmp/kubeadm-join-images.yaml 2>/dev/null || true
echo "  镜像预拉取完成"

# ----------------------------------------------------------------------------
# 3. 执行 join 命令加入集群
# ----------------------------------------------------------------------------
echo "[步骤 3] 执行 join 命令加入集群..."

# 执行 join 命令
eval ${JOIN_COMMAND} --ignore-preflight-errors=SystemVerification,NumCPU,Mem

echo "  节点已加入集群"

# ----------------------------------------------------------------------------
# 4. 启动 kubelet
# ----------------------------------------------------------------------------
echo "[步骤 4] 启动 kubelet..."
systemctl enable kubelet
systemctl start kubelet || true
echo "  kubelet 已启动并设置为开机自启"

# ----------------------------------------------------------------------------
# 5. 验证节点状态
# ----------------------------------------------------------------------------
echo "[步骤 5] 验证节点加入状态..."

# 等待节点就绪
echo "  等待节点状态更新 (最多 120 秒)..."
sleep 10

# 检查 kubelet 是否正常运行
if systemctl is-active --quiet kubelet; then
    echo "  kubelet 状态: 运行中"
else
    echo "  警告: kubelet 未正常运行，请检查日志: journalctl -u kubelet -f"
fi

# 显示本节点信息
NODE_NAME=$(hostname)
echo ""
echo "  本节点信息:"
echo "    节点名: ${NODE_NAME}"
echo "    IP 地址: $(hostname -I | awk '{print $1}')"
echo ""
echo "  请在 Master 节点 (10.30.10.140) 验证节点加入:"
echo "    kubectl get nodes"
echo ""

# ----------------------------------------------------------------------------
# 完成提示
# ----------------------------------------------------------------------------
echo "=========================================="
echo "  工作节点加入完成!"
echo "=========================================="
echo ""
echo "后续操作 (在 Master 节点执行):"
echo "  1. 查看所有节点: kubectl get nodes -o wide"
echo "  2. 查看 Pod 状态: kubectl get pods -A -o wide"
echo "  3. 查看节点详情: kubectl describe node ${NODE_NAME}"
echo ""
echo "常见问题排查:"
echo "  - 节点状态为 NotReady: 检查 Calico CNI 是否已在该节点部署"
echo "    kubectl get pods -n kube-system -o wide | grep ${NODE_NAME}"
echo "  - Pod 一直 ContainerCreating: 检查镜像是否拉取成功"
echo "    crictl ps -a | grep -v Running"
echo "  - kubelet 日志: journalctl -u kubelet -f --no-pager | tail -50"
echo ""
