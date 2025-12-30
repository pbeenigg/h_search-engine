#!/bin/bash

################################################################################
# Redis 数据迁移脚本
# 功能：使用 Redis-shake 工具将本地 Redis 数据迁移到远程 Redis
# 作者：HotelTrip 团队
# 日期：2024-12-24
################################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置变量
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REDIS_SHAKE_VERSION="v4.4.2"
REDIS_SHAKE_DIR="${SCRIPT_DIR}/redis-shake"
REDIS_SHAKE_BIN="${REDIS_SHAKE_DIR}/redis-shake"
CONFIG_FILE="${SCRIPT_DIR}/redis-shake.toml"
LOG_FILE="${SCRIPT_DIR}/redis-shake.log"
STATUS_FILE="${SCRIPT_DIR}/redis-shake-status.json"

# 源Redis配置
SOURCE_HOST="localhost"
SOURCE_PORT="6379"
SOURCE_PASSWORD=""

# 目标Redis配置
TARGET_HOST="8.210.191.60"
TARGET_PORT="6379"
TARGET_PASSWORD="hotel!!search!!engine1234"

################################################################################
# 工具函数
################################################################################

# 打印信息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# 打印成功信息
print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 打印警告信息
print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 打印错误信息
print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v "$1" &> /dev/null; then
        print_error "命令 '$1' 未找到，请先安装"
        return 1
    fi
    return 0
}

# 检查Redis连接
check_redis_connection() {
    local host=$1
    local port=$2
    local password=$3
    local name=$4
    
    print_info "检查 ${name} Redis 连接 (${host}:${port})..."
    
    if [ -z "$password" ]; then
        if redis-cli -h "$host" -p "$port" ping &> /dev/null; then
            print_success "${name} Redis 连接正常"
            return 0
        fi
    else
        if redis-cli -h "$host" -p "$port" -a "$password" --no-auth-warning ping &> /dev/null; then
            print_success "${name} Redis 连接正常"
            return 0
        fi
    fi
    
    print_error "${name} Redis 连接失败"
    return 1
}

# 获取Redis信息
get_redis_info() {
    local host=$1
    local port=$2
    local password=$3
    local name=$4
    
    print_info "获取 ${name} Redis 信息..."
    
    if [ -z "$password" ]; then
        local dbsize=$(redis-cli -h "$host" -p "$port" DBSIZE 2>/dev/null || echo "0")
    else
        local dbsize=$(redis-cli -h "$host" -p "$port" -a "$password" --no-auth-warning DBSIZE 2>/dev/null || echo "0")
    fi
    
    echo "  - 键数量: ${dbsize}"
}

# 下载 Redis-shake
download_redis_shake() {
    print_info "检查 Redis-shake..."
    
    if [ -f "$REDIS_SHAKE_BIN" ]; then
        print_success "Redis-shake 已存在，跳过下载"
        return 0
    fi
    
    print_info "下载 Redis-shake ${REDIS_SHAKE_VERSION}..."
    
    # 创建临时目录
    mkdir -p "${REDIS_SHAKE_DIR}"
    cd "${REDIS_SHAKE_DIR}"
    
    # 检测操作系统和架构
    local os_type=$(uname -s | tr '[:upper:]' '[:lower:]')
    local arch_type=$(uname -m)
    
    case "$arch_type" in
        x86_64)
            arch_type="amd64"
            ;;
        aarch64|arm64)
            arch_type="arm64"
            ;;
        *)
            print_error "不支持的架构: $arch_type"
            return 1
            ;;
    esac
    
    case "$os_type" in
        darwin)
            os_type="darwin"
            ;;
        linux)
            os_type="linux"
            ;;
        *)
            print_error "不支持的操作系统: $os_type"
            return 1
            ;;
    esac

    #https://github.com/tair-opensource/RedisShake/releases/download/v4.4.2/redis-shake-v4.4.2-linux-arm64.tar.gz

    local download_url="https://github.com/tair-opensource/RedisShake/releases/download/${REDIS_SHAKE_VERSION}/redis-shake-${REDIS_SHAKE_VERSION}-${os_type}-${arch_type}.tar.gz"
    local tar_filename="redis-shake-${REDIS_SHAKE_VERSION}-${os_type}-${arch_type}.tar.gz"
    
    # 检查是否已有手动下载的文件
    if [ -f "${SCRIPT_DIR}/${tar_filename}" ]; then
        print_success "发现已下载的文件: ${tar_filename}"
        print_info "使用手动下载的文件..."
        cp "${SCRIPT_DIR}/${tar_filename}" "${REDIS_SHAKE_DIR}/redis-shake.tar.gz"
    else
        print_info "下载地址: ${download_url}"
        print_info "尝试自动下载..."
        
        if ! curl -L -o "${REDIS_SHAKE_DIR}/redis-shake.tar.gz" "$download_url" 2>/dev/null; then
            print_error "自动下载失败（可能是网络限制）"
            echo ""
            print_warning "请手动下载 Redis-shake："
            echo ""
            echo "  1. 浏览器打开: ${download_url}"
            echo "  2. 下载完成后，将文件放到: ${SCRIPT_DIR}/"
            echo "  3. 确保文件名为: ${tar_filename}"
            echo "  4. 重新执行本脚本"
            echo ""
            return 1
        fi
        print_success "下载完成"
    fi
    
    print_info "解压文件..."
    cd "${REDIS_SHAKE_DIR}"
    tar -xzf redis-shake.tar.gz
    rm -f redis-shake.tar.gz
    
    if [ ! -f "$REDIS_SHAKE_BIN" ]; then
        print_error "Redis-shake 二进制文件未找到"
        print_info "解压后的文件列表:"
        ls -la
        return 1
    fi
    
    chmod +x "$REDIS_SHAKE_BIN"
    print_success "Redis-shake 准备完成"
    
    cd "$SCRIPT_DIR"
}

