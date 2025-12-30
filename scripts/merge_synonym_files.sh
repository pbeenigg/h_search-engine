#!/bin/bash
#
# 同义词文件合并脚本
# 用途：将分类的同义词文件合并成一个主文件
#
# 使用方法：
#   ./merge_synonym_files.sh
#

set -e

# ========== 配置 ==========
SYNONYM_DIR="../deploy/elasticsearch/analysis"
CATEGORY_DIR="$SYNONYM_DIR/synonyms"
OUTPUT_FILE="$SYNONYM_DIR/hotel_synonyms.txt"
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

echo "=== 同义词文件合并开始 ==="
echo "分类目录: $CATEGORY_DIR"
echo "输出文件: $OUTPUT_FILE"
echo ""

# ========== 检查目录 ==========
if [ ! -d "$CATEGORY_DIR" ]; then
    echo "错误: 分类目录不存在: $CATEGORY_DIR"
    exit 1
fi

# ========== 创建主文件头部 ==========
cat > "$OUTPUT_FILE" <<EOF
# 酒店搜索同义词词典（主文件）
# 最后更新: $TIMESTAMP
#
# 格式说明：
# - A,B,C 表示双向同义词（A、B、C 互相等价）
# - A,B => C 表示单向同义词（搜索 A 或 B 时转换为 C，但搜索 C 不会匹配 A 或 B）
#
# 文件组织：
# - 本文件为汇总文件，包含所有同义词
# - 分类文件位于 synonyms/ 目录下，便于分类管理
# - 修改时建议直接修改分类文件，然后重新生成本文件
#
# 分类文件：
# - synonyms/city_synonyms.txt       - 城市、商圈、区域
# - synonyms/landmark_synonyms.txt   - 景点、地标
# - synonyms/transport_synonyms.txt  - 机场、火车站
# - synonyms/facility_synonyms.txt   - 设施、服务、房型
# - synonyms/brand_synonyms.txt      - 品牌（补充）
#
# ⚠️  警告：本文件由脚本自动生成，请勿手动编辑！
# ⚠️  如需修改，请编辑对应的分类文件，然后运行 merge_synonym_files.sh

EOF

echo "✓ 主文件头部已创建"

# ========== 合并分类文件 ==========
CATEGORY_FILES=(
    "city_synonyms.txt"
    "landmark_synonyms.txt"
    "transport_synonyms.txt"
    "facility_synonyms.txt"
    "brand_synonyms.txt"
)

for file in "${CATEGORY_FILES[@]}"; do
    file_path="$CATEGORY_DIR/$file"
    
    if [ ! -f "$file_path" ]; then
        echo "⚠️  警告: 文件不存在，跳过: $file"
        continue
    fi
    
    echo "正在合并: $file"
    
    # 添加分隔符
    echo "" >> "$OUTPUT_FILE"
    echo "# ==========================================" >> "$OUTPUT_FILE"
    echo "# 来源: synonyms/$file" >> "$OUTPUT_FILE"
    echo "# ==========================================" >> "$OUTPUT_FILE"
    echo "" >> "$OUTPUT_FILE"
    
    # 合并内容（跳过文件头部的注释）
    grep -v "^# 最后更新:" "$file_path" | \
    grep -v "^# .*同义词词典$" | \
    cat >> "$OUTPUT_FILE"
    
    echo "✓ $file 已合并"
done

# ========== 统计信息 ==========
echo ""
echo "=== 合并完成 ==="
echo ""

TOTAL_LINES=$(wc -l < "$OUTPUT_FILE")
SYNONYM_LINES=$(grep -v "^#" "$OUTPUT_FILE" | grep -v "^$" | wc -l)
COMMENT_LINES=$(grep "^#" "$OUTPUT_FILE" | wc -l)

echo "统计信息："
echo "  - 总行数: $TOTAL_LINES"
echo "  - 同义词规则: $SYNONYM_LINES"
echo "  - 注释行数: $COMMENT_LINES"
echo ""
echo "输出文件: $OUTPUT_FILE"
echo ""

# ========== 验证文件格式 ==========
echo "验证文件格式..."

# 检查是否有无效行（非注释、非空行、不包含 => 或 ,）
INVALID_LINES=$(grep -v "^#" "$OUTPUT_FILE" | grep -v "^$" | grep -v "=>" | grep -v "," | wc -l)

if [ "$INVALID_LINES" -gt 0 ]; then
    echo "⚠️  警告: 发现 $INVALID_LINES 行可能无效的同义词规则"
    echo "请检查以下行："
    grep -v "^#" "$OUTPUT_FILE" | grep -v "^$" | grep -v "=>" | grep -v "," | head -10
else
    echo "✓ 文件格式验证通过"
fi

echo ""
echo "=== 完成 ==="

exit 0
