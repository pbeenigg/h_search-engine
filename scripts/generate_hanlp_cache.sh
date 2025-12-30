#!/bin/bash

# HanLP 缓存文件生成脚本
# 用途：预先生成 HanLP 的二进制缓存文件，避免运行时首次加载的性能问题

set -e  # 遇到错误立即退出

echo "=========================================="
echo "HanLP 缓存文件生成脚本"
echo "=========================================="
echo ""

# 切换到项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

echo "项目根目录: $PROJECT_ROOT"
echo ""

# 检查 HanLP 数据目录
HANLP_DATA_DIR="backend/common/data"
if [ ! -d "$HANLP_DATA_DIR" ]; then
    echo "✗ 错误: HanLP 数据目录不存在: $HANLP_DATA_DIR"
    exit 1
fi

echo "HanLP 数据目录: $HANLP_DATA_DIR"
echo ""

# 方法1：使用 Maven 运行
echo "=========================================="
echo "方法1: 使用 Maven 运行缓存生成器"
echo "=========================================="
echo ""

if command -v mvn &> /dev/null; then
    echo "检测到 Maven，开始编译和运行..."
    
    # 复制 Java 文件到 common 模块
    cp scripts/HanLPCacheGenerator.java backend/common/src/main/java/
    
    # 编译
    echo ""
    echo "[1/2] 编译项目..."
    cd backend/common
    mvn compile -DskipTests -q
    
    # 运行
    echo ""
    echo "[2/2] 运行缓存生成器..."
    mvn exec:java \
        -Dexec.mainClass="HanLPCacheGenerator" \
        -Dexec.cleanupDaemonThreads=false \
        -q
    
    # 清理
    rm -f src/main/java/HanLPCacheGenerator.java
    
    cd "$PROJECT_ROOT"
    
else
    echo "✗ 未检测到 Maven，跳过方法1"
fi

echo ""
echo "=========================================="
echo "方法2: 直接使用 Java 运行"
echo "=========================================="
echo ""

# 查找 HanLP JAR 文件
HANLP_JAR=$(find ~/.m2/repository/com/hankcs/hanlp -name "*.jar" 2>/dev/null | head -1)

if [ -z "$HANLP_JAR" ]; then
    echo "✗ 未找到 HanLP JAR 文件"
    echo "  请先运行: mvn dependency:resolve"
else
    echo "找到 HanLP JAR: $HANLP_JAR"
    
    # 编译 Java 文件
    echo ""
    echo "[1/2] 编译缓存生成器..."
    javac -cp "$HANLP_JAR" scripts/HanLPCacheGenerator.java
    
    # 运行
    echo ""
    echo "[2/2] 运行缓存生成器..."
    cd backend/common
    java -cp "../../scripts:$HANLP_JAR" HanLPCacheGenerator
    cd "$PROJECT_ROOT"
    
    # 清理
    rm -f scripts/HanLPCacheGenerator.class
fi

echo ""
echo "=========================================="
echo "验证缓存文件"
echo "=========================================="
echo ""

# 检查生成的缓存文件
echo "检查缓存文件..."
CACHE_FILES=(
    "backend/common/data/dictionary/CoreNatureDictionary.txt.bin"
    "backend/common/data/dictionary/CoreNatureDictionary.ngram.txt.bin"
    "backend/common/data/dictionary/tc/s2t.txt.bin"
    "backend/common/data/dictionary/tc/t2s.txt.bin"
)

ALL_EXISTS=true
for file in "${CACHE_FILES[@]}"; do
    if [ -f "$file" ]; then
        SIZE=$(du -h "$file" | cut -f1)
        echo "  ✓ $file ($SIZE)"
    else
        echo "  ✗ $file (不存在)"
        ALL_EXISTS=false
    fi
done

echo ""
if [ "$ALL_EXISTS" = true ]; then
    echo "=========================================="
    echo "✓ 所有缓存文件生成成功！"
    echo "=========================================="
    exit 0
else
    echo "=========================================="
    echo "✗ 部分缓存文件生成失败"
    echo "=========================================="
    exit 1
fi
