#!/bin/bash
# ==============================================================================
# 脚本名称: 01-k8s-init.sh
# 功能描述: K8s 集群初始化脚本
#           使用 kubeadm init 初始化单节点 K8s 集群 (master 同时作为 worker)
#           安装 Calico CNI 和 local-path storage provisioner
# 适用系统: CentOS 8.3
# 执行用户: root (需在执行 00-system-prep.sh 并重启后运行)
# 使用方法: bash 01-k8s-init.sh
# ==============================================================================
set -e

echo "=========================================="
echo "  K8s 集群初始化 - 单节点模式"
echo "=========================================="

# ----------------------------------------------------------------------------
# 0. 前置检查
# ----------------------------------------------------------------------------
echo "[步骤 0] 前置检查..."

# 检查是否为 root 用户
if [ "$(id -u)" -ne 0 ]; then
    echo "错误: 请使用 root 用户执行此脚本"
    exit 1
fi

# 检查 swap 是否已关闭
if swapon --show | grep -q swap; then
    echo "错误: swap 未关闭，请先执行 00-system-prep.sh"
    exit 1
fi
echo "  swap 已关闭: 通过"

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

# 定义变量
MASTER_IP="10.30.10.140"
POD_CIDR="10.244.0.0/16"
SERVICE_CIDR="10.96.0.0/12"
NODE_NAME=$(hostname)
K8S_USER="taiking"

echo "  Master IP: ${MASTER_IP}"
echo "  Pod CIDR: ${POD_CIDR}"
echo "  Service CIDR: ${SERVICE_CIDR}"
echo "  Node Name: ${NODE_NAME}"

# ----------------------------------------------------------------------------
# 1. 预拉取 K8s 镜像
# ----------------------------------------------------------------------------
echo "[步骤 1] 预拉取 K8s 组件镜像..."

# 修改 kubeadm 配置文件中的镜像源为阿里云
cat > /tmp/kubeadm-config-images.yaml <<EOF
apiVersion: kubeadm.k8s.io/v1beta3
kind: InitConfiguration
imageRepository: registry.cn-hangzhou.aliyuncs.com/google_containers
EOF

# 拉取所需镜像
kubeadm config images pull --config /tmp/kubeadm-config-images.yaml
echo "  K8s 镜像拉取完成"

# 列出已拉取的镜像
echo "  已拉取镜像列表:"
docker images | grep -E "kube-apiserver|kube-controller-manager|kube-scheduler|kube-proxy|etcd|coredns|pause"

# ----------------------------------------------------------------------------
# 2. 生成 kubeadm 初始化配置文件
# ----------------------------------------------------------------------------
echo "[步骤 2] 生成 kubeadm 初始化配置文件..."

cat > /tmp/kubeadm-init.yaml <<EOF
apiVersion: kubeadm.k8s.io/v1beta3
kind: InitConfiguration
# 镜像仓库使用阿里云镜像源
imageRepository: registry.cn-hangzhou.aliyuncs.com/google_containers
# kubelet 配置
nodeRegistration:
  criSocket: unix:///var/run/containerd/containerd.sock
  name: ${NODE_NAME}
  kubeletExtraArgs:
    cgroup-driver: systemd

---
apiVersion: kubeadm.k8s.io/v1beta3
kind: ClusterConfiguration
# 控制平面 API 服务器地址
controlPlaneEndpoint: "${MASTER_IP}:6443"
# 网络插件 CIDR (Calico 兼容)
networking:
  podSubnet: "${POD_CIDR}"
  serviceSubnet: "${SERVICE_CIDR}"
# Kubernetes 版本
kubernetesVersion: "v1.28.2"
# 镜像仓库
imageRepository: registry.cn-hangzhou.aliyuncs.com/google_containers
# API Server 额外参数
apiServer:
  extraArgs:
    authorization-mode: "Node,RBAC"
    service-node-port-range: "30000-32767"
# 调度器配置
scheduler:
  extraArgs:
    bind-address: "0.0.0.0"

---
apiVersion: kubeadm.k8s.io/v1beta3
kind: KubeletConfiguration
cgroupDriver: systemd
clusterDNS:
  - 10.96.0.10
clusterDomain: "cluster.local"
failSwapOn: false
resolvConf: "/etc/resolv.conf"

---
apiVersion: kubeadm.k8s.io/v1beta3
kind: KubeProxyConfiguration
mode: "ipvs"
ipvs:
  scheduler: "rr"
EOF