# 执行迁移
execute_migration() {
    print_info "开始执行 Redis 数据迁移..."
    print_info "配置文件: ${CONFIG_FILE}"
    print_info "日志文件: ${LOG_FILE}"
    
    # 清理旧的状态文件
    if [ -f "$STATUS_FILE" ]; then
        print_warning "发现旧的状态文件，将被删除"
        rm -f "$STATUS_FILE"
    fi
    
    # 执行迁移
    print_info "执行迁移命令..."
    "$REDIS_SHAKE_BIN" "$CONFIG_FILE"
    
    print_success "迁移完成！"
}

# 显示迁移摘要
show_migration_summary() {
    print_info "迁移摘要："
    echo ""
    echo "源Redis (本地):"
    get_redis_info "$SOURCE_HOST" "$SOURCE_PORT" "$SOURCE_PASSWORD" "源"
    echo ""
    echo "目标Redis (远程):"
    get_redis_info "$TARGET_HOST" "$TARGET_PORT" "$TARGET_PASSWORD" "目标"
    echo ""
}

# 主函数
main() {
    echo ""
    echo "========================================================"
    echo "          Redis 数据迁移工具 (基于 Redis-shake)         "
    echo "========================================================"
    echo ""
    
    # 检查必要的命令
    print_info "检查系统依赖..."
    check_command "curl" || exit 1
    check_command "tar" || exit 1
    check_command "redis-cli" || {
        print_warning "redis-cli 未安装，无法检查 Redis 连接状态"
        print_warning "建议安装 redis-cli: brew install redis (macOS)"
    }
    
    # 检查Redis连接
    echo ""
    if command -v redis-cli &> /dev/null; then
        check_redis_connection "$SOURCE_HOST" "$SOURCE_PORT" "$SOURCE_PASSWORD" "源" || {
            print_error "源 Redis 连接失败，请检查配置"
            exit 1
        }
        
        check_redis_connection "$TARGET_HOST" "$TARGET_PORT" "$TARGET_PASSWORD" "目标" || {
            print_error "目标 Redis 连接失败，请检查配置"
            exit 1
        }
        
        echo ""
        show_migration_summary
    fi
    
    # 用户确认
    echo ""
    print_warning "即将开始迁移，目标Redis的数据可能会被覆盖！"
    read -p "确认继续？(yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        print_info "迁移已取消"
        exit 0
    fi
    
    # 下载 Redis-shake
    echo ""
    download_redis_shake || exit 1
    
    # 执行迁移
    echo ""
    execute_migration
    
    # 显示迁移后的摘要
    if command -v redis-cli &> /dev/null; then
        echo ""
        print_info "迁移后状态："
        show_migration_summary
    fi
    
    echo ""
    print_success "所有操作完成！"
    print_info "日志文件: ${LOG_FILE}"
    echo ""
}

# 执行主函数
main "$@"
