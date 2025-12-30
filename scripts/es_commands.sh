#!/bin/bash

################################################################################
# Elasticsearch 常用命令脚本
# 用途：快速生成并执行 Elasticsearch 常用请求命令
################################################################################

set -e

# ==================== 配置参数 ====================
ES_URL=${ES_URL:-"http://localhost:19200"}
ES_USER=${ES_USER:-"elastic"}
ES_PASSWORD=${ES_PASSWORD:-"DA6oHeV8sUbratbDDPnfSvLLGC1jk5FU"}

# Elasticdump 配置
# 预定义的ES服务器
ES_LOCAL="http://elastic:DA6oHeV8sUbratbDDPnfSvLLGC1jk5FU@localhost:19200"
ES_REMOTE1="http://elastic:DA6oHeV8sUbratbDDPnfSvLLGC1jk5FU@47.76.191.223:19200"
# 注意：密码中的特殊字符需要URL编码 (#=%23, !=%21)
ES_REMOTE2="http://elastic:h69PR3%23QImku2%23HWL%21e2ug4kg7pw@es-cn-adn3rha130002yd4w.public.elasticsearch.aliyuncs.com:9200"

ES_REMOTE3="http://elastic:DA6oHeV8sUbratbDDPnfSvLLGC1jk5FU@8.210.191.60:19200"

# 默认导出目录
DUMP_DIR=${DUMP_DIR:-"./es_dumps"}

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ==================== 辅助函数 ====================

# 打印标题
print_title() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# 打印成功信息
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# 打印警告信息
print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# 打印错误信息
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 执行 ES 命令
es_request() {
    local method=$1
    local path=$2
    local data=$3
    
    # 打印实际请求命令（输出到stderr避免干扰JSON解析）
    echo -e "${YELLOW}[请求命令]${NC}" >&2
    if [ -z "$data" ]; then
        # 输出不带颜色的命令（可直接复制执行）
        echo "curl -u \"$ES_USER:$ES_PASSWORD\" -X $method \"$ES_URL$path\"" >&2
        echo "" >&2
        curl -s -u "$ES_USER:$ES_PASSWORD" -X "$method" "$ES_URL$path"
    else
        # 输出不带颜色的命令（可直接复制执行）
        echo "curl -u \"$ES_USER:$ES_PASSWORD\" -X $method \"$ES_URL$path\" \\" >&2
        echo "  -H 'Content-Type: application/json' \\" >&2
        echo "  -d '$data'" >&2
        echo "" >&2
        curl -s -u "$ES_USER:$ES_PASSWORD" -X "$method" "$ES_URL$path" \
            -H 'Content-Type: application/json' \
            -d "$data"
    fi
}

# 格式化 JSON 输出
format_json() {
    if command -v jq &> /dev/null; then
        jq .
    else
        cat
    fi
}

# ==================== 集群管理命令 ====================

# 查看集群健康状态
cluster_health() {
    print_title "集群健康状态"
    es_request GET "/_cluster/health?pretty" | format_json
}

# 查看集群状态
cluster_state() {
    print_title "集群状态"
    es_request GET "/_cluster/state?pretty" | format_json
}

# 查看集群统计信息
cluster_stats() {
    print_title "集群统计信息"
    es_request GET "/_cluster/stats?pretty" | format_json
}

# 查看节点信息
node_info() {
    print_title "节点信息"
    es_request GET "/_nodes?pretty" | format_json
}

# ==================== 索引管理命令 ====================

# 列出所有索引
list_indices() {
    print_title "所有索引列表"
    es_request GET "/_cat/indices?v&s=index"
}

# 查看索引详细信息
index_info() {
    local index_name=$1
    if [ -z "$index_name" ]; then
        print_error "请提供索引名称"
        return 1
    fi
    
    print_title "索引信息: $index_name"
    es_request GET "/$index_name?pretty" | format_json
}

# 查看索引映射
index_mapping() {
    local index_name=$1
    if [ -z "$index_name" ]; then
        print_error "请提供索引名称"
        return 1
    fi
    
    print_title "索引映射: $index_name"
    es_request GET "/$index_name/_mapping?pretty" | format_json
}

# 查看索引设置
index_settings() {
    local index_name=$1
    if [ -z "$index_name" ]; then
        print_error "请提供索引名称"
        return 1
    fi
    
    print_title "索引设置: $index_name"
    es_request GET "/$index_name/_settings?pretty" | format_json
}

# 查看索引统计信息
index_stats() {
    local index_name=$1
    if [ -z "$index_name" ]; then
        print_error "请提供索引名称"
        return 1
    fi
    
    print_title "索引统计: $index_name"
    es_request GET "/$index_name/_stats?pretty" | format_json
}

# 创建索引
create_index() {
    local index_name=$1
    local settings=$2
    
    if [ -z "$index_name" ]; then
        print_error "请提供索引名称"
        return 1
    fi
    
    if [ -z "$settings" ]; then
        settings='{
            "settings": {
                "number_of_shards": 3,
                "number_of_replicas": 1
            }
        }'
    fi
    
    print_title "创建索引: $index_name"
    es_request PUT "/$index_name" "$settings" | format_json
    
    if [ $? -eq 0 ]; then
        print_success "索引创建成功: $index_name"
    else
        print_error "索引创建失败: $index_name"
    fi
}