echo "  kubeadm 配置文件已生成: /tmp/kubeadm-init.yaml"

# ----------------------------------------------------------------------------
# 3. 执行 kubeadm init 初始化集群
# ----------------------------------------------------------------------------
echo "[步骤 3] 执行 kubeadm init 初始化集群..."

# 使用 --ignore-preflight-errors 忽略部分预检错误（如 SystemVerification 等）
kubeadm init \
    --config /tmp/kubeadm-init.yaml \
    --ignore-preflight-errors=SystemVerification,NumCPU,Mem

echo "  K8s 集群初始化完成"

# ----------------------------------------------------------------------------
# 4. 配置 kubectl 命令行工具
# ----------------------------------------------------------------------------
echo "[步骤 4] 配置 kubectl for user ${K8S_USER}..."

# 创建用户目录
USER_HOME=$(eval echo ~${K8S_USER})
mkdir -p ${USER_HOME}/.kube

# 复制 admin kubeconfig 文件
cp /etc/kubernetes/admin.conf ${USER_HOME}/.kube/config
chown ${K8S_USER}:${K8S_USER} ${USER_HOME}/.kube/config
chmod 600 ${USER_HOME}/.kube/config

# 同时为 root 用户配置 kubectl
mkdir -p /root/.kube
cp /etc/kubernetes/admin.conf /root/.kube/config
chmod 600 /root/.kube/config

echo "  kubectl 已配置: ${K8S_USER} 用户和 root 用户"

# 配置 kubectl 自动补全
echo 'source <(kubectl completion bash)' >> ${USER_HOME}/.bashrc
echo 'alias k=kubectl' >> ${USER_HOME}/.bashrc
echo 'source <(kubectl completion bash)' >> /root/.bashrc
echo 'alias k=kubectl' >> /root/.bashrc
echo "  kubectl 自动补全已配置"

# 设置 KUBECONFIG 环境变量
echo 'export KUBECONFIG=${HOME}/.kube/config' >> ${USER_HOME}/.bashrc
echo 'export KUBECONFIG=/root/.kube/config' >> /root/.bashrc

# ----------------------------------------------------------------------------
# 5. 去除 master 节点 taint (单节点模式，允许调度工作负载)
# ----------------------------------------------------------------------------
echo "[步骤 5] 去除 master 节点 taint (允许调度工作负载)..."

# 等待 API Server 就绪
echo "  等待 API Server 就绪..."
sleep 10

# 设置 KUBECONFIG 环境变量以执行 kubectl 命令
export KUBECONFIG=/etc/kubernetes/admin.conf

# 获取所有 taint 并移除
for taint in $(kubectl get nodes ${NODE_NAME} -o jsonpath='{.spec.taints[*].key}' 2>/dev/null); do
    kubectl taint nodes ${NODE_NAME} ${taint}:NoSchedule- 2>/dev/null || true
done

# 验证 taint 已移除
echo "  节点 taint 状态:"
kubectl describe node ${NODE_NAME} | grep -i taint || echo "  无 taint (工作负载可调度)"

# ----------------------------------------------------------------------------
# 6. 安装 Calico CNI 网络插件
# ----------------------------------------------------------------------------
echo "[步骤 6] 安装 Calico CNI 网络插件..."

# 下载 Calico 部署清单（使用阿里云镜像源替代默认镜像）
cat > /tmp/calico.yaml <<'EOF'
# Source: https://docs.projectcalico.org/v3.26/manifests/calico.yaml
# 修改为阿里云镜像源并适配 Pod CIDR 10.244.0.0/16
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: calico-node
  namespace: kube-system
---
apiVersion: v1
kind: Secret
metadata:
  name: calico-node
  namespace: kube-system
type: Opaque
data:
  # 自动生成的凭据
  calico-node-password: ""
---
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: felixconfigurations.crd.projectcalico.org
spec:
  group: crd.projectcalico.org
  names:
    kind: FelixConfiguration
    listKind: FelixConfigurationList
    plural: felixconfigurations
    singular: felixconfiguration
  scope: Cluster
  versions:
  - name: v1
    schema:
      openAPIV3Schema:
        type: object
        properties:
          apiVersion: {type: string}
          kind: {type: string}
          metadata: {type: object}
          spec: {type: object, x-kubernetes-preserve-unknown-fields: true}
    served: true
    storage: true
