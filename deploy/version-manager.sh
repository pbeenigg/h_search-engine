#!/bin/bash

# HeyTrip Supplier Integration 版本管理脚本
# 用于管理Docker镜像版本和标签
# 作者: Pax
# 版本: 1.0.0

set -e

# 颜色定义
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

# 显示使用帮助
show_help() {
    echo "HeyTrip Supplier Integration 版本管理脚本"
    echo ""
    echo "用法:"
    echo "  $0 <命令> [选项]"
    echo ""
    echo "命令:"
    echo "  list                  列出所有镜像版本"
    echo "  tag <源标签> <新标签>   为现有镜像添加新标签"
    echo "  clean                 清理旧版本镜像（保留最新3个版本）"
    echo "  prune                 清理所有悬空镜像"
    echo "  info <标签>           显示指定镜像的详细信息"
    echo "  history               显示镜像构建历史"
    echo ""
    echo "选项:"
    echo "  --keep N             清理时保留最新N个版本 (默认: 3)"
    echo "  --force              强制执行操作，不询问确认"
    echo "  --help               显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 list"
    echo "  $0 tag 1.0.0-SNAPSHOT-20250924-104500 v1.0.0"
    echo "  $0 clean --keep 5"
    echo "  $0 info latest"
}

# 列出所有镜像版本
list_images() {
    log_info "HeyTrip Supplier Integration 镜像列表:"
    echo "================================================================"
    
    # 检查是否存在镜像
    if ! docker images --filter "reference=heytrip/supplier*" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}" | grep -q "heytrip/supplier"; then
        log_warning "未找到任何 heytrip/supplier 镜像"
        return 0
    fi
    
    # 显示镜像列表
    docker images --filter "reference=heytrip/supplier*" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
    
    echo ""
    log_info "镜像统计:"
    local count=$(docker images --filter "reference=heytrip/supplier*" --format "{{.ID}}" | wc -l)
    echo "总计: $count 个镜像"
    
    local total_size=$(docker images --filter "reference=heytrip/supplier*" --format "{{.Size}}" | sed 's/MB//g' | sed 's/GB//g' | awk '{sum += $1} END {print sum}')
    echo "总大小: 约 ${total_size}MB"
}

# 为镜像添加新标签
tag_image() {
    local source_tag="$1"
    local target_tag="$2"
    
    if [ -z "$source_tag" ] || [ -z "$target_tag" ]; then
        log_error "请指定源标签和目标标签"
        echo "用法: $0 tag <源标签> <目标标签>"
        exit 1
    fi
    
    local source_image="heytrip/supplier:$source_tag"
    local target_image="heytrip/supplier:$target_tag"
    
    # 检查源镜像是否存在
    if ! docker image inspect "$source_image" &>/dev/null; then
        log_error "源镜像不存在: $source_image"
        exit 1
    fi
    
    log_info "为镜像添加新标签..."
    log_info "源镜像: $source_image"
    log_info "目标镜像: $target_image"
    
    # 添加标签
    docker tag "$source_image" "$target_image"
    
    log_success "标签添加成功！"
    
    # 显示结果
    echo ""
    log_info "更新后的镜像列表:"
    docker images --filter "reference=heytrip/supplier*" --format "table {{.Repository}}\t{{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.Size}}"
}

# 清理旧版本镜像
clean_old_images() {
    local keep_count="${1:-3}"
    local force="${2:-false}"
    
    log_info "清理旧版本镜像（保留最新 $keep_count 个版本）..."
    
    # 获取所有镜像，按创建时间排序
    local images=$(docker images --filter "reference=heytrip/supplier*" --format "{{.Repository}}:{{.Tag}}\t{{.CreatedAt}}" | sort -k2 -r)
    
    if [ -z "$images" ]; then
        log_warning "未找到任何镜像"
        return 0
    fi
    
    # 计算要删除的镜像
    local total_count=$(echo "$images" | wc -l)
    
    if [ "$total_count" -le "$keep_count" ]; then
        log_info "当前镜像数量 ($total_count) 不超过保留数量 ($keep_count)，无需清理"
        return 0
    fi
    
    local delete_count=$((total_count - keep_count))
    local images_to_delete=$(echo "$images" | tail -n "$delete_count" | awk '{print $1}')
    
    echo ""
    log_info "将要删除的镜像:"
    echo "$images_to_delete"
    
    if [ "$force" != "true" ]; then
        echo ""
        read -p "确认删除这些镜像? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "操作已取消"
            return 0
        fi
    fi
    
    # 删除镜像
    echo "$images_to_delete" | while read -r image; do
        if [ -n "$image" ]; then
            log_info "删除镜像: $image"
            docker rmi "$image" 2>/dev/null || log_warning "删除失败: $image"
        fi
    done
    
    log_success "清理完成！"
    
    # 显示清理后的结果
    echo ""
    list_images
}