# 删除索引
delete_index() {
    local index_name=$1
    if [ -z "$index_name" ]; then
        print_error "请提供索引名称"
        return 1
    fi
    
    print_warning "即将删除索引: $index_name"
    read -p "确认删除? (yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        print_warning "取消删除操作"
        return 0
    fi
    
    print_title "删除索引: $index_name"
    es_request DELETE "/$index_name" | format_json
    
    if [ $? -eq 0 ]; then
        print_success "索引删除成功: $index_name"
    else
        print_error "索引删除失败: $index_name"
    fi
}

# 刷新索引
refresh_index() {
    local index_name=$1
    if [ -z "$index_name" ]; then
        index_name="_all"
    fi
    
    print_title "刷新索引: $index_name"
    es_request POST "/$index_name/_refresh" | format_json
    print_success "索引刷新完成"
}

# 查看索引文档数量
count_docs() {
    local index_name=$1
    if [ -z "$index_name" ]; then
        print_error "请提供索引名称"
        return 1
    fi
    
    print_title "文档数量: $index_name"
    es_request GET "/$index_name/_count?pretty" | format_json
}

# 查询最近更新或新增的记录
recent_docs() {
    local index_name=$1
    local size=${2:-10}
    
    if [ -z "$index_name" ]; then
        print_error "请提供索引名称"
        return 1
    fi
    
    print_title "最近更新的文档: $index_name (显示 $size 条)"
    
    # 使用 updatedAt 字段排序（精确时间排序）
    # 如果索引没有 updatedAt 字段，则降级为 _doc 排序
    local query='{
        "size": '$size',
        "sort": [
            {"updatedAt": {"order": "desc", "unmapped_type": "date"}},
            "_doc"
        ],
        "query": {
            "match_all": {}
        }
    }'
    
    es_request GET "/$index_name/_search?pretty" "$query" | format_json
    print_success "查询完成"
}
# 批量更新索引字段值
bulk_update_field() {
    local index_name=$1
    local field_name=$2
    local field_value=$3
    local query_condition=${4:-"match_all"}
    
    if [ -z "$index_name" ] || [ -z "$field_name" ] || [ -z "$field_value" ]; then
        print_error "请提供索引名称、字段名称和字段值"
        echo "用法: bulk_update_field <index> <field_name> <field_value> [query_condition]"
        echo ""
        echo "示例:"
        echo "  # 更新所有文档的searchEnable字段为1"
        echo "  bulk_update_field hotels_write searchEnable 1"
        echo ""
        echo "  # 更新score>4.5的文档的searchEnable字段为1"
        echo "  bulk_update_field hotels_write searchEnable 1 '{\"range\":{\"score\":{\"gt\":4.5}}}'"
        return 1
    fi
    
    print_warning "即将批量更新索引: $index_name"
    echo "  字段: $field_name = $field_value"
    echo "  查询条件: $query_condition"
    read -p "确认执行批量更新? (yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        print_warning "取消批量更新操作"
        return 0
    fi
    
    print_title "批量更新字段: $index_name.$field_name"
    
    # 构造查询条件
    local query_part
    if [ "$query_condition" = "match_all" ]; then
        query_part='"query": {"match_all": {}}'
    else
        query_part='"query": '"$query_condition"
    fi
    
    # 判断字段值类型（数字、布尔值、字符串）
    local script_value
    if [[ "$field_value" =~ ^[0-9]+\.?[0-9]*$ ]]; then
        # 数字类型
        script_value="$field_value"
    elif [ "$field_value" = "true" ] || [ "$field_value" = "false" ]; then
        # 布尔类型
        script_value="$field_value"
    else
        # 字符串类型
        script_value="'$field_value'"
    fi
    
    # 使用 Update By Query API
    local update_query='{
        '"$query_part"',
        "script": {
            "source": "ctx._source.'"$field_name"' = '"$script_value"'",
            "lang": "painless"
        }
    }'
    
    echo "  执行更新脚本:"
    echo "$update_query" | format_json | sed 's/.*/    &/'
    echo ""
    
    # 使用异步模式执行批量更新
    print_warning "启动异步批量更新任务..."
    UPDATE_RESULT=$(es_request POST "/$index_name/_update_by_query?conflicts=proceed&wait_for_completion=false" "$update_query")
    
    # 提取 task ID
    local task_id=$(echo "$UPDATE_RESULT" | grep -o '"task":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$task_id" ]; then
        print_error "无法获取任务ID，请检查返回结果:"
        echo "$UPDATE_RESULT" | format_json
        return 1
    fi
    
    print_success "任务已启动，Task ID: $task_id"
    echo ""
    echo "监控任务进度中..."
    echo "----------------------------------------"
    
    # 轮询任务状态
    local completed=false
    local last_updated=0
    while [ "$completed" = false ]; do
        sleep 2
        
        # 查询任务状态
        TASK_STATUS=$(curl -s -u "$ES_USER:$ES_PASSWORD" -X GET "$ES_URL/_tasks/$task_id")
        
        # 检查任务是否完成
        local task_completed=$(echo "$TASK_STATUS" | grep -o '"completed":[^,}]*' | cut -d':' -f2 | tr -d ' ')
        
        if [ "$task_completed" = "true" ]; then
            completed=true
            echo ""
            print_success "任务执行完成！"
            echo ""
            
            # 提取最终结果
            local total=$(echo "$TASK_STATUS" | grep -o '"total":[0-9]*' | head -1 | cut -d: -f2)
            local updated=$(echo "$TASK_STATUS" | grep -o '"updated":[0-9]*' | head -1 | cut -d: -f2)
            local deleted=$(echo "$TASK_STATUS" | grep -o '"deleted":[0-9]*' | head -1 | cut -d: -f2)
            local batches=$(echo "$TASK_STATUS" | grep -o '"batches":[0-9]*' | head -1 | cut -d: -f2)
            local failures=$(echo "$TASK_STATUS" | grep -o '"failures":[0-9]*' | head -1 | cut -d: -f2)
            
            echo "📊 执行统计:"
            echo "  总文档数: ${total:-0}"
            echo "  更新成功: ${updated:-0}"
            echo "  删除数量: ${deleted:-0}"
            echo "  批次数量: ${batches:-0}"
            echo "  失败数量: ${failures:-0}"
            
            if [ "${failures:-0}" != "0" ]; then
                print_error "存在失败的更新"
                echo ""
                echo "失败详情:"
                echo "$TASK_STATUS" | format_json | grep -A 20 '"failures"'
            fi
        else
            # 显示进度
            local created=$(echo "$TASK_STATUS" | grep -o '"created":[0-9]*' | head -1 | cut -d: -f2)
            local updated=$(echo "$TASK_STATUS" | grep -o '"updated":[0-9]*' | head -1 | cut -d: -f2)
            local total=$(echo "$TASK_STATUS" | grep -o '"total":[0-9]*' | head -1 | cut -d: -f2)
            
            local current_updated=${updated:-0}
            if [ "$current_updated" != "$last_updated" ]; then
                if [ -n "$total" ] && [ "$total" != "0" ]; then
                    local percent=$((current_updated * 100 / total))
                    echo "  进度: $current_updated / $total (${percent}%)"
                else
                    echo "  已更新: $current_updated"
                fi
                last_updated=$current_updated
            fi
        fi
    done
    
    echo "----------------------------------------"
    print_success "批量更新任务完成"
}

