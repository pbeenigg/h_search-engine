#!/usr/bin/env bash
set -eo pipefail

# ========================================
# 一键重建 hotels 索引并切换别名脚本
# ========================================
# 功能：
#   1. 创建新索引（带时间戳版本号）
#   2. 从旧索引回灌数据（reindex）
#   3. 原子性切换读写别名到新索引
#   4. 可选删除旧索引
#
# 依赖：curl, jq (可选)
#
# 用法示例：
#   # 交互式选择ES服务器
#   ./rebuild_hotels_index.sh
#
#   # 指定ES服务器（跳过选择菜单）
#   ./rebuild_hotels_index.sh 1  # 使用本地服务器
#   ./rebuild_hotels_index.sh 2  # 使用远程服务器1
#
#   # 自定义配置（环境变量优先级最高）
#   ES_URL=http://localhost:9200 \
#   ES_USER=elastic \
#   ES_PASSWORD=your_password \
#   ./rebuild_hotels_index.sh
# ========================================

# ========== 服务器选择 ==========
# 如果没有通过环境变量指定ES_URL，则显示选择菜单
if [ -z "${ES_URL:-}" ]; then
    # 检查是否通过命令行参数指定
    if [ -n "${1:-}" ] && [[ "${1:-}" =~ ^[1-4]$ ]]; then
        ES_CHOICE="$1"
    else
        echo "=========================================="
        echo "请选择 Elasticsearch 服务器:"
        echo "=========================================="
        echo "1) 本地服务器 (localhost:19200)"
        echo "2) 远程服务器1 (47.76.191.223:19200)"
        echo "3) 阿里云ES (es-cn-adn3rha130002yd4w...)"
        echo "4) 远程服务器3 (8.210.191.60:19200)"
        echo ""
        read -p "请输入选项 [1-4]: " ES_CHOICE
    fi
    
    # 验证输入
    if ! [[ "$ES_CHOICE" =~ ^[1-4]$ ]]; then
        echo "[ERR] 无效的选项: $ES_CHOICE" >&2
        exit 1
    fi
    
    # 根据选择设置ES配置
    case "$ES_CHOICE" in
        1)
            ES_URL="http://localhost:19200"
            ES_USER="elastic"
            ES_PASSWORD="DA6oHeV8sUbratbDDPnfSvLLGC1jk5FU"
            ES_NAME="本地服务器"
            ;;
        2)
            ES_URL="http://47.76.191.223:19200"
            ES_USER="elastic"
            ES_PASSWORD="DA6oHeV8sUbratbDDPnfSvLLGC1jk5FU"
            ES_NAME="远程服务器1"
            ;;
        3)
            ES_URL="http://es-cn-adn3rha130002yd4w.public.elasticsearch.aliyuncs.com:9200"
            ES_USER="elastic"
            ES_PASSWORD="h69PR3#QImku2#HWL!e2ug4kg7pw"
            ES_NAME="阿里云ES"
            ;;
        4)
            ES_URL="http://8.210.191.60:19200"
            ES_USER="elastic"
            ES_PASSWORD="DA6oHeV8sUbratbDDPnfSvLLGC1jk5FU"
            ES_NAME="远程服务器3"
            ;;
    esac
    
    echo ""
    echo "✓ 已选择: $ES_NAME"
    echo ""
else
    # 使用环境变量提供的配置
    ES_USER=${ES_USER:-"elastic"}
    ES_PASSWORD=${ES_PASSWORD:-"DA6oHeV8sUbratbDDPnfSvLLGC1jk5FU"}
fi

# 索引读别名
READ_ALIAS=${READ_ALIAS:-"hotels_read"}

# 索引写别名
WRITE_ALIAS=${WRITE_ALIAS:-"hotels_write"}

# 是否删除旧索引（除新索引外）
DELETE_OLD=${DELETE_OLD:-"false"}

# 当检测到与别名同名的索引时，是否强制删除该索引以避免冲突
FORCE_DELETE_ALIAS_NAMED_INDEX=${FORCE_DELETE_ALIAS_NAMED_INDEX:-"true"}

# 新索引名称（自动生成带时间戳）
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
NEW_INDEX=${NEW_INDEX:-"hotels_${TIMESTAMP}"}

