#!/bin/bash

###############################################################################
# 前端 Docker 一键部署脚本
# 功能：构建 Docker 镜像、推送到阿里云镜像仓库、远程服务器拉取并运行
###############################################################################

set -e  # 遇到错误立即退出

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 加载环境变量
if [ -f ".env.deploy" ]; then
    log_info "加载部署配置文件..."
    source .env.deploy
else
    log_error "未找到 .env.deploy 配置文件"
    exit 1
fi

# 检查必需的环境变量
required_vars=(
    "UPLOAD_HOST" "UPLOAD_USER" "UPLOAD_PORT" 
    "DOCKER_REGISTRY" "DOCKER_USERNAME" "DOCKER_PASSWORD" 
    "DOCKER_IMAGE_NAME" "DOCKER_CONTAINER_NAME"
)

for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        log_error "缺少必需的环境变量: $var"
        exit 1
    fi
done

# 配置变量
TIMESTAMP=$(date +%Y%m%d%H%M%S)
VERSION="hotel-search-web_1.0.0_${TIMESTAMP}"
FULL_IMAGE_NAME="${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:${VERSION}"
LATEST_IMAGE_NAME="${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}:latest"
TARGET_PLATFORM="${TARGET_PLATFORM:-linux/amd64}"

# 可选：Docker Hub 加速镜像源（例如：registry.cn-hangzhou.aliyuncs.com/library）
DOCKER_BASE_MIRROR="${DOCKER_BASE_MIRROR}"
BUILD_ARGS=""
if [ -n "${DOCKER_BASE_MIRROR}" ]; then
    NODE_IMAGE_OVERRIDE="${DOCKER_BASE_MIRROR}/node:22-alpine"
    NGINX_IMAGE_OVERRIDE="${DOCKER_BASE_MIRROR}/nginx:alpine"
    BUILD_ARGS="${BUILD_ARGS} --build-arg NODE_IMAGE=${NODE_IMAGE_OVERRIDE} --build-arg NGINX_IMAGE=${NGINX_IMAGE_OVERRIDE}"
    log_info "使用基础镜像镜像源: ${DOCKER_BASE_MIRROR}"
fi

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    log_error "Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查 sshpass 是否安装
USE_SSHPASS=false
if command -v sshpass &> /dev/null; then
    USE_SSHPASS=true
    log_info "检测到 sshpass，将使用自动登录"
else
    log_warning "未检测到 sshpass，将使用手动输入密码方式"
fi

# SSH 通用参数（尽量保证走密码/键盘交互，避免优先尝试公钥导致误判）
SSH_COMMON_OPTS=(
    -p "${UPLOAD_PORT}"
    -o StrictHostKeyChecking=no
    -o UserKnownHostsFile=/dev/null
    -o PreferredAuthentications=password,keyboard-interactive
    -o PubkeyAuthentication=no
    -o NumberOfPasswordPrompts=1
)

# 显示部署信息
log_info "=========================================="
log_info "部署信息"
log_info "=========================================="
log_info "镜像版本: ${VERSION}"
log_info "镜像名称: ${FULL_IMAGE_NAME}"
log_info "容器名称: ${DOCKER_CONTAINER_NAME}"
log_info "远程服务器: ${UPLOAD_USER}@${UPLOAD_HOST}:${UPLOAD_PORT}"
log_info "=========================================="

# 步骤 1: 构建 Docker 镜像
log_info "=========================================="
log_info "步骤 1/5: 构建 Docker 镜像"
log_info "=========================================="

# 检查是否存在最近构建的镜像
IMAGE_EXISTS=false
SKIP_BUILD=false