# 清理悬空镜像
prune_images() {
    local force="${1:-false}"
    
    log_info "清理悬空镜像..."
    
    # 检查是否有悬空镜像
    local dangling_images=$(docker images -f "dangling=true" -q)
    
    if [ -z "$dangling_images" ]; then
        log_info "没有悬空镜像需要清理"
        return 0
    fi
    
    local count=$(echo "$dangling_images" | wc -l)
    log_info "发现 $count 个悬空镜像"
    
    if [ "$force" != "true" ]; then
        read -p "确认清理悬空镜像? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            log_info "操作已取消"
            return 0
        fi
    fi
    
    docker image prune -f
    log_success "悬空镜像清理完成！"
}

# 显示镜像详细信息
show_image_info() {
    local tag="$1"
    
    if [ -z "$tag" ]; then
        log_error "请指定镜像标签"
        echo "用法: $0 info <标签>"
        exit 1
    fi
    
    local image="heytrip/supplier:$tag"
    
    # 检查镜像是否存在
    if ! docker image inspect "$image" &>/dev/null; then
        log_error "镜像不存在: $image"
        exit 1
    fi
    
    log_info "镜像详细信息: $image"
    echo "================================================================"
    
    # 显示基本信息
    docker image inspect "$image" --format "
镜像ID: {{.Id}}
创建时间: {{.Created}}
大小: {{.Size}} bytes
架构: {{.Architecture}}
操作系统: {{.Os}}
"
    
    # 显示标签信息
    echo "所有标签:"
    docker image inspect "$image" --format "{{range .RepoTags}}  {{.}}{{end}}"
    echo ""
    
    # 显示层信息
    echo "镜像层数: $(docker image inspect "$image" --format "{{len .RootFS.Layers}}")"
    
    # 显示环境变量（如果有）
    local env_vars=$(docker image inspect "$image" --format "{{range .Config.Env}}{{.}}{{end}}")
    if [ -n "$env_vars" ]; then
        echo ""
        echo "环境变量:"
        docker image inspect "$image" --format "{{range .Config.Env}}  {{.}}{{end}}"
    fi
}

# 显示构建历史
show_history() {
    log_info "镜像构建历史:"
    echo "================================================================"
    
    # 获取所有镜像并按时间排序
    docker images --filter "reference=heytrip/supplier*" \
        --format "table {{.Tag}}\t{{.ID}}\t{{.CreatedSince}}\t{{.CreatedAt}}\t{{.Size}}" \
        | sort -k4
}

# 主函数
main() {
    echo "========================================"
    echo "HeyTrip Supplier Integration 版本管理"
    echo "========================================"
    
    # 解析命令行参数
    COMMAND=""
    KEEP_COUNT=3
    FORCE=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            list|tag|clean|prune|info|history)
                COMMAND="$1"
                shift
                ;;
            --keep)
                KEEP_COUNT="$2"
                shift 2
                ;;
            --force)
                FORCE=true
                shift
                ;;
            --help|-h)
                show_help
                exit 0
                ;;
            -*)
                log_error "未知选项: $1"
                show_help
                exit 1
                ;;
            *)
                # 处理命令参数
                case $COMMAND in
                    tag)
                        if [ -z "$SOURCE_TAG" ]; then
                            SOURCE_TAG="$1"
                        elif [ -z "$TARGET_TAG" ]; then
                            TARGET_TAG="$1"
                        else
                            log_error "tag命令只接受两个参数"
                            exit 1
                        fi
                        ;;
                    info)
                        if [ -z "$INFO_TAG" ]; then
                            INFO_TAG="$1"
                        else
                            log_error "info命令只接受一个参数"
                            exit 1
                        fi
                        ;;
                    *)
                        log_error "未知参数: $1"
                        show_help
                        exit 1
                        ;;
                esac
                shift
                ;;
        esac
    done
    
    # 验证命令
    if [ -z "$COMMAND" ]; then
        log_error "请指定命令"
        show_help
        exit 1
    fi
    
    # 执行命令
    case $COMMAND in
        list)
            list_images
            ;;
        tag)
            tag_image "$SOURCE_TAG" "$TARGET_TAG"
            ;;
        clean)
            clean_old_images "$KEEP_COUNT" "$FORCE"
            ;;
        prune)
            prune_images "$FORCE"
            ;;
        info)
            show_image_info "$INFO_TAG"
            ;;
        history)
            show_history
            ;;
        *)
            log_error "未知命令: $COMMAND"
            show_help
            exit 1
            ;;
    esac
}

# 错误处理
trap 'log_error "执行过程中发生错误"; exit 1' ERR

# 执行主函数
main "$@"
