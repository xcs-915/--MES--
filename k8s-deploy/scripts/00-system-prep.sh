#!/bin/bash
# ==============================================================================
# 脚本名称: 00-system-prep.sh
# 功能描述: K8s 集群部署 - 系统准备脚本
#           关闭 firewalld、SELinux、swap，配置内核参数，
#           配置 yum 源（阿里云），安装 Docker CE 与 K8s 组件
# 适用系统: CentOS 8.3
# 执行用户: root
# 使用方法: bash 00-system-prep.sh
# ==============================================================================
set -e

echo "=========================================="
echo "  K8s 系统准备 - CentOS 8.3"
echo "=========================================="

# ----------------------------------------------------------------------------
# 1. 关闭 firewalld
# ----------------------------------------------------------------------------
echo "[步骤 1] 关闭 firewalld..."
systemctl stop firewalld 2>/dev/null || true
systemctl disable firewalld 2>/dev/null || true
echo "  firewalld 已关闭并禁用"

# ----------------------------------------------------------------------------
# 2. 关闭 SELinux
# ----------------------------------------------------------------------------
echo "[步骤 2] 关闭 SELinux..."
setenforce 0 2>/dev/null || true
# 永久关闭 SELinux
if grep -q "^SELINUX=" /etc/selinux/config; then
    sed -i 's/^SELINUX=.*/SELINUX=disabled/' /etc/selinux/config
else
    echo "SELINUX=disabled" >> /etc/selinux/config
fi
echo "  SELinux 已关闭，重启后永久生效"

# ----------------------------------------------------------------------------
# 3. 关闭 swap
# ----------------------------------------------------------------------------
echo "[步骤 3] 关闭 swap..."
swapoff -a
# 永久关闭 swap，注释掉 /etc/fstab 中的 swap 行
sed -i '/swap/s/^/# /' /etc/fstab
echo "  swap 已关闭，/etc/fstab 中的 swap 行已注释"

# ----------------------------------------------------------------------------
# 4. 设置内核参数
# ----------------------------------------------------------------------------
echo "[步骤 4] 设置内核参数..."
cat > /etc/sysctl.d/k8s.conf <<'EOF'
net.ipv4.ip_forward = 1
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.tcp_keepalive_time = 600
net.ipv4.tcp_keepalive_intvl = 30
net.ipv4.tcp_keepalive_probes = 10
net.ipv4.tcp_max_tw_buckets = 1048576
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 30
net.core.somaxconn = 65535
net.core.netdev_max_backlog = 65535
net.ipv4.tcp_max_syn_backlog = 65535
fs.file-max = 2097152
fs.inotify.max_user_instances = 8192
fs.inotify.max_user_watches = 1048576
vm.max_map_count = 262144
vm.swappiness = 0
EOF

sysctl --system > /dev/null 2>&1
echo "  内核参数已设置"

# ----------------------------------------------------------------------------
# 5. 加载内核模块
# ----------------------------------------------------------------------------
echo "[步骤 5] 加载内核模块 br_netfilter 和 overlay..."
cat > /etc/modules-load.d/k8s.conf <<'EOF'
br_netfilter
overlay
EOF

modprobe br_netfilter
modprobe overlay
echo "  br_netfilter 和 overlay 模块已加载"

# 验证模块是否加载成功
if lsmod | grep -q br_netfilter && lsmod | grep -q overlay; then
    echo "  模块加载验证: 通过"
else
    echo "  警告: 模块可能未正确加载，请手动检查"
fi

# ----------------------------------------------------------------------------
# 6. 配置 yum 源（阿里云 CentOS 8 vault 源）
# ----------------------------------------------------------------------------
echo "[步骤 6] 配置阿里云 CentOS 8 yum 源..."

# 备份原有 repo 文件
if [ ! -d /etc/yum.repos.d/bak ]; then
    mkdir -p /etc/yum.repos.d/bak
