#!/bin/bash

# ============================================
# Git 作者信息快速修改脚本（预配置版本）
# ============================================
# 使用方法：直接运行即可，无需交互输入
# ============================================

set -e

# ========== 配置区域 ==========
# 请在这里修改你的作者信息

# 旧的作者信息（留空表示匹配所有）
OLD_AUTHOR_NAME=""
OLD_AUTHOR_EMAIL=""

# 新的作者信息
NEW_AUTHOR_NAME="Pax"
NEW_AUTHOR_EMAIL="pax@heytrippgo.com"

# ========== 配置区域结束 ==========

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}[信息]${NC} $1"; }
print_success() { echo -e "${GREEN}[成功]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[警告]${NC} $1"; }
print_error() { echo -e "${RED}[错误]${NC} $1"; }

# 检查 Git 仓库
if ! git rev-parse --git-dir > /dev/null 2>&1; then
    print_error "当前目录不是 Git 仓库！"
    exit 1
fi

# 检查工作区
if ! git diff-index --quiet HEAD --; then
    print_error "工作区有未提交的修改，请先提交或暂存！"
    exit 1
fi

# 验证配置
if [ "$NEW_AUTHOR_NAME" = "Your Name" ] || [ "$NEW_AUTHOR_EMAIL" = "your.email@example.com" ]; then
    print_error "请先在脚本中配置新的作者信息！"
    print_info "编辑文件: $0"
    print_info "修改 NEW_AUTHOR_NAME 和 NEW_AUTHOR_EMAIL 变量"
    exit 1
fi

print_info "当前作者信息："
git log --format='%an <%ae>' | sort -u
echo ""

print_warning "即将修改为: $NEW_AUTHOR_NAME <$NEW_AUTHOR_EMAIL>"
echo ""
read -p "确认执行？(yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    print_warning "操作已取消"
    exit 0
fi

print_info "开始修改..."

# 执行修改
git filter-branch --env-filter "
    export GIT_AUTHOR_NAME='$NEW_AUTHOR_NAME'
    export GIT_AUTHOR_EMAIL='$NEW_AUTHOR_EMAIL'
    export GIT_COMMITTER_NAME='$NEW_AUTHOR_NAME'
    export GIT_COMMITTER_EMAIL='$NEW_AUTHOR_EMAIL'
" --tag-name-filter cat -- --branches --tags

print_success "修改完成！"
echo ""
print_info "修改后的作者信息："
git log --format='%an <%ae>' | sort -u
echo ""

print_warning "后续操作："
echo "1. 验证: git log --oneline"
echo "2. 推送: git push --force --all && git push --force --tags"
echo "3. 清理: git for-each-ref --format='delete %(refname)' refs/original | git update-ref --stdin"
