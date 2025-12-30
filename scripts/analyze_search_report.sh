#!/bin/bash
#
# 搜索日志分析脚本 - 自动发现潜在的同义词
# 用途：分析零结果查询，发现可能需要添加的同义词
#
# 使用方法：
#   ./analyze_search_report.sh [days]
#
# 参数：
#   days - 分析最近几天的日志（默认7天）
#

set -e

# ========== 配置 ==========
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="hotel_search"
DB_USER="root"
DB_PASS="password"

DAYS=${1:-7}
OUTPUT_DIR="./synonym_suggestions"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# ========== 创建输出目录 ==========
mkdir -p "$OUTPUT_DIR"

echo "=== 搜索日志分析开始 ==="
echo "分析时间范围: 最近 $DAYS 天"
echo "输出目录: $OUTPUT_DIR"
echo ""

# ========== 1. 导出零结果查询 ==========
echo "[1/5] 导出零结果查询..."

mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" <<EOF > "$OUTPUT_DIR/zero_results_${TIMESTAMP}.csv"
SELECT 
    query_text,
    COUNT(*) as search_count,
    AVG(response_time_ms) as avg_response_time
FROM search_logs
WHERE result_count = 0
    AND created_at >= DATE_SUB(NOW(), INTERVAL $DAYS DAY)
    AND query_text IS NOT NULL
    AND query_text != ''
GROUP BY query_text
HAVING search_count >= 3
ORDER BY search_count DESC
LIMIT 500;
EOF

ZERO_COUNT=$(wc -l < "$OUTPUT_DIR/zero_results_${TIMESTAMP}.csv")
echo "✓ 发现 $ZERO_COUNT 个零结果查询"

# ========== 2. 查找相似的成功查询 ==========
echo "[2/5] 查找相似的成功查询..."

mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" <<EOF > "$OUTPUT_DIR/successful_queries_${TIMESTAMP}.csv"
SELECT 
    query_text,
    COUNT(*) as search_count,
    AVG(result_count) as avg_results,
    SUM(click_count) as total_clicks
FROM search_logs
WHERE result_count > 0
    AND created_at >= DATE_SUB(NOW(), INTERVAL $DAYS DAY)
    AND query_text IS NOT NULL
    AND query_text != ''
GROUP BY query_text
HAVING search_count >= 5 AND total_clicks > 0
ORDER BY search_count DESC
LIMIT 1000;
EOF

SUCCESS_COUNT=$(wc -l < "$OUTPUT_DIR/successful_queries_${TIMESTAMP}.csv")
echo "✓ 发现 $SUCCESS_COUNT 个成功查询"

# ========== 3. 使用 Python 分析相似度 ==========
echo "[3/5] 分析查询相似度..."

python3 <<'PYTHON_SCRIPT' > "$OUTPUT_DIR/synonym_suggestions_${TIMESTAMP}.txt"
import csv
import difflib
from collections import defaultdict

