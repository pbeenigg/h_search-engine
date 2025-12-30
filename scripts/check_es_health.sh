#!/bin/bash

################################################################################
# Elasticsearch 健康检查和诊断脚本
# 用于快速诊断ES索引问题
################################################################################

set -e

# 配置
ES_URL=${ES_URL:-"http://localhost:19200"}
ES_USER=${ES_USER:-"elastic"}
ES_PASSWORD=${ES_PASSWORD:-"DA6oHeV8sUbratbDDPnfSvLLGC1jk5FU"}
INDEX_ALIAS=${INDEX_ALIAS:-"hotels_read"}

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_header() {
    echo ""
    echo "=========================================="
    echo "$1"
    echo "=========================================="
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

# 认证参数
AUTH="-u ${ES_USER}:${ES_PASSWORD}"

print_header "Elasticsearch 健康检查"

# 1. 检查集群健康
print_info "检查集群健康状态..."
CLUSTER_HEALTH=$(curl -s $AUTH "$ES_URL/_cluster/health")
CLUSTER_STATUS=$(echo $CLUSTER_HEALTH | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
NODE_COUNT=$(echo $CLUSTER_HEALTH | grep -o '"number_of_nodes":[0-9]*' | cut -d':' -f2)
ACTIVE_SHARDS=$(echo $CLUSTER_HEALTH | grep -o '"active_primary_shards":[0-9]*' | cut -d':' -f2)
UNASSIGNED_SHARDS=$(echo $CLUSTER_HEALTH | grep -o '"unassigned_shards":[0-9]*' | cut -d':' -f2)

echo "  集群状态: $CLUSTER_STATUS"
echo "  节点数量: $NODE_COUNT"
echo "  活跃分片: $ACTIVE_SHARDS"
echo "  未分配分片: $UNASSIGNED_SHARDS"

if [ "$CLUSTER_STATUS" = "green" ]; then
    print_success "集群状态健康"
elif [ "$CLUSTER_STATUS" = "yellow" ]; then
    print_warning "集群状态为 yellow（单节点时正常，副本未分配）"
else
    print_error "集群状态为 red（存在问题）"
fi

# 2. 检查索引状态
print_header "检查索引状态"
print_info "索引别名: $INDEX_ALIAS"

# 检查别名指向
ALIAS_INDEX=$(curl -s $AUTH "$ES_URL/_cat/aliases/${INDEX_ALIAS}?h=index" | tr -d '\n')
if [ -z "$ALIAS_INDEX" ]; then
    print_error "别名 $INDEX_ALIAS 不存在或未指向任何索引"
    exit 1
else
    print_success "别名指向索引: $ALIAS_INDEX"
fi

# 检查索引健康
INDEX_HEALTH=$(curl -s $AUTH "$ES_URL/_cat/indices/${ALIAS_INDEX}?h=health,status,pri,rep,docs.count,store.size&format=json")
echo "$INDEX_HEALTH" | python3 -m json.tool 2>/dev/null || echo "$INDEX_HEALTH"

# 3. 检查分片状态
print_header "检查分片状态"
curl -s $AUTH "$ES_URL/_cat/shards/${ALIAS_INDEX}?v&h=index,shard,prirep,state,docs,store,node"

# 4. 检查location字段mapping
print_header "检查 location 字段映射"
LOCATION_MAPPING=$(curl -s $AUTH "$ES_URL/${INDEX_ALIAS}/_mapping/field/location?pretty")
echo "$LOCATION_MAPPING"

LOCATION_TYPE=$(echo "$LOCATION_MAPPING" | grep -o '"type" : "[^"]*"' | head -1 | cut -d'"' -f4)
if [ "$LOCATION_TYPE" = "geo_point" ]; then
    print_success "location 字段类型正确: geo_point"
else
    print_error "location 字段类型错误: $LOCATION_TYPE（应为 geo_point）"
fi

# 5. 测试geo_distance查询
print_header "测试地理位置查询"
print_info "测试查询广州附近10km的酒店..."

GEO_QUERY='{
  "size": 3,
  "query": {
    "bool": {
      "filter": {
        "geo_distance": {
          "distance": "10km",
          "location": {
            "lat": 23.1181,
            "lon": 113.2539
          }
        }
      }
    }
  },
  "_source": ["nameCn", "lat", "lon", "location"]
}'

GEO_RESULT=$(curl -s $AUTH "$ES_URL/${INDEX_ALIAS}/_search" \
    -H 'Content-Type: application/json' \
    -d "$GEO_QUERY")

TOTAL_HITS=$(echo "$GEO_RESULT" | grep -o '"total":{"value":[0-9]*' | grep -o '[0-9]*$')
FAILED_SHARDS=$(echo "$GEO_RESULT" | grep -o '"failed":[0-9]*' | grep -o '[0-9]*$')

if [ "$FAILED_SHARDS" = "0" ]; then
    print_success "地理查询成功，返回 $TOTAL_HITS 个结果"
    echo ""
    echo "查询结果示例:"
    echo "$GEO_RESULT" | python3 -m json.tool 2>/dev/null | grep -A 20 '"hits" : \[' | head -25
else
    print_error "地理查询失败，分片错误数: $FAILED_SHARDS"
    echo "$GEO_RESULT" | python3 -m json.tool 2>/dev/null
fi

# 6. 检查数据样本
print_header "数据样本检查"
SAMPLE=$(curl -s $AUTH "$ES_URL/${INDEX_ALIAS}/_search?size=2&pretty" \
    -H 'Content-Type: application/json' \
    -d '{"query":{"match_all":{}},"_source":["nameCn","lat","lon","location"]}')

echo "$SAMPLE" | python3 -m json.tool 2>/dev/null | grep -A 30 '"hits" : \[' | head -40

# 7. 性能建议
print_header "性能和配置建议"

if [ "$UNASSIGNED_SHARDS" -gt 0 ] && [ "$NODE_COUNT" = "1" ]; then
    print_warning "单节点集群有 $UNASSIGNED_SHARDS 个未分配副本分片（正常现象）"
    echo "  建议：生产环境应部署多节点集群以支持副本分片"
fi

echo ""
print_info "应用配置建议："
echo "  - connection-timeout: 5000ms 或更高"
echo "  - socket-timeout: 30000ms"
echo "  - 确保应用连接到正确的ES地址: $ES_URL"

echo ""
print_success "健康检查完成！"