fi
mv /etc/yum.repos.d/*.repo /etc/yum.repos.d/bak/ 2>/dev/null || true

# CentOS 8 已 EOL，使用阿里云 vault 镜像源
cat > /etc/yum.repos.d/CentOS-Base.repo <<'EOF'
[base]
name=CentOS-8 - Base
baseurl=https://mirrors.aliyun.com/centos-vault/8.3.2011/BaseOS/$basearch/os/
gpgcheck=1
gpgkey=https://mirrors.aliyun.com/centos-vault/8.3.2011/BaseOS/$basearch/os/RPM-GPG-KEY-centosofficial
enabled=1

[extras]
name=CentOS-8 - Extras
baseurl=https://mirrors.aliyun.com/centos-vault/8.3.2011/extras/$basearch/os/
gpgcheck=1
gpgkey=https://mirrors.aliyun.com/centos-vault/8.3.2011/BaseOS/$basearch/os/RPM-GPG-KEY-centosofficial
enabled=1

[appstream]
name=CentOS-8 - AppStream
baseurl=https://mirrors.aliyun.com/centos-vault/8.3.2011/AppStream/$basearch/os/
gpgcheck=1
gpgkey=https://mirrors.aliyun.com/centos-vault/8.3.2011/BaseOS/$basearch/os/RPM-GPG-KEY-centosofficial
enabled=1

[centosplus]
name=CentOS-8 - Plus
baseurl=https://mirrors.aliyun.com/centos-vault/8.3.2011/centosplus/$basearch/os/
gpgcheck=1
gpgkey=https://mirrors.aliyun.com/centos-vault/8.3.2011/BaseOS/$basearch/os/RPM-GPG-KEY-centosofficial
enabled=0
EOF

echo "  CentOS 8 BaseOS / AppStream / Extras 源已配置"

# 配置阿里云 Docker CE yum 源
echo "  配置阿里云 Docker CE yum 源..."
cat > /etc/yum.repos.d/docker-ce.repo <<'EOF'
[docker-ce-stable]
name=Docker CE Stable - $basearch
baseurl=https://mirrors.aliyun.com/docker-ce/linux/centos/8/$basearch/stable
enabled=1
gpgcheck=1
gpgkey=https://mirrors.aliyun.com/docker-ce/linux/centos/gpg
EOF

# 配置阿里云 Kubernetes yum 源
echo "  配置阿里云 Kubernetes yum 源..."
cat > /etc/yum.repos.d/kubernetes.repo <<'EOF'
[kubernetes]
name=Kubernetes
baseurl=https://mirrors.aliyun.com/kubernetes-new/core/stable/v1.28/rpm/
enabled=1
gpgcheck=1
gpgkey=https://mirrors.aliyun.com/kubernetes-new/core/stable/v1.28/rpm/doc/rpm-package-key.gpg
EOF

# 清除缓存并重建
yum clean all
yum makecache
echo "  yum 缓存已重建"

# ----------------------------------------------------------------------------
# 7. 安装基础工具
# ----------------------------------------------------------------------------
echo "[步骤 7] 安装基础工具..."
yum install -y yum-utils device-mapper-persistent-data lvm2 wget curl vim socat conntrack ipset
echo "  基础工具安装完成"

# ----------------------------------------------------------------------------
# 8. 安装 Docker CE
# ----------------------------------------------------------------------------
echo "[步骤 8] 安装 Docker CE..."
yum install -y docker-ce-20.10.* docker-ce-cli-20.10.* containerd.io
echo "  Docker CE 安装完成"

# ----------------------------------------------------------------------------
# 9. 配置 Docker daemon.json
# ----------------------------------------------------------------------------
echo "[步骤 9] 配置 Docker daemon.json..."
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://registry.cn-hangzhou.aliyuncs.com"
  ],
  "insecure-registries": [
    "10.30.10.140:5000",
    "10.30.10.140:5001"
  ],
  "storage-driver": "overlay2",
  "storage-opts": [
    "overlay2.override_kernel_check=true"
  ],
  "exec-opts": [
    "native.cgroupdriver=systemd"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  },
  "live-restore": true,
  "max-concurrent-downloads": 10,
  "max-concurrent-uploads": 5
}
EOF
echo "  Docker daemon.json 已配置"
echo "    - registry-mirrors: 阿里云杭州镜像加速器"
echo "    - insecure-registries: 本地私有仓库地址"
echo "    - storage-driver: overlay2"
echo "    - cgroupdriver: systemd"

# 配置 Docker systemd 服务
mkdir -p /etc/systemd/system/docker.service.d

# 重载 systemd 配置
systemctl daemon-reload

# 启动并启用 Docker
systemctl enable docker
systemctl start docker
echo "  Docker 已启动并设置为开机自启"

# 验证 Docker
docker version
docker info | grep -E "Storage Driver|Cgroup Driver|Cgroup Version"

# ----------------------------------------------------------------------------
# 10. 安装 Kubernetes 组件 (kubeadm, kubelet, kubectl 1.28)
# ----------------------------------------------------------------------------
echo "[步骤 10] 安装 Kubernetes 1.28 组件..."
yum install -y kubeadm-1.28.* kubelet-1.28.* kubectl-1.28.*
echo "  kubeadm, kubelet, kubectl 1.28 安装完成"

# 配置 kubelet systemd 服务（确保 cgroupdriver 与 docker 一致）
cat > /etc/sysconfig/kubelet <<'EOF'
KUBELET_EXTRA_ARGS="--cgroup-driver=systemd"
EOF

# 重载 systemd 配置
systemctl daemon-reload

# 启动并启用 kubelet（kubelet 在 kubeadm init 之前会不断重启，这是正常现象）
systemctl enable kubelet
systemctl start kubelet || true
echo "  kubelet 已启动并设置为开机自启"
echo "  (注意: kubeadm init 之前 kubelet 会反复重启，这是正常行为)"

# 验证安装
echo ""
echo "=========================================="
echo "  安装验证"
echo "=========================================="
echo "Docker 版本:"
docker --version
echo ""
echo "kubeadm 版本:"
kubeadm version
echo ""
echo "kubelet 版本:"
kubelet --version
echo ""
echo "kubectl 版本:"
kubectl version --client
echo ""

# ----------------------------------------------------------------------------
# 11. 设置时间同步 (chrony)
# ----------------------------------------------------------------------------
echo "[步骤 11] 配置时间同步 (chrony)..."
yum install -y chrony
# 配置阿里云 NTP 服务器
cat > /etc/chrony.conf <<'EOF'
server ntp.aliyun.com iburst
server ntp1.aliyun.com iburst
server ntp2.aliyun.com iburst
driftfile /var/lib/chrony/drift
makestep 1.0 3
rtcsync
allow 10.30.10.0/24
local stratum 10
logdir /var/log/chrony
EOF
systemctl enable chronyd
systemctl restart chronyd
echo "  chrony 已配置并启动，NTP 服务器: ntp.aliyun.com"

# ----------------------------------------------------------------------------
# 12. 优化系统限制
# ----------------------------------------------------------------------------
echo "[步骤 12] 优化系统文件描述符限制..."
cat > /etc/security/limits.d/k8s.conf <<'EOF'
*       soft    nofile      655350
*       hard    nofile      655350
*       soft    nproc       655350
*       hard    nproc       655350
*       soft    memlock     unlimited
*       hard    memlock     unlimited
root    soft    nofile      655350
root    hard    nofile      655350
EOF
echo "  文件描述符限制已优化"

# ----------------------------------------------------------------------------
# 完成提示
# ----------------------------------------------------------------------------
echo ""
echo "=========================================="
echo "  系统准备完成!"
echo "=========================================="
echo ""
echo "已完成:"
echo "  1. firewalld 已关闭"
echo "  2. SELinux 已关闭"
echo "  3. swap 已关闭"
echo "  4. 内核参数已配置 (ip_forward, bridge-nf-call-iptables 等)"
echo "  5. br_netfilter / overlay 模块已加载"
echo "  6. 阿里云 yum 源已配置"
echo "  7. Docker CE 已安装并启动"
echo "  8. Kubernetes 1.28 组件已安装"
echo "  9. chrony 时间同步已配置"
echo "  10. 系统限制已优化"
echo ""
echo "建议: 重启服务器后执行 01-k8s-init.sh 初始化 K8s 集群"
echo "  reboot"
echo "  bash 01-k8s-init.sh"
echo ""