# 读取零结果查询
zero_queries = []
with open('$OUTPUT_DIR/zero_results_${TIMESTAMP}.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f, delimiter='\t')
    for row in reader:
        if 'query_text' in row:
            zero_queries.append(row['query_text'].strip())

# 读取成功查询
success_queries = []
with open('$OUTPUT_DIR/successful_queries_${TIMESTAMP}.csv', 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f, delimiter='\t')
    for row in reader:
        if 'query_text' in row:
            success_queries.append(row['query_text'].strip())

# 查找相似查询
suggestions = []
for zero_q in zero_queries:
    # 使用 difflib 查找最相似的成功查询
    matches = difflib.get_close_matches(zero_q, success_queries, n=3, cutoff=0.6)
    
    for match in matches:
        similarity = difflib.SequenceMatcher(None, zero_q, match).ratio()
        suggestions.append({
            'source': zero_q,
            'target': match,
            'similarity': similarity
        })

# 输出建议
print("# 同义词建议")
print("# 生成时间:", "$TIMESTAMP")
print("# 格式: 源词 => 目标词 (相似度)")
print("")

for s in sorted(suggestions, key=lambda x: x['similarity'], reverse=True):
    print(f"{s['source']} => {s['target']}  # 相似度: {s['similarity']:.2f}")

print(f"\n# 总计: {len(suggestions)} 条建议")
PYTHON_SCRIPT

SUGGESTION_COUNT=$(grep -c "=>" "$OUTPUT_DIR/synonym_suggestions_${TIMESTAMP}.txt" || echo "0")
echo "✓ 生成 $SUGGESTION_COUNT 条同义词建议"

# ========== 4. 分析常见模式 ==========
echo "[4/5] 分析常见搜索模式..."

cat > "$OUTPUT_DIR/pattern_analysis_${TIMESTAMP}.txt" <<EOF
# 搜索模式分析

## 1. 英文缩写查询
$(grep -E "^[A-Z]{2,4}$" "$OUTPUT_DIR/zero_results_${TIMESTAMP}.csv" | head -20)

## 2. 拼音查询
$(grep -E "^[a-z]+$" "$OUTPUT_DIR/zero_results_${TIMESTAMP}.csv" | head -20)

## 3. 混合查询（中英文）
$(grep -E "[a-zA-Z].*[\u4e00-\u9fa5]|[\u4e00-\u9fa5].*[a-zA-Z]" "$OUTPUT_DIR/zero_results_${TIMESTAMP}.csv" | head -20)

## 4. 数字+文字查询
$(grep -E "[0-9]" "$OUTPUT_DIR/zero_results_${TIMESTAMP}.csv" | head -20)
EOF

echo "✓ 模式分析完成"

# ========== 5. 生成统计报告 ==========
echo "[5/5] 生成统计报告..."

cat > "$OUTPUT_DIR/report_${TIMESTAMP}.md" <<EOF
# 搜索日志分析报告

**生成时间**: $(date)  
**分析时间范围**: 最近 $DAYS 天

---

## 一、概览

| 指标 | 数值 |
|------|------|
| 零结果查询数 | $ZERO_COUNT |
| 成功查询数 | $SUCCESS_COUNT |
| 同义词建议数 | $SUGGESTION_COUNT |

---

## 二、Top 20 零结果查询

\`\`\`
$(head -20 "$OUTPUT_DIR/zero_results_${TIMESTAMP}.csv")
\`\`\`

---

## 三、同义词建议（Top 50）

\`\`\`
$(head -50 "$OUTPUT_DIR/synonym_suggestions_${TIMESTAMP}.txt")
\`\`\`

---

## 四、搜索模式分析

详见: \`pattern_analysis_${TIMESTAMP}.txt\`

---

## 五、建议行动

1. **高优先级**：审核相似度 > 0.8 的同义词建议
2. **中优先级**：分析英文缩写和拼音查询
3. **低优先级**：优化混合查询和数字查询

---

## 六、下一步

- [ ] 人工审核同义词建议
- [ ] 添加到同义词文件
- [ ] 测试验证效果
- [ ] 部署上线
- [ ] 监控效果

EOF

echo "✓ 报告生成完成"

# ========== 输出总结 ==========
echo ""
echo "=== 分析完成 ==="
echo ""
echo "输出文件："
echo "  - 零结果查询: $OUTPUT_DIR/zero_results_${TIMESTAMP}.csv"
echo "  - 成功查询: $OUTPUT_DIR/successful_queries_${TIMESTAMP}.csv"
echo "  - 同义词建议: $OUTPUT_DIR/synonym_suggestions_${TIMESTAMP}.txt"
echo "  - 模式分析: $OUTPUT_DIR/pattern_analysis_${TIMESTAMP}.txt"
echo "  - 统计报告: $OUTPUT_DIR/report_${TIMESTAMP}.md"
echo ""
echo "请查看报告并审核同义词建议"
echo ""

# ========== 可选：发送邮件通知 ==========
if command -v mail &> /dev/null; then
    echo "发送邮件通知..."
    cat "$OUTPUT_DIR/report_${TIMESTAMP}.md" | mail -s "搜索日志分析报告 - $TIMESTAMP" admin@example.com
    echo "✓ 邮件已发送"
fi

exit 0