# ==================== 别名管理命令 ====================

# 列出所有别名
list_aliases() {
    print_title "所有别名列表"
    es_request GET "/_cat/aliases?v&s=alias"
}

# 查看别名详细信息
alias_info() {
    local alias_name=$1
    if [ -z "$alias_name" ]; then
        print_error "请提供别名"
        return 1
    fi
    
    print_title "别名信息: $alias_name"
    es_request GET "/_alias/$alias_name?pretty" | format_json
}

# 创建别名
create_alias() {
    local index_name=$1
    local alias_name=$2
    
    if [ -z "$index_name" ] || [ -z "$alias_name" ]; then
        print_error "请提供索引名称和别名"
        echo "用法: create_alias <index_name> <alias_name>"
        return 1
    fi
    
    local data='{
        "actions": [
            {
                "add": {
                    "index": "'$index_name'",
                    "alias": "'$alias_name'"
                }
            }
        ]
    }'
    
    print_title "创建别名: $alias_name -> $index_name"
    es_request POST "/_aliases" "$data" | format_json
    
    if [ $? -eq 0 ]; then
        print_success "别名创建成功: $alias_name -> $index_name"
    else
        print_error "别名创建失败"
    fi
}

# 删除别名
delete_alias() {
    local index_name=$1
    local alias_name=$2
    
    if [ -z "$index_name" ] || [ -z "$alias_name" ]; then
        print_error "请提供索引名称和别名"
        echo "用法: delete_alias <index_name> <alias_name>"
        return 1
    fi
    
    print_title "删除别名: $alias_name"
    es_request DELETE "/$index_name/_alias/$alias_name" | format_json
    
    if [ $? -eq 0 ]; then
        print_success "别名删除成功: $alias_name"
    else
        print_error "别名删除失败"
    fi
}

# 切换别名（原子操作）
switch_alias() {
    local old_index=$1
    local new_index=$2
    local alias_name=$3
    
    if [ -z "$old_index" ] || [ -z "$new_index" ] || [ -z "$alias_name" ]; then
        print_error "请提供旧索引、新索引和别名"
        echo "用法: switch_alias <old_index> <new_index> <alias_name>"
        return 1
    fi
    
    local data='{
        "actions": [
            {
                "remove": {
                    "index": "'$old_index'",
                    "alias": "'$alias_name'"
                }
            },
            {
                "add": {
                    "index": "'$new_index'",
                    "alias": "'$alias_name'"
                }
            }
        ]
    }'
    
    print_title "切换别名: $alias_name ($old_index -> $new_index)"
    es_request POST "/_aliases" "$data" | format_json
    
    if [ $? -eq 0 ]; then
        print_success "别名切换成功: $alias_name -> $new_index"
    else
        print_error "别名切换失败"
    fi
}

# ==================== POI 索引专用命令 ====================