---
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: bgppeers.crd.projectcalico.org
spec:
  group: crd.projectcalico.org
  names:
    kind: BGPPeer
    listKind: BGPPeerList
    plural: bgppeers
    singular: bgppeer
  scope: Cluster
  versions:
  - name: v1
    schema:
      openAPIV3Schema:
        type: object
        properties:
          apiVersion: {type: string}
          kind: {type: string}
          metadata: {type: object}
          spec: {type: object, x-kubernetes-preserve-unknown-fields: true}
    served: true
    storage: true
---
apiVersion: apiextensions.k8s.io/v1
kind: CustomResourceDefinition
metadata:
  name: ipamblocks.crd.projectcalico.org
spec:
  group: crd.projectcalico.org
  names:
    kind: IPAMBlock
    listKind: IPAMBlockList
    plural: ipamblocks
    singular: ipamblock
  scope: Cluster
  versions:
  - name: v1
    schema:
      openAPIV3Schema:
        type: object
        properties:
          apiVersion: {type: string}
          kind: {type: string}
          metadata: {type: object}
          spec: {type: object, x-kubernetes-preserve-unknown-fields: true}
    served: true
    storage: true
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: calico-kube-controllers
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: calico-kube-controllers
rules:
- apiGroups: [""]
  resources: ["nodes"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["crd.projectcalico.org"]
  resources: ["ippools"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: calico-kube-controllers
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: calico-kube-controllers
subjects:
- kind: ServiceAccount
  name: calico-kube-controllers
  namespace: kube-system
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: calico-node
rules:
- apiGroups: [""]
  resources: ["pods", "nodes", "namespaces"]
  verbs: ["get", "list", "watch"]
- apiGroups: [""]
  resources: ["endpoints", "services"]
  verbs: ["get", "list", "watch"]
- apiGroups: ["crd.projectcalico.org"]
  resources: ["felixconfigurations", "ippools", "ipreservations", "networkpolicies", "networksets", "bgpconfigurations", "bgppeers", "globalnetworkpolicies", "globalnetworksets"]
  verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: calico-node
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: calico-node
subjects:
- kind: ServiceAccount
  name: calico-node
  namespace: kube-system
---
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: calico-node
  namespace: kube-system
  labels:
    k8s-app: calico-node
spec:
  selector:
    matchLabels:
      k8s-app: calico-node
  template:
    metadata:
      labels:
        k8s-app: calico-node
    spec:
      nodeSelector:
        kubernetes.io/os: linux
      hostNetwork: true
      serviceAccountName: calico-node
      containers:
      - name: calico-node
        image: registry.cn-hangzhou.aliyuncs.com/google_containers/calico-node:v3.26.1
        env:
        - name: CALICO_IPV4POOL_CIDR
          value: "10.244.0.0/16"
        - name: CALICO_IPV4POOL_IPIP
          value: "Never"
        - name: CALICO_IPV4POOL_VXLAN
          value: "CrossSubnet"
        - name: DATASTORE_TYPE
          value: "kubernetes"
        - name: IP_AUTODETECTION_METHOD
          value: "interface=eth.*|ens.*|enp.*"
        - name: FELIX_LOGSEVERITY
          value: "info"
        - name: NODENAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        - name: FELIX_DEFAULTENDPOINTTOHOSTACTION
          value: "ACCEPT"
        - name: FELIX_IPV6SUPPORT
          value: "false"
        - name: FELIX_IPINIPLEN
          value: "0"
        - name: FELIX_LOGFILE
          value: "/var/log/calico/cni/cni.log"
        - name: CALICO_DISABLE_FILE_LOGGING
          value: "false"
        - name: CALICO_NETWORKING_BACKEND
          value: "bird"
        securityContext:
          privileged: true
        resources:
          requests:
            cpu: 250m
            memory: 256Mi
          limits:
            cpu: 1000m
            memory: 1024Mi
        volumeMounts:
        - mountPath: /host/opt/cni/bin
          name: cni-bin-dir
        - mountPath: /host/etc/cni/net.d
          name: cni-net-dir
        - mountPath: /var/run/calico
          name: var-run-calico
          readOnly: false
        - mountPath: /var/lib/calico
          name: var-lib-calico
          readOnly: false
        - mountPath: /run/xtables.lock
          name: xtables-lock
          readOnly: false
        - mountPath: /lib/modules
          name: lib-modules
          readOnly: true
        - mountPath: /var/log/calico
          name: var-log-calico
          readOnly: false
      volumes:
      - name: cni-bin-dir
        hostPath:
          path: /opt/cni/bin
      - name: cni-net-dir
        hostPath:
          path: /etc/cni/net.d
      - name: var-run-calico
        hostPath:
          path: /var/run/calico
      - name: var-lib-calico
        hostPath:
          path: /var/lib/calico
      - name: xtables-lock
        hostPath:
          path: /run/xtables.lock
          type: FileOrCreate
      - name: lib-modules
        hostPath:
          path: /lib/modules
      - name: var-log-calico
        hostPath:
          path: /var/log/calico
          type: DirectoryOrCreate
      tolerations:
      - operator: Exists
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: calico-kube-controllers
  namespace: kube-system
  labels:
    k8s-app: calico-kube-controllers
spec:
  replicas: 1
  selector:
    matchLabels:
      k8s-app: calico-kube-controllers
  template:
    metadata:
      labels:
        k8s-app: calico-kube-controllers
    spec:
      nodeSelector:
        kubernetes.io/os: linux
      tolerations:
      - key: node-role.kubernetes.io/master
        effect: NoSchedule
        operator: Exists
      - key: node-role.kubernetes.io/control-plane
        effect: NoSchedule
        operator: Exists
      serviceAccountName: calico-kube-controllers
      containers:
      - name: calico-kube-controllers
        image: registry.cn-hangzhou.aliyuncs.com/google_containers/calico-kube-controllers:v3.26.1
        env:
        - name: DATASTORE_TYPE
          value: "kubernetes"
        - name: ENABLED_CONTROLLERS
          value: "node"
        - name: NODENAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 512Mi
        volumeMounts:
        - mountPath: /var/lib/calico
          name: var-lib-calico
          readOnly: true
      volumes:
      - name: var-lib-calico
        hostPath:
          path: /var/lib/calico
          type: DirectoryOrCreate
---
apiVersion: crd.projectcalico.org/v1
kind: IPPool
metadata:
  name: default-ipv4-ippool
spec:
  cidr: 10.244.0.0/16
  ipipMode: Never
  vxlanMode: CrossSubnet
  natOutgoing: true
  disabled: false
EOF

# 应用 Calico 配置
kubectl apply -f /tmp/calico.yaml
echo "  Calico CNI 正在部署，等待 Pod 就绪..."

# 等待 Calico Pod 就绪
echo "  等待 Calico Pod 就绪 (最多等待 180 秒)..."
kubectl -n kube-system wait --for=condition=Ready pod -l k8s-app=calico-node --timeout=180s || \
    echo "  警告: 部分 Calico Pod 未就绪，请手动检查: kubectl get pods -n kube-system"

kubectl -n kube-system wait --for=condition=Ready pod -l k8s-app=calico-kube-controllers --timeout=180s || \
    echo "  警告: calico-kube-controllers Pod 未就绪，请手动检查"

echo "  Calico CNI 已安装"

# ----------------------------------------------------------------------------
# 7. 安装 local-path storage provisioner (Rancher)
# ----------------------------------------------------------------------------
echo "[步骤 7] 安装 local-path storage provisioner (Rancher)..."

cat > /tmp/local-path-storage.yaml <<'EOF'
---
apiVersion: v1
kind: Namespace
metadata:
  name: local-path-storage
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: local-path-provisioner-service-account
  namespace: local-path-storage
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: local-path-provisioner-role
rules:
- apiGroups: [""]
  resources: ["nodes", "persistentvolumeclaims", "persistentvolumes"]
  verbs: ["get", "list", "watch", "create", "delete"]
- apiGroups: [""]
  resources: ["endpoints", "events", "services"]
  verbs: ["get", "list", "watch", "create", "update", "patch"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: local-path-provisioner-bind
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: local-path-provisioner-role
subjects:
- kind: ServiceAccount
  name: local-path-provisioner-service-account
  namespace: local-path-storage
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: local-path-provisioner
  namespace: local-path-storage
spec:
  replicas: 1
  selector:
    matchLabels:
      app: local-path-provisioner
  template:
    metadata:
      labels:
        app: local-path-provisioner
    spec:
      serviceAccountName: local-path-provisioner-service-account
      tolerations:
      - key: node-role.kubernetes.io/master
        operator: Exists
      - key: node-role.kubernetes.io/control-plane
        operator: Exists
      containers:
      - name: local-path-provisioner
        image: registry.cn-hangzhou.aliyuncs.com/google_containers/local-path-provisioner:v0.0.24
        imagePullPolicy: IfNotPresent
        command:
        - local-path-provisioner
        - --debug
        - start
        - --config
        - /etc/config/config.json
        env:
        - name: POD_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        volumeMounts:
        - name: config-volume
          mountPath: /etc/config/
        resources:
          requests:
            cpu: 100m
            memory: 128Mi
          limits:
            cpu: 500m
            memory: 512Mi
      volumes:
      - name: config-volume
        configMap:
          name: local-path-config
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: local-path-config
  namespace: local-path-storage
data:
  config.json: |-
    {
      "nodePathMap": [
        {
          "node": "DEFAULT_PATH_FOR_NON_LISTED_NODES",
          "paths": ["/data/local-path-provisioner"]
        }
      ]
    }
---
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: local-path
  annotations:
    storageclass.kubernetes.io/is-default-class: "true"
    default.storageclass.kubernetes.io/is-default-class: "true"
provisioner: rancher.io/local-path
volumeBindingMode: WaitForFirstConsumer
reclaimPolicy: Delete
allowVolumeExpansion: true
EOF

# 应用 local-path storage 配置
kubectl apply -f /tmp/local-path-storage.yaml
echo "  local-path storage provisioner 正在部署..."

# 创建本地存储目录
mkdir -p /data/local-path-provisioner

# 等待 local-path-provisioner Pod 就绪
echo "  等待 local-path-provisioner Pod 就绪 (最多等待 60 秒)..."
kubectl -n local-path-storage wait --for=condition=Ready pod -l app=local-path-provisioner --timeout=60s || \
    echo "  警告: local-path-provisioner Pod 未就绪，请手动检查"

echo "  local-path storage provisioner 已安装"
echo "  StorageClass local-path 已设置为默认 StorageClass"

# ----------------------------------------------------------------------------
# 8. 保存 join 命令到文件
# ----------------------------------------------------------------------------
echo "[步骤 8] 保存 worker 节点 join 命令..."

# 创建 join 命令文件目录
JOIN_DIR="/root/k8s-deploy"
mkdir -p ${JOIN_DIR}

# 生成 join 命令并保存
kubeadm token create --print-join-command > ${JOIN_DIR}/join-command.sh
chmod +x ${JOIN_DIR}/join-command.sh
echo "  join 命令已保存到: ${JOIN_DIR}/join-command.sh"

# 同时复制一份到部署脚本目录
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cp ${JOIN_DIR}/join-command.sh ${SCRIPT_DIR}/join-command.sh 2>/dev/null || true

# 显示 join 命令
echo ""
echo "  Worker 节点加入命令:"
echo "  ------------------------------------------"
cat ${JOIN_DIR}/join-command.sh
echo "  ------------------------------------------"

# ----------------------------------------------------------------------------
# 9. 验证集群状态
# ----------------------------------------------------------------------------
echo ""
echo "[步骤 9] 验证集群状态..."

# 等待核心组件就绪
echo "  等待核心组件就绪..."
sleep 15

echo ""
echo "  节点状态:"
kubectl get nodes -o wide

echo ""
echo "  所有命名空间的 Pod 状态:"
kubectl get pods --all-namespaces -o wide

echo ""
echo "  StorageClass 列表:"
kubectl get storageclass

# ----------------------------------------------------------------------------
# 完成提示
# ----------------------------------------------------------------------------
echo ""
echo "=========================================="
echo "  K8s 集群初始化完成!"
echo "=========================================="
echo ""
echo "集群信息:"
echo "  Master/Worker 节点: ${NODE_NAME} (${MASTER_IP})"
echo "  Pod CIDR: ${POD_CIDR}"
echo "  Service CIDR: ${SERVICE_CIDR}"
echo "  CNI: Calico v3.26"
echo "  StorageClass: local-path (默认)"
echo ""
echo "Join 命令文件:"
echo "  ${JOIN_DIR}/join-command.sh"
echo ""
echo "后续操作:"
echo "  1. 切换到 taiking 用户: su - taiking"
echo "  2. 查看集群: kubectl get nodes"
echo "  3. 查看 Pod: kubectl get pods -A"
echo "  4. 添加新节点: 复制 join-command.sh 到新节点执行"
echo "  5. 初始化 SQL Server: 在 Windows 服务器执行 03-init-sqlserver.ps1"
echo ""
echo "  Windows 数据库服务器 (10.30.10.141) SQL Server 初始化:"
echo "    在 Windows 上运行 PowerShell 脚本: 03-init-sqlserver.ps1"
echo ""