if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "${LATEST_IMAGE_NAME}"; then
    IMAGE_EXISTS=true
    # 获取镜像创建时间（Unix 时间戳）
    IMAGE_CREATED=$(docker inspect -f '{{.Created}}' "${LATEST_IMAGE_NAME}" 2>/dev/null || echo "")
    
    if [ -n "$IMAGE_CREATED" ]; then
        IMAGE_TIMESTAMP=$(date -j -f "%Y-%m-%dT%H:%M:%S" "$(echo $IMAGE_CREATED | cut -d'.' -f1)" "+%s" 2>/dev/null || echo "0")
        CURRENT_TIMESTAMP=$(date +%s)
        TIME_DIFF=$((CURRENT_TIMESTAMP - IMAGE_TIMESTAMP))
        
        # 如果镜像在 5 分钟（300秒）内构建的，跳过构建
        if [ $TIME_DIFF -lt 300 ]; then
            SKIP_BUILD=true
            MINUTES=$((TIME_DIFF / 60))
            SECONDS=$((TIME_DIFF % 60))
            log_info "检测到最近构建的镜像（${MINUTES}分${SECONDS}秒前）"
            log_warning "跳过构建步骤，直接使用现有镜像"
            
            # 重新标记镜像为新版本
            log_info "为现有镜像添加新版本标签: ${VERSION}"
            docker tag "${LATEST_IMAGE_NAME}" "${FULL_IMAGE_NAME}"
        fi
    fi
fi

if [ "$SKIP_BUILD" = false ]; then
    log_info "开始构建镜像..."
    # 优先使用 buildx 构建指定平台镜像，避免架构不匹配
    if docker buildx version >/dev/null 2>&1; then
        log_info "使用 Buildx 以平台: ${TARGET_PLATFORM} 构建镜像"
        # 创建或切换到构建器
        docker buildx create --name hs-builder --use >/dev/null 2>&1 || docker buildx use hs-builder >/dev/null 2>&1 || true
        docker buildx inspect hs-builder >/dev/null 2>&1 || docker buildx create --name hs-builder --use >/dev/null 2>&1
        # 使用 --load 将单平台镜像加载到本地以便后续 push
        docker buildx build --platform "${TARGET_PLATFORM}" -t "${FULL_IMAGE_NAME}" -t "${LATEST_IMAGE_NAME}" --load ${BUILD_ARGS} .
    else
        log_warning "未检测到 buildx，将使用 docker build（可能导致架构不匹配）"
        log_warning "如遇到 exec format error，请安装/启用 buildx 或设置 TARGET_PLATFORM"
        docker build -t "${FULL_IMAGE_NAME}" -t "${LATEST_IMAGE_NAME}" ${BUILD_ARGS} .
    fi
    log_success "镜像构建完成: ${FULL_IMAGE_NAME}"
else
    log_success "使用现有镜像: ${FULL_IMAGE_NAME}"
fi

# 步骤 2: 登录阿里云镜像仓库
log_info "=========================================="
log_info "步骤 2/5: 登录阿里云镜像仓库"
log_info "=========================================="
log_info "登录到: ${DOCKER_REGISTRY}"

# 重试登录（最多3次）
MAX_RETRIES=3
RETRY_COUNT=0
LOGIN_SUCCESS=false

while [ $RETRY_COUNT -lt $MAX_RETRIES ] && [ "$LOGIN_SUCCESS" = false ]; do
    RETRY_COUNT=$((RETRY_COUNT + 1))
    log_info "尝试登录 (${RETRY_COUNT}/${MAX_RETRIES})..."
    
    if echo "${DOCKER_PASSWORD}" | docker login --username "${DOCKER_USERNAME}" --password-stdin "${DOCKER_REGISTRY}" 2>&1; then
        LOGIN_SUCCESS=true
        log_success "登录成功"
    else
        if [ $RETRY_COUNT -lt $MAX_RETRIES ]; then
            log_warning "登录失败，等待 5 秒后重试..."
            sleep 5
        else
            log_error "登录失败，已达到最大重试次数"
            log_error "请检查："
            log_error "1. 网络连接是否正常"
            log_error "2. 镜像仓库地址是否正确: ${DOCKER_REGISTRY}"
            log_error "3. 用户名和密码是否正确"
            log_error "4. 是否需要配置代理或 VPN"
            log_info ""
            log_info "可以手动测试登录："
            log_info "  echo '${DOCKER_PASSWORD}' | docker login --username '${DOCKER_USERNAME}' --password-stdin ${DOCKER_REGISTRY}"
            exit 1
        fi
    fi
done

# 步骤 3: 推送镜像到阿里云
log_info "=========================================="
log_info "步骤 3/5: 推送镜像到阿里云仓库"
log_info "=========================================="

# 推送版本镜像（带重试）
log_info "推送版本镜像: ${FULL_IMAGE_NAME}"
MAX_PUSH_RETRIES=3
PUSH_RETRY_COUNT=0
PUSH_SUCCESS=false