# 定位映射文件（相对脚本目录）
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
MAPPING_FILE="$SCRIPT_DIR/rebuild_hotels_index.json"

if [ ! -f "$MAPPING_FILE" ]; then
  echo "[ERR] 映射文件不存在: $MAPPING_FILE" >&2
  exit 1
fi

# 构建认证参数（如果提供了用户名和密码）
if [ -n "$ES_USER" ] && [ -n "$ES_PASSWORD" ]; then
  AUTH_PARAM="-u ${ES_USER}:${ES_PASSWORD}"
  echo "使用认证: $ES_USER"
else
  AUTH_PARAM=""
  echo "无认证"
fi

echo "=========================================="
echo "开始重建索引流程"
echo "=========================================="
echo "ES 地址: $ES_URL"
echo "新索引名: $NEW_INDEX"
echo "读别名: $READ_ALIAS"
echo "写别名: $WRITE_ALIAS"
echo "删除旧索引: $DELETE_OLD"
echo "=========================================="

# ========== 步骤 1：检查并处理与别名同名的索引 ==========
echo ""
echo "[1/6] 检查别名冲突..."

conflict_index_exists() {
  local name="$1"
  local http_code
  http_code=$(curl -sS $AUTH_PARAM -o /dev/null -w "%{http_code}" "$ES_URL/$name")
  if [ "$http_code" = "200" ]; then
    # 进一步检查是否是索引（而非别名）
    local is_index
    is_index=$(curl -sS $AUTH_PARAM "$ES_URL/_cat/indices/$name?h=index" 2>/dev/null | grep -c "^$name$" || true)
    if [ "$is_index" -gt 0 ]; then
      return 0  # 是索引
    fi
  fi
  return 1  # 不是索引
}

for alias in "$READ_ALIAS" "$WRITE_ALIAS"; do
  if conflict_index_exists "$alias"; then
    if [ "$FORCE_DELETE_ALIAS_NAMED_INDEX" = "true" ]; then
      echo "  [WARN] 检测到与别名同名的索引 '$alias'，正在删除..."
      curl -sS $AUTH_PARAM -X DELETE "$ES_URL/$alias" | sed 's/.*/  [ES] &/'
      echo "  [OK] 已删除索引: $alias"
    else
      echo "  [ERR] 存在与别名同名的索引 '$alias'，这会导致别名操作失败。" >&2
      echo "  请先删除该索引，或设置 FORCE_DELETE_ALIAS_NAMED_INDEX=true 允许脚本自动删除。" >&2
      exit 2
    fi
  else
    echo "  [OK] 别名 '$alias' 无冲突"
  fi
done

# ========== 步骤 2：创建新索引 ==========
echo ""
echo "[2/6] 创建新索引: $NEW_INDEX"

# 检查新索引是否已存在
if curl -sS $AUTH_PARAM -f -o /dev/null "$ES_URL/$NEW_INDEX" 2>/dev/null; then
  echo "  [WARN] 索引 $NEW_INDEX 已存在，将先删除"
  curl -sS $AUTH_PARAM -X DELETE "$ES_URL/$NEW_INDEX" | sed 's/.*/  [ES] &/'
fi

CREATE_RESULT=$(curl -sS $AUTH_PARAM -X PUT "$ES_URL/$NEW_INDEX" \
  -H 'Content-Type: application/json' \
  --data-binary "@${MAPPING_FILE}")

echo "$CREATE_RESULT" | sed 's/.*/  [ES] &/'

if echo "$CREATE_RESULT" | grep -q '"acknowledged":true'; then
  echo "  [OK] 索引创建成功"
else
  echo "  [ERR] 索引创建失败" >&2
  exit 3
fi

# ========== 步骤 3：数据回灌 ==========
echo ""
echo "[3/6] 从别名 $WRITE_ALIAS 回灌数据到 $NEW_INDEX (reindex)..."

# 检查源索引/别名是否存在
if ! curl -sS $AUTH_PARAM -f -o /dev/null "$ES_URL/$WRITE_ALIAS/_count" 2>/dev/null; then
  echo "  [WARN] 源索引/别名 $WRITE_ALIAS 不存在，跳过数据回灌"