# 查看 POI 索引状态
poi_status() {
    print_title "POI 索引状态"
    echo ""
    echo "1. 物理索引信息:"
    es_request GET "/_cat/indices/amap_poi*?v&s=index"
    echo ""
    echo "2. 别名信息:"
    es_request GET "/_cat/aliases/amap_poi*?v&s=alias"
    echo ""
    echo "3. 文档数量:"
    es_request GET "/amap_poi/_count?pretty" | format_json
}

# 创建 POI 读写别名
create_poi_aliases() {
    local index_name=${1:-"amap_poi"}
    
    print_title "创建 POI 读写别名"
    
    local data='{
        "actions": [
            {
                "add": {
                    "index": "'$index_name'",
                    "alias": "amap_poi_read"
                }
            },
            {
                "add": {
                    "index": "'$index_name'",
                    "alias": "amap_poi_write"
                }
            }
        ]
    }'
    
    es_request POST "/_aliases" "$data" | format_json
    
    if [ $? -eq 0 ]; then
        print_success "POI 别名创建成功"
        poi_status
    else
        print_error "POI 别名创建失败"
    fi
}

# 删除 POI 索引（谨慎操作）
delete_poi_index() {
    print_warning "即将删除 POI 索引及所有数据"
    read -p "确认删除? (输入 DELETE 确认): " confirm
    
    if [ "$confirm" != "DELETE" ]; then
        print_warning "取消删除操作"
        return 0
    fi
    
    delete_index "amap_poi"
}

# ==================== Elasticdump 数据迁移命令 ====================

# 检查 elasticdump 是否已安装
check_elasticdump() {
    if ! command -v elasticdump &> /dev/null; then
        print_error "elasticdump 未安装！"
        echo ""
        echo "安装方法："
        echo "  npm install -g elasticdump"
        echo ""
        echo "或使用 Docker："
        echo "  docker pull elasticdump/elasticsearch-dump"
        echo ""
        echo "官方文档: https://github.com/elasticsearch-dump/elasticsearch-dump"
        return 1
    fi
    return 0
}

# 离线导出：从ES导出到文件
# 用法: dump_to_file <source_es_url> <index> [output_dir]
dump_to_file() {
    local source=$1
    local index=$2
    local output_dir=${3:-"$DUMP_DIR"}
    
    if [ -z "$source" ] || [ -z "$index" ]; then
        print_error "请提供ES源地址和索引名称"
        echo "用法: dump_to_file <source_es_url> <index> [output_dir]"
        echo ""
        echo "预定义ES服务器："
        echo "  local    - 本地ES服务器"
        echo "  remote1  - 远程ES服务器1"
        echo "  remote2  - 远程ES服务器2（阿里云）"
        echo ""
        echo "示例："
        echo "  dump_to_file local amap_poi"
        echo "  dump_to_file remote1 hotels ./backups"
        return 1
    fi
    
    check_elasticdump || return 1
    
    # 转换预定义名称为实际URL
    case $source in
        local)
            source=$ES_LOCAL
            ;;
        remote1)
            source=$ES_REMOTE1
            ;;
        remote2)
            source=$ES_REMOTE2
            ;;
        remote3)
            source=$ES_REMOTE3
            ;;
    esac
    
    # 创建输出目录
    mkdir -p "$output_dir"
    
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local base_file="$output_dir/${index}_${timestamp}"
    
    print_title "离线导出索引: $index"
    echo "源ES: $source"
    echo "目标目录: $output_dir"
    echo ""
    
    # 导出映射
    print_warning "正在导出映射 (mapping)..."
    elasticdump \
        --input="$source/$index" \
        --output="${base_file}.mapping.json" \
        --type=mapping
    
    if [ $? -eq 0 ]; then
        print_success "映射导出成功: ${base_file}.mapping.json"
    else
        print_error "映射导出失败"
        return 1
    fi
    
    # 导出数据
    print_warning "正在导出数据 (data)..."
    elasticdump \
        --input="$source/$index" \
        --output="${base_file}.data.json" \
        --type=data \
        --limit=5000 \
        --timeout=120000 \
        --noRefresh
    
    if [ $? -eq 0 ]; then
        print_success "数据导出成功: ${base_file}.data.json"
    else
        print_error "数据导出失败"
        return 1
    fi
    
    # 显示文件信息
    echo ""
    print_title "导出文件信息"
    ls -lh "${base_file}".*
    echo ""
    print_success "离线导出完成！"
}