while [ $PUSH_RETRY_COUNT -lt $MAX_PUSH_RETRIES ] && [ "$PUSH_SUCCESS" = false ]; do
    PUSH_RETRY_COUNT=$((PUSH_RETRY_COUNT + 1))
    log_info "推送尝试 (${PUSH_RETRY_COUNT}/${MAX_PUSH_RETRIES})..."
    
    if docker push "${FULL_IMAGE_NAME}"; then
        PUSH_SUCCESS=true
        log_success "版本镜像推送完成"
    else
        if [ $PUSH_RETRY_COUNT -lt $MAX_PUSH_RETRIES ]; then
            log_warning "推送失败，等待 10 秒后重试..."
            sleep 10
        else
            log_error "镜像推送失败"
            log_error ""
            log_error "可能的原因："
            log_error "1. 用户 '${DOCKER_USERNAME}' 没有推送到命名空间 '${DOCKER_IMAGE_NAME}' 的权限"
            log_error "2. 命名空间不存在或路径不正确"
            log_error "3. 镜像仓库配置有误"
            log_error ""
            log_error "解决方案："
            log_error "1. 登录阿里云控制台检查镜像仓库设置"
            log_error "2. 确认命名空间 '${DOCKER_IMAGE_NAME}' 已创建"
            log_error "3. 确认当前用户有推送权限"
            log_error "4. 尝试修改 .env.deploy 中的 DOCKER_IMAGE_NAME"
            log_error ""
            log_info "当前配置："
            log_info "  仓库地址: ${DOCKER_REGISTRY}"
            log_info "  镜像路径: ${DOCKER_IMAGE_NAME}"
            log_info "  完整名称: ${FULL_IMAGE_NAME}"
            exit 1
        fi
    fi
done

# 推送 latest 镜像
log_info "推送 latest 镜像: ${LATEST_IMAGE_NAME}"
if docker push "${LATEST_IMAGE_NAME}"; then
    log_success "latest 镜像推送完成"
else
    log_warning "latest 镜像推送失败，但版本镜像已推送成功，继续部署..."
fi

# 步骤 4: 在远程服务器上部署
log_info "=========================================="
log_info "步骤 4/5: 在远程服务器上部署"
log_info "=========================================="

REMOTE_COMMANDS=$(cat <<EOF
set -e

echo ">>> 登录阿里云镜像仓库..."
echo "${DOCKER_PASSWORD}" | docker login --username "${DOCKER_USERNAME}" --password-stdin "${DOCKER_REGISTRY}"

echo ">>> 拉取最新镜像..."
docker pull "${FULL_IMAGE_NAME}"

echo ">>> 停止并删除旧容器（如果存在）..."
if docker ps -a --format '{{.Names}}' | grep -q "^${DOCKER_CONTAINER_NAME}\$"; then
    docker stop "${DOCKER_CONTAINER_NAME}" || true
    docker rm "${DOCKER_CONTAINER_NAME}" || true
    echo "已删除旧容器: ${DOCKER_CONTAINER_NAME}"
fi

echo ">>> 启动新容器..."
docker run -d \\
    --name "${DOCKER_CONTAINER_NAME}" \\
    --restart unless-stopped \\
    -p ${DOCKER_PORT:-80}:80 \\
    "${FULL_IMAGE_NAME}"

echo ">>> 等待容器启动..."
sleep 3

echo ">>> 检查容器状态..."
if docker ps --format '{{.Names}}' | grep -q "^${DOCKER_CONTAINER_NAME}\$"; then
    echo "✅ 容器运行正常: ${DOCKER_CONTAINER_NAME}"
    docker ps --filter "name=${DOCKER_CONTAINER_NAME}" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
else
    echo "❌ 容器启动失败，查看日志:"
    docker logs "${DOCKER_CONTAINER_NAME}" || true
    exit 1
fi

echo ">>> 清理远程旧镜像（保留当前版本与latest）..."
REPO_PATH="${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}"
CURRENT_TAG="${REPO_PATH}:${VERSION}"
LATEST_TAG="${REPO_PATH}:latest"

# 删除同一仓库下除当前版本与 latest 外的镜像标签（与本地清理方式一致）
while IFS= read -r IMG; do
    if [ -n "$IMG" ]; then
        echo "移除远程旧镜像: $IMG"
        docker rmi -f "$IMG" || true
    fi
done < <(docker images --format "{{.Repository}}:{{.Tag}}" \
    | grep "^${REPO_PATH}:" \
    | grep -v -E ":(${VERSION}|latest)$")

echo ">>> 清理未使用的镜像层..."
docker image prune -f || true

echo ">>> 部署完成！"
EOF
)