else
  SOURCE_COUNT=$(curl -sS $AUTH_PARAM "$ES_URL/$WRITE_ALIAS/_count" | grep -o '"count":[0-9]*' | cut -d: -f2)
  echo "  源索引文档数: $SOURCE_COUNT"
  
  if [ "$SOURCE_COUNT" -gt 0 ]; then
    echo "  正在回灌数据，请稍候..."
    REINDEX_RESULT=$(curl -sS $AUTH_PARAM -X POST "$ES_URL/_reindex?wait_for_completion=true" \
      -H 'Content-Type: application/json' \
      -d "{\"source\":{\"index\":\"$WRITE_ALIAS\"},\"dest\":{\"index\":\"$NEW_INDEX\"}}")
    
    echo "$REINDEX_RESULT" | sed 's/.*/  [ES] &/'
    
    CREATED=$(echo "$REINDEX_RESULT" | grep -o '"created":[0-9]*' | cut -d: -f2 || echo "0")
    echo "  [OK] 回灌完成，已创建 $CREATED 条文档"
  else
    echo "  [INFO] 源索引为空，跳过数据回灌"
  fi
fi

# ========== 步骤 4：原子性切换别名 ==========
echo ""
echo "[4/6] 原子性切换读写别名到新索引..."

# 获取当前持有别名的索引列表
list_indices_by_alias() {
  local alias_name="$1"
  curl -sS $AUTH_PARAM "$ES_URL/_cat/aliases/$alias_name?h=index" 2>/dev/null | awk 'NF>0 {print $1}' || true
}

READ_HOLDERS=$(list_indices_by_alias "$READ_ALIAS")
WRITE_HOLDERS=$(list_indices_by_alias "$WRITE_ALIAS")

echo "  当前 $READ_ALIAS 持有者: ${READ_HOLDERS:-无}"
echo "  当前 $WRITE_ALIAS 持有者: ${WRITE_HOLDERS:-无}"

# 构建别名切换 JSON
ACTIONS_JSON="["

# 移除旧的读别名
FIRST=true
for idx in $READ_HOLDERS; do
  if [ "$FIRST" = false ]; then
    ACTIONS_JSON+=","
  fi
  ACTIONS_JSON+="{\"remove\":{\"index\":\"$idx\",\"alias\":\"$READ_ALIAS\"}}"
  FIRST=false
done

# 添加新的读别名
if [ "$FIRST" = false ]; then
  ACTIONS_JSON+=","
fi
ACTIONS_JSON+="{\"add\":{\"index\":\"$NEW_INDEX\",\"alias\":\"$READ_ALIAS\"}}"
FIRST=false

# 移除旧的写别名
for idx in $WRITE_HOLDERS; do
  if [ "$FIRST" = false ]; then
    ACTIONS_JSON+=","
  fi
  ACTIONS_JSON+="{\"remove\":{\"index\":\"$idx\",\"alias\":\"$WRITE_ALIAS\"}}"
  FIRST=false
done

# 添加新的写别名
if [ "$FIRST" = false ]; then
  ACTIONS_JSON+=","
fi
ACTIONS_JSON+="{\"add\":{\"index\":\"$NEW_INDEX\",\"alias\":\"$WRITE_ALIAS\"}}"

ACTIONS_JSON+="]"

echo "  执行别名切换操作..."
ALIAS_RESULT=$(curl -sS $AUTH_PARAM -X POST "$ES_URL/_aliases" \
  -H 'Content-Type: application/json' \
  -d "{\"actions\":$ACTIONS_JSON}")

echo "$ALIAS_RESULT" | sed 's/.*/  [ES] &/'

if echo "$ALIAS_RESULT" | grep -q '"acknowledged":true'; then
  echo "  [OK] 别名切换成功"
  echo "  $READ_ALIAS -> $NEW_INDEX"
  echo "  $WRITE_ALIAS -> $NEW_INDEX"
else
  echo "  [ERR] 别名切换失败" >&2
  exit 4
fi

