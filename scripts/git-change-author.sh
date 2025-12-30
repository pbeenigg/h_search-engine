#!/bin/bash

# ============================================
# Git 提交历史作者信息批量修改脚本
# ============================================
# 功能：批量修改所有 Git 提交记录的作者和邮箱
# 警告：此操作会重写 Git 历史，请谨慎使用！
# ============================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[信息]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[成功]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[警告]${NC} $1"
}

print_error() {
    echo -e "${RED}[错误]${NC} $1"
}

# 打印分隔线
print_separator() {
    echo "============================================"
}

# 检查是否在 Git 仓库中
check_git_repo() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "当前目录不是 Git 仓库！"
        exit 1
    fi
}

# 检查工作区是否干净
check_clean_working_tree() {
    if ! git diff-index --quiet HEAD --; then
        print_error "工作区有未提交的修改，请先提交或暂存！"
        git status --short
        exit 1
    fi
}

# 显示当前作者信息
show_current_authors() {
    print_separator
    print_info "当前仓库的所有作者信息："
    print_separator
    git log --format='%an <%ae>' | sort -u
    print_separator
}

# 获取用户输入
get_user_input() {
    print_separator
    print_warning "此操作将重写 Git 历史，请确保已备份！"
    print_separator
    
    echo ""
    read -p "请输入旧的作者名称（留空表示匹配所有）: " OLD_NAME
    read -p "请输入旧的邮箱地址（留空表示匹配所有）: " OLD_EMAIL
    echo ""
    read -p "请输入新的作者名称: " NEW_NAME
    read -p "请输入新的邮箱地址: " NEW_EMAIL
    
    # 验证新的作者信息不能为空
    if [ -z "$NEW_NAME" ] || [ -z "$NEW_EMAIL" ]; then
        print_error "新的作者名称和邮箱不能为空！"
        exit 1
    fi
    
    # 验证邮箱格式
    if ! echo "$NEW_EMAIL" | grep -qE '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'; then
        print_error "邮箱格式不正确！"
        exit 1
    fi
}

# 显示将要执行的操作
show_operation_summary() {
    print_separator
    print_info "操作摘要："
    print_separator
    
    if [ -z "$OLD_NAME" ] && [ -z "$OLD_EMAIL" ]; then
        echo "匹配条件: 所有提交"
    else
        echo "匹配条件:"
        [ -n "$OLD_NAME" ] && echo "  旧作者名称: $OLD_NAME"
        [ -n "$OLD_EMAIL" ] && echo "  旧邮箱地址: $OLD_EMAIL"
    fi
    
    echo ""
    echo "修改为:"
    echo "  新作者名称: $NEW_NAME"
    echo "  新邮箱地址: $NEW_EMAIL"
    
    print_separator
}

# 确认操作
confirm_operation() {
    echo ""
    read -p "确认要执行此操作吗？(yes/no): " CONFIRM
    
    if [ "$CONFIRM" != "yes" ]; then
        print_warning "操作已取消"
        exit 0
    fi
}

# 执行作者信息修改
change_author_info() {
    print_separator
    print_info "开始修改作者信息..."
    print_separator
    
    # 构建过滤条件
    local filter_script="
        # 获取当前提交的作者和邮箱
        COMMIT_NAME=\"\$GIT_AUTHOR_NAME\"
        COMMIT_EMAIL=\"\$GIT_AUTHOR_EMAIL\"
        
        # 判断是否需要修改
        SHOULD_CHANGE=0
    "
    
    # 如果指定了旧的作者名称
    if [ -n "$OLD_NAME" ]; then
        filter_script+="
        if [ \"\$COMMIT_NAME\" = \"$OLD_NAME\" ]; then
            SHOULD_CHANGE=1
        fi
        "
    else
        filter_script+="
        SHOULD_CHANGE=1
        "
    fi
    
    # 如果指定了旧的邮箱
    if [ -n "$OLD_EMAIL" ]; then
        filter_script+="
        if [ \"\$COMMIT_EMAIL\" != \"$OLD_EMAIL\" ]; then
            SHOULD_CHANGE=0
        fi
        "
    fi
    
    # 执行修改
    filter_script+="
        if [ \$SHOULD_CHANGE -eq 1 ]; then
            export GIT_AUTHOR_NAME=\"$NEW_NAME\"
            export GIT_AUTHOR_EMAIL=\"$NEW_EMAIL\"
            export GIT_COMMITTER_NAME=\"$NEW_NAME\"
            export GIT_COMMITTER_EMAIL=\"$NEW_EMAIL\"
        fi
    "
    
    # 执行 git filter-branch
    git filter-branch --env-filter "$filter_script" --tag-name-filter cat -- --branches --tags
    
    print_success "作者信息修改完成！"
}

# 显示修改后的作者信息
show_new_authors() {
    print_separator
    print_info "修改后的作者信息："
    print_separator
    git log --format='%an <%ae>' | sort -u
    print_separator
}

# 显示后续操作提示
show_next_steps() {
    print_separator
    print_warning "后续操作提示："
    print_separator
    echo "1. 验证修改结果："
    echo "   git log --oneline --graph --all"
    echo ""
    echo "2. 如果需要撤销修改："
    echo "   git reset --hard refs/original/refs/heads/main"
    echo "   (将 main 替换为你的分支名)"
    echo ""
    echo "3. 如果确认无误，推送到远程仓库："
    echo "   git push --force --all"
    echo "   git push --force --tags"
    echo ""
    echo "4. 清理备份引用："
    echo "   git for-each-ref --format='delete %(refname)' refs/original | git update-ref --stdin"
    echo "   git reflog expire --expire=now --all"
    echo "   git gc --prune=now"
    print_separator
}

# 主函数
main() {
    clear
    print_separator
    print_info "Git 提交历史作者信息批量修改工具"
    print_separator
    
    # 检查环境
    check_git_repo
    check_clean_working_tree
    
    # 显示当前作者信息
    show_current_authors
    
    # 获取用户输入
    get_user_input
    
    # 显示操作摘要
    show_operation_summary
    
    # 确认操作
    confirm_operation
    
    # 执行修改
    change_author_info
    
    # 显示修改后的作者信息
    show_new_authors
    
    # 显示后续操作提示
    show_next_steps
    
    print_success "脚本执行完成！"
}

# 执行主函数
main