# 离线导入：从文件导入到ES
# 用法: load_from_file <target_es_url> <index> <mapping_file> <data_file>
load_from_file() {
    local target=$1
    local index=$2
    local mapping_file=$3
    local data_file=$4
    
    if [ -z "$target" ] || [ -z "$index" ] || [ -z "$mapping_file" ] || [ -z "$data_file" ]; then
        print_error "请提供完整参数"
        echo "用法: load_from_file <target_es_url> <index> <mapping_file> <data_file>"
        echo ""
        echo "预定义ES服务器："
        echo "  local    - 本地ES服务器"
        echo "  remote1  - 远程ES服务器1"
        echo "  remote2  - 远程ES服务器2（阿里云）"
        echo ""
        echo "示例："
        echo "  load_from_file local amap_poi ./dumps/amap_poi.mapping.json ./dumps/amap_poi.data.json"
        return 1
    fi
    
    check_elasticdump || return 1
    
    # 转换预定义名称为实际URL
    case $target in
        local)
            target=$ES_LOCAL
            ;;
        remote1)
            target=$ES_REMOTE1
            ;;
        remote2)
            target=$ES_REMOTE2
            ;;
        remote3)
            target=$ES_REMOTE3
            ;;
    esac
    
    # 检查文件是否存在
    if [ ! -f "$mapping_file" ]; then
        print_error "映射文件不存在: $mapping_file"
        return 1
    fi
    
    if [ ! -f "$data_file" ]; then
        print_error "数据文件不存在: $data_file"
        return 1
    fi
    
    print_title "离线导入索引: $index"
    echo "目标ES: $target"
    echo "映射文件: $mapping_file"
    echo "数据文件: $data_file"
    echo ""
    
    # 导入映射
    print_warning "正在导入映射 (mapping)..."
    elasticdump \
        --input="$mapping_file" \
        --output="$target/$index" \
        --type=mapping
    
    if [ $? -eq 0 ]; then
        print_success "映射导入成功"
    else
        print_error "映射导入失败"
        return 1
    fi
    
    # 导入数据
    print_warning "正在导入数据 (data)..."
    elasticdump \
        --input="$data_file" \
        --output="$target/$index" \
        --type=data \
        --limit=5000 \
        --timeout=120000 \
        --noRefresh
    
    if [ $? -eq 0 ]; then
        print_success "数据导入成功"
    else
        print_error "数据导入失败"
        return 1
    fi
    
    echo ""
    print_success "离线导入完成！"
}

# 在线迁移：直接从一个ES迁移到另一个ES
# 用法: migrate_online <source_es_url> <target_es_url> <index>
migrate_online() {
    local source=$1
    local target=$2
    local index=$3
    
    if [ -z "$source" ] || [ -z "$target" ] || [ -z "$index" ]; then
        print_error "请提供完整参数"
        echo "用法: migrate_online <source_es_url> <target_es_url> <index>"
        echo ""
        echo "预定义ES服务器："
        echo "  local    - 本地ES服务器"
        echo "  remote1  - 远程ES服务器1"
        echo "  remote2  - 远程ES服务器2（阿里云）"
        echo ""
        echo "示例："
        echo "  migrate_online local remote1 amap_poi"
        echo "  migrate_online remote1 local hotels"
        return 1
    fi
    
    check_elasticdump || return 1
    
    # 转换预定义名称为实际URL
    case $source in
        local)
            source=$ES_LOCAL
            ;;
        remote1)
            source=$ES_REMOTE1
            ;;
        remote2)
            source=$ES_REMOTE2
            ;;
        remote3)
            source=$ES_REMOTE3
            ;;
    esac
    
    case $target in
        local)
            target=$ES_LOCAL
            ;;
        remote1)
            target=$ES_REMOTE1
            ;;
        remote2)
            target=$ES_REMOTE2
            ;;
        remote3)
            target=$ES_REMOTE3
            ;;
    esac
    
    print_title "在线迁移索引: $index"
    echo "源ES: $source"
    echo "目标ES: $target"
    echo ""
    
    print_warning "即将开始迁移，这可能需要较长时间..."
    read -p "确认继续? (yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        print_warning "取消迁移操作"
        return 0
    fi
    
    # 迁移映射
    print_warning "正在迁移映射 (mapping)..."
    elasticdump \
        --input="$source/$index" \
        --output="$target/$index" \
        --type=mapping
    
    if [ $? -eq 0 ]; then
        print_success "映射迁移成功"
    else
        print_error "映射迁移失败"
        return 1
    fi
    
    # 迁移数据
    print_warning "正在迁移数据 (data)..."
    elasticdump \
        --input="$source/$index" \
        --output="$target/$index" \
        --type=data \
        --limit=5000 \
        --timeout=120000 \
        --noRefresh
    
    if [ $? -eq 0 ]; then
        print_success "数据迁移成功"
    else
        print_error "数据迁移失败"
        return 1
    fi
    
    echo ""
    print_success "在线迁移完成！"
}