# ========== 步骤 5：删除旧索引（可选）==========
echo ""
if [ "$DELETE_OLD" = "true" ]; then
  echo "[5/6] 删除旧索引（保留新索引 ${NEW_INDEX})..."
  
  # 列出所有 hotels_* 索引
  OLD_INDICES=$(curl -sS $AUTH_PARAM "$ES_URL/_cat/indices/hotels_*?h=index" 2>/dev/null | awk 'NF>0 {print $1}' || true)
  
  if [ -z "$OLD_INDICES" ]; then
    echo "  [INFO] 没有找到旧索引"
  else
    DELETED_COUNT=0
    for idx in $OLD_INDICES; do
      if [ "$idx" != "${NEW_INDEX}" ]; then
        echo "  正在删除: $idx"
        DELETE_RESULT=$(curl -sS $AUTH_PARAM -X DELETE "$ES_URL/$idx")
        if echo "$DELETE_RESULT" | grep -q '"acknowledged":true'; then
          echo "    [OK] 已删除"
          DELETED_COUNT=$((DELETED_COUNT + 1))
        else
          echo "    [WARN] 删除失败"
            echo "$DELETE_RESULT" | sed 's/.*/    [ES] &/'
        fi
      fi
    done
    echo "  [OK] 共删除 $DELETED_COUNT 个旧索引"
  fi
else
  echo "[5/6] 跳过删除旧索引 (DELETE_OLD=false)"
fi

# ========== 步骤 6：验证结果 ==========
echo ""
echo "[6/6] 验证索引和别名状态..."

# 验证新索引
NEW_INDEX_COUNT=$(curl -sS $AUTH_PARAM "$ES_URL/$NEW_INDEX/_count" 2>/dev/null | grep -o '"count":[0-9]*' | cut -d: -f2 || echo "0")
echo "  新索引 $NEW_INDEX 文档数: $NEW_INDEX_COUNT"

# 验证读别名
echo ""
echo "  读别名 $READ_ALIAS 指向:"
curl -sS $AUTH_PARAM "$ES_URL/_cat/aliases/$READ_ALIAS?v" | sed 's/.*/    &/'

# 验证写别名
echo ""
echo "  写别名 $WRITE_ALIAS 指向:"
curl -sS $AUTH_PARAM "$ES_URL/_cat/aliases/$WRITE_ALIAS?v" | sed 's/.*/    &/'

# 显示所有 hotels 相关索引
echo ""
echo "  当前所有 hotels 相关索引:"
curl -sS $AUTH_PARAM "$ES_URL/_cat/indices/hotels_*?v&s=index" | sed 's/.*/    &/'

echo ""
echo "=========================================="
echo "✅ 索引重建完成！"
echo "=========================================="
echo "新索引: $NEW_INDEX"
echo "文档数: $NEW_INDEX_COUNT"
echo "读别名: $READ_ALIAS -> $NEW_INDEX"
echo "写别名: $WRITE_ALIAS -> $NEW_INDEX"
echo "=========================================="
echo ""
echo "验证命令："
if [ -n "$AUTH_PARAM" ]; then
  echo "  # 查看别名"
  echo "  curl -u $ES_USER:$ES_PASSWORD $ES_URL/_cat/aliases/$READ_ALIAS?v"
  echo "  curl -u $ES_USER:$ES_PASSWORD $ES_URL/_cat/aliases/$WRITE_ALIAS?v"
  echo ""
  echo "  # 搜索测试"
  echo "  curl -u $ES_USER:$ES_PASSWORD \"$ES_URL/$READ_ALIAS/_search?size=1\" | jq ."
  echo ""
  echo "  # 查看索引统计"
  echo "  curl -u $ES_USER:$ES_PASSWORD $ES_URL/$NEW_INDEX/_stats | jq .indices"
else
  echo "  # 查看别名"
  echo "  curl -sS $ES_URL/_cat/aliases/$READ_ALIAS?v"
  echo "  curl -sS $ES_URL/_cat/aliases/$WRITE_ALIAS?v"
  echo ""
  echo "  # 搜索测试"
  echo "  curl -sS \"$ES_URL/$READ_ALIAS/_search?size=1\" | jq ."
  echo ""
  echo "  # 查看索引统计"
  echo "  curl -sS $ES_URL/$NEW_INDEX/_stats | jq .indices"
fi
echo ""