if [ "$USE_SSHPASS" = true ] && [ -n "$UPLOAD_PASSWORD" ]; then
    log_info "在远程服务器执行部署命令..."
    if ! sshpass -p "$UPLOAD_PASSWORD" ssh "${SSH_COMMON_OPTS[@]}" "$UPLOAD_USER@$UPLOAD_HOST" "$REMOTE_COMMANDS"; then
        log_warning "sshpass 自动登录失败，将回退为手动输入密码方式继续部署。"
        log_info "如果手动也失败，请确认 UPLOAD_PASSWORD 是否为实际明文密码，以及服务器是否允许该账号密码登录。"
        if ! ssh "${SSH_COMMON_OPTS[@]}" "$UPLOAD_USER@$UPLOAD_HOST" "$REMOTE_COMMANDS"; then
            log_error "SSH 登录失败（Permission denied）。"
            log_error "请确认："
            log_error "1) UPLOAD_USER/UPLOAD_HOST/UPLOAD_PORT 是否正确"
            log_error "2) UPLOAD_PASSWORD 是否为明文密码（不是加密/编码后的串）"
            log_error "3) 目标服务器是否允许密码登录/是否允许 root 密码登录"
            exit 1
        fi
    fi
else
    log_info "请输入 SSH 密码以执行远程部署命令"
    if ! ssh "${SSH_COMMON_OPTS[@]}" "$UPLOAD_USER@$UPLOAD_HOST" "$REMOTE_COMMANDS"; then
        log_error "SSH 登录失败（Permission denied）。"
        log_error "请确认："
        log_error "1) UPLOAD_USER/UPLOAD_HOST/UPLOAD_PORT 是否正确"
        log_error "2) 目标服务器是否允许密码登录/是否允许 root 密码登录"
        exit 1
    fi
fi
log_success "远程部署完成"

# 步骤 5: 清理本地镜像（可选）
log_info "=========================================="
log_info "步骤 5/5: 清理本地旧镜像"
log_info "=========================================="
REPO_PATH="${DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}"
read -p "是否清理本地旧镜像（保留当前版本与latest）？(y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    log_info "开始清理本地旧镜像: ${REPO_PATH}"
    # 删除同一仓库下除当前版本与 latest 外的镜像标签
    while IFS= read -r IMG; do
        if [ -n "$IMG" ]; then
            log_info "移除本地旧镜像: $IMG"
            docker rmi -f "$IMG" || true
        fi
    done < <(docker images --format "{{.Repository}}:{{.Tag}}" \
        | grep "^${REPO_PATH}:" \
        | grep -v -E ":(${VERSION}|latest)$")

    # 清理悬空镜像层
    docker image prune -f || true
    log_success "本地镜像清理完成"
else
    log_info "跳过本地镜像清理"
fi

# 完成
log_info "=========================================="
log_success "🎉 部署成功完成！"
log_info "=========================================="
log_info "部署时间: $(date)"
log_info "镜像版本: ${VERSION}"
log_info "镜像地址: ${FULL_IMAGE_NAME}"
log_info "容器名称: ${DOCKER_CONTAINER_NAME}"
log_info "服务器地址: ${UPLOAD_HOST}"
log_info "访问端口: ${DOCKER_PORT:-80}"
log_info "=========================================="
log_info ""
log_info "查看容器日志："
log_info "  ssh -p ${UPLOAD_PORT} ${UPLOAD_USER}@${UPLOAD_HOST} \"docker logs -f ${DOCKER_CONTAINER_NAME}\""
log_info ""
log_info "重启容器："
log_info "  ssh -p ${UPLOAD_PORT} ${UPLOAD_USER}@${UPLOAD_HOST} \"docker restart ${DOCKER_CONTAINER_NAME}\""
log_info ""
log_info "停止容器："
log_info "  ssh -p ${UPLOAD_PORT} ${UPLOAD_USER}@${UPLOAD_HOST} \"docker stop ${DOCKER_CONTAINER_NAME}\""