# 对比两个ES索引的文档数量
# 用法: compare_indices <source_es_url> <target_es_url> <index>
compare_indices() {
    local source=$1
    local target=$2
    local index=$3
    
    if [ -z "$source" ] || [ -z "$target" ] || [ -z "$index" ]; then
        print_error "请提供完整参数"
        echo "用法: compare_indices <source_es_url> <target_es_url> <index>"
        echo ""
        echo "预定义ES服务器："
        echo "  local    - 本地ES服务器"
        echo "  remote1  - 远程ES服务器1"
        echo "  remote2  - 远程ES服务器2（阿里云）"
        echo ""
        echo "示例："
        echo "  compare_indices local remote1 amap_poi"
        return 1
    fi
    
    # 转换预定义名称为实际URL
    case $source in
        local)
            source=$ES_LOCAL
            ;;
        remote1)
            source=$ES_REMOTE1
            ;;
        remote2)
            source=$ES_REMOTE2
            ;;
        remote3)
            source=$ES_REMOTE3
            ;;
    esac
    
    case $target in
        local)
            target=$ES_LOCAL
            ;;
        remote1)
            target=$ES_REMOTE1
            ;;
        remote2)
            target=$ES_REMOTE2
            ;;
        remote3)
            source=$ES_REMOTE3
            ;;
    esac
    
    print_title "对比索引文档数量: $index"
    echo "源ES: $source"
    echo "目标ES: $target"
    echo ""
    
    # 获取源ES文档数
    print_warning "查询源ES文档数..."
    local source_count=$(curl -s "$source/$index/_count" | jq -r '.count // 0')
    
    if [ "$source_count" = "0" ] || [ -z "$source_count" ]; then
        print_error "无法获取源ES文档数，请检查连接"
        return 1
    fi
    
    # 获取目标ES文档数
    print_warning "查询目标ES文档数..."
    local target_count=$(curl -s "$target/$index/_count" | jq -r '.count // 0')
    
    if [ -z "$target_count" ]; then
        target_count=0
    fi
    
    # 计算差值
    local diff=$((source_count - target_count))
    local progress=0
    if [ $source_count -gt 0 ]; then
        progress=$(echo "scale=2; $target_count * 100 / $source_count" | bc)
    fi
    
    echo ""
    print_title "对比结果"
    echo -e "${BLUE}源ES文档数:${NC}    $source_count"
    echo -e "${BLUE}目标ES文档数:${NC}  $target_count"
    echo -e "${BLUE}差值:${NC}          $diff"
    echo -e "${BLUE}迁移进度:${NC}      ${progress}%"
    echo ""
    
    if [ $diff -eq 0 ]; then
        print_success "✅ 两个索引文档数量一致，迁移完成！"
    elif [ $diff -gt 0 ]; then
        print_warning "⚠️  还有 $diff 条文档未迁移"
        echo ""
        echo "建议使用以下方式继续迁移："
        echo "  1. 使用 migrate_resume 断点续传（推荐）"
        echo "  2. 使用 migrate_online 重新迁移（会跳过已存在文档）"
    else
        print_error "❌ 目标ES文档数多于源ES，数据异常！"
    fi
}

# 断点续传迁移（基于Reindex API）
# 用法: migrate_resume <source_es_url> <target_es_url> <index>
migrate_resume() {
    local source=$1
    local target=$2
    local index=$3
    
    if [ -z "$source" ] || [ -z "$target" ] || [ -z "$index" ]; then
        print_error "请提供完整参数"
        echo "用法: migrate_resume <source_es_url> <target_es_url> <index>"
        echo ""
        echo "预定义ES服务器："
        echo "  local    - 本地ES服务器"
        echo "  remote1  - 远程ES服务器1"
        echo "  remote2  - 远程ES服务器2（阿里云）"
        echo ""
        echo "说明："
        echo "  使用ES Reindex API进行断点续传迁移"
        echo "  自动跳过已存在的文档（基于_id）"
        echo "  支持异步执行，可查询进度"
        echo ""
        echo "示例："
        echo "  migrate_resume local remote1 amap_poi"
        return 1
    fi
    
    # 转换预定义名称为实际URL
    local source_url=$source
    local target_url=$target
    
    case $source in
        local)
            source_url=$ES_LOCAL
            ;;
        remote1)
            source_url=$ES_REMOTE1
            ;;
        remote2)
            source_url=$ES_REMOTE2
            ;;
       remote3)
           source=$ES_REMOTE3
           ;;
    esac
    
    case $target in
        local)
            target_url=$ES_LOCAL
            ;;
        remote1)
            target_url=$ES_REMOTE1
            ;;
        remote2)
            target_url=$ES_REMOTE2
            ;;
        remote3)
            source=$ES_REMOTE3
            ;;
    esac
    
    print_title "断点续传迁移: $index"
    echo "源ES: $source_url"
    echo "目标ES: $target_url"
    echo ""
    
    print_warning "提示：此方法使用ES Reindex API，需要在目标ES上配置远程白名单"
    echo "在目标ES的elasticsearch.yml中添加："
    echo "  reindex.remote.whitelist: \"源ES的host:port\""
    echo ""
    
    read -p "确认继续? (yes/no): " confirm
    
    if [ "$confirm" != "yes" ]; then
        print_warning "取消操作"
        return 0
    fi
    
    # 提取源ES的认证信息和地址
    local source_host=$(echo "$source_url" | sed -E 's|https?://([^@]+@)?||' | sed 's|/.*||')
    local source_user=$(echo "$source_url" | grep -oP '(?<=://).*(?=:.*@)' || echo "")
    local source_pass=$(echo "$source_url" | grep -oP '(?<=:)[^@]+(?=@)' || echo "")
    
    # 构建reindex请求
    local reindex_body='{
  "source": {
    "remote": {
      "host": "http://'$source_host'"'
    
    if [ -n "$source_user" ]; then
        reindex_body+=',
      "username": "'$source_user'",
      "password": "'$source_pass'"'
    fi
    
    reindex_body+='
    },
    "index": "'$index'",
    "size": 1000
  },
  "dest": {
    "index": "'$index'",
    "op_type": "create"
  }
}'
    
    print_warning "正在提交Reindex任务（异步执行）..."
    
    # 提交reindex任务
    local response=$(curl -s "$target_url/_reindex?wait_for_completion=false" \
        -H 'Content-Type: application/json' \
        -d "$reindex_body")
    
    local task_id=$(echo "$response" | jq -r '.task // empty')
    
    if [ -z "$task_id" ]; then
        print_error "Reindex任务提交失败"
        echo "$response" | jq .
        return 1
    fi
    
    print_success "Reindex任务已提交，task_id: $task_id"
    echo ""
    echo "查询任务进度："
    echo "  curl -s \"$target_url/_tasks/$task_id\" | jq ."
    echo ""
    echo "或使用以下命令监控："
    echo "  watch -n 5 \"curl -s '$target_url/_tasks/$task_id' | jq '{status: .task.status, created: .task.status.created, updated: .task.status.updated, total: .task.status.total}'\""
}

# 批量导出所有索引
# 用法: dump_all <source_es_url> [output_dir]
dump_all() {
    local source=$1
    local output_dir=${2:-"$DUMP_DIR"}
    
    if [ -z "$source" ]; then
        print_error "请提供ES源地址"
        echo "用法: dump_all <source_es_url> [output_dir]"
        echo ""
        echo "预定义ES服务器："
        echo "  local    - 本地ES服务器"
        echo "  remote1  - 远程ES服务器1"
        echo "  remote2  - 远程ES服务器2（阿里云）"
        return 1
    fi
    
    check_elasticdump || return 1
    
    if ! command -v multielasticdump &> /dev/null; then
        print_error "multielasticdump 未安装！"
        echo "multielasticdump 与 elasticdump 一起安装"
        return 1
    fi
    
    # 转换预定义名称为实际URL
    case $source in
        local)
            source=$ES_LOCAL
            ;;
        remote1)
            source=$ES_REMOTE1
            ;;
        remote2)
            source=$ES_REMOTE2
            ;;
       remote3)
           source=$ES_REMOTE3
           ;;
    esac
    
    # 创建输出目录
    local timestamp=$(date +%Y%m%d_%H%M%S)
    local full_output_dir="$output_dir/full_backup_${timestamp}"
    mkdir -p "$full_output_dir"
    
    print_title "批量导出所有索引"
    echo "源ES: $source"
    echo "目标目录: $full_output_dir"
    echo ""
    
    print_warning "正在导出所有索引（排除系统索引）..."
    multielasticdump \
        --direction=dump \
        --input="$source" \
        --output="$full_output_dir" \
        --match='^[^.].*$'
    
    if [ $? -eq 0 ]; then
        echo ""
        print_success "批量导出完成！"
        echo ""
        print_title "导出目录信息"
        ls -lh "$full_output_dir"
    else
        print_error "批量导出失败"
        return 1
    fi
}

# ==================== 帮助信息 ====================

show_help() {
    echo -e ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Elasticsearch 常用命令脚本${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e ""
    echo -e "${GREEN}集群管理:${NC}"
    echo -e "  cluster_health          查看集群健康状态"
    echo -e "  cluster_state           查看集群状态"
    echo -e "  cluster_stats           查看集群统计信息"
    echo -e "  node_info               查看节点信息"
    echo -e ""
    echo -e "${GREEN}索引管理:${NC}"
    echo -e "  list_indices            列出所有索引"
    echo -e "  index_info <index>      查看索引详细信息"
    echo -e "  index_mapping <index>   查看索引映射"
    echo -e "  index_settings <index>  查看索引设置"
    echo -e "  index_stats <index>     查看索引统计信息"
    echo -e "  create_index <index>    创建索引"
    echo -e "  delete_index <index>    删除索引"
    echo -e "  refresh_index [index]   刷新索引（默认全部）"
    echo -e "  count_docs <index>      查看文档数量"
    echo -e "  recent_docs <index> [size]  查询最近更新的记录（默认10条）"
    echo -e "  bulk_update_field <index> <field> <value> [query]  批量更新字段值"
    echo -e ""
    echo -e "${GREEN}别名管理:${NC}"
    echo -e "  list_aliases                        列出所有别名"
    echo -e "  alias_info <alias>                  查看别名详细信息"
    echo -e "  create_alias <index> <alias>        创建别名"
    echo -e "  delete_alias <index> <alias>        删除别名"
    echo -e "  switch_alias <old> <new> <alias>    切换别名（原子操作）"
    echo -e ""
    echo -e "${GREEN}POI 索引专用:${NC}"
    echo -e "  poi_status              查看 POI 索引状态"
    echo -e "  create_poi_aliases      创建 POI 读写别名"
    echo -e "  delete_poi_index        删除 POI 索引（谨慎）"
    echo -e ""
    echo -e "${GREEN}数据迁移 (Elasticdump):${NC}"
    echo -e "  dump_to_file <source> <index> [output_dir]          离线导出索引到文件"
    echo -e "  load_from_file <target> <index> <mapping> <data>    离线从文件导入索引"
    echo -e "  migrate_online <source> <target> <index>            在线迁移索引"
    echo -e "  dump_all <source> [output_dir]                      批量导出所有索引"
    echo -e ""
    echo -e "${GREEN}断点续传工具:${NC}"
    echo -e "  compare_indices <source> <target> <index>           对比两个ES索引文档数"
    echo -e "  migrate_resume <source> <target> <index>            断点续传迁移（Reindex API）"
    echo -e ""
    echo -e "${GREEN}性能优化参数（已内置）:${NC}"
    echo -e "  --limit=5000        每次操作5000条数据（默认100）"
    echo -e "  --timeout=120000    超时时间120秒（默认30秒）"
    echo -e "  --noRefresh         跳过索引刷新，提升导入速度"
    echo -e ""
    echo -e "${GREEN}预定义ES服务器:${NC}"
    echo -e "  local     - 本地ES服务器 (localhost:19200)"
    echo -e "  remote1   - 远程ES服务器1 (47.76.191.223:19200)"
    echo -e "  remote2   - 远程ES服务器2 (阿里云)"
    echo -e ""
    echo -e "${GREEN}配置:${NC}"
    echo -e "  ES_URL:      $ES_URL"
    echo -e "  ES_USER:     $ES_USER"
    echo -e "  DUMP_DIR:    $DUMP_DIR"
    echo -e ""
    echo -e "${YELLOW}基本使用示例:${NC}"
    echo -e "  source ./scripts/es_commands.sh"
    echo -e "  cluster_health"
    echo -e "  list_indices"
    echo -e "  poi_status"
    echo -e "  create_alias amap_poi amap_poi_read"
    echo -e ""
    echo -e "${YELLOW}数据迁移示例:${NC}"
    echo -e "  # 离线导出本地POI索引到文件"
    echo -e "  dump_to_file local amap_poi"
    echo -e "  "
    echo -e "  # 离线导入文件到远程服务器"
    echo -e "  load_from_file remote1 amap_poi ./es_dumps/amap_poi_xxx.mapping.json ./es_dumps/amap_poi_xxx.data.json"
    echo -e "  "
    echo -e "  # 在线迁移：本地 -> 远程1"
    echo -e "  migrate_online local remote1 amap_poi"
    echo -e "  "
    echo -e "  # 批量导出本地所有索引"
    echo -e "  dump_all local"
    echo -e ""
    echo -e "${YELLOW}断点续传示例:${NC}"
    echo -e "  # 1. 对比源和目标ES的文档数，判断是否需要继续迁移"
    echo -e "  compare_indices local remote1 amap_poi"
    echo -e "  "
    echo -e "  # 2. 使用Reindex API断点续传（自动跳过已存在文档）"
    echo -e "  migrate_resume local remote1 amap_poi"
    echo -e "  "
    echo -e "  # 注意：migrate_resume需要在目标ES配置远程白名单"
    echo -e "  # elasticsearch.yml: reindex.remote.whitelist: \"源ES的host:port\""
    echo -e ""
    echo -e "${YELLOW}安装 Elasticdump:${NC}"
    echo -e "  npm install -g elasticdump"
    echo -e ""
}

# ==================== 主函数 ====================

main() {
    if [ $# -eq 0 ]; then
        show_help
        return 0
    fi
    
    local command=$1
    shift
    
    case $command in
        # 集群管理
        cluster_health|health)
            cluster_health
            ;;
        cluster_state|state)
            cluster_state
            ;;
        cluster_stats|stats)
            cluster_stats
            ;;
        node_info|nodes)
            node_info
            ;;
        
        # 索引管理
        list_indices|list|ls)
            list_indices
            ;;
        index_info|info)
            index_info "$@"
            ;;
        index_mapping|mapping)
            index_mapping "$@"
            ;;
        index_settings|settings)
            index_settings "$@"
            ;;
        index_stats|istats)
            index_stats "$@"
            ;;
        create_index|create)
            create_index "$@"
            ;;
        delete_index|delete)
            delete_index "$@"
            ;;
        refresh_index|refresh)
            refresh_index "$@"
            ;;
        count_docs|count)
            count_docs "$@"
            ;;
        recent_docs|recent)
            recent_docs "$@"
            ;;
        bulk_update_field|bulk_update|update_field)
            bulk_update_field "$@"
            ;;
        
        # 别名管理
        list_aliases|aliases)
            list_aliases
            ;;
        alias_info|ainfo)
            alias_info "$@"
            ;;
        create_alias|calias)
            create_alias "$@"
            ;;
        delete_alias|dalias)
            delete_alias "$@"
            ;;
        switch_alias|switch)
            switch_alias "$@"
            ;;
        
        # POI 索引专用
        poi_status|poi)
            poi_status
            ;;
        create_poi_aliases|cpoi)
            create_poi_aliases "$@"
            ;;
        delete_poi_index|dpoi)
            delete_poi_index
            ;;
        
        # 数据迁移 (Elasticdump)
        dump_to_file|dump|export)
            dump_to_file "$@"
            ;;
        load_from_file|load|import)
            load_from_file "$@"
            ;;
        migrate_online|migrate)
            migrate_online "$@"
            ;;
        dump_all|backup)
            dump_all "$@"
            ;;
        
        # 断点续传工具
        compare_indices|compare)
            compare_indices "$@"
            ;;
        migrate_resume|resume)
            migrate_resume "$@"
            ;;
        
        # 帮助
        help|-h|--help)
            show_help
            ;;
        
        *)
            print_error "未知命令: $command"
            show_help
            return 1
            ;;
    esac
}

# 如果脚本被直接执行（而不是被 source）
if [ "${BASH_SOURCE[0]}" = "${0}" ]; then
    main "$@"
else
    # 脚本被source时显示帮助
    show_help
fi
