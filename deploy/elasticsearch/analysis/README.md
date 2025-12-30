# Elasticsearch 同义词文件管理

## 📁 文件结构

```
deploy/elasticsearch/analysis/
├── hotel_synonyms.txt              # 主同义词文件（汇总，自动生成）
├── synonyms/                       # 分类同义词目录
│   ├── city_synonyms.txt          # 城市、商圈、区域同义词
│   ├── landmark_synonyms.txt      # 景点、地标同义词
│   ├── transport_synonyms.txt     # 机场、火车站同义词
│   ├── facility_synonyms.txt      # 设施、服务、房型同义词
│   └── brand_synonyms.txt         # 品牌同义词（补充）
└── README.md                       # 本文件
```

---

## 🎯 设计理念

### 为什么要分类管理？

1. **易于维护**：每个类别独立管理，职责清晰
2. **团队协作**：不同团队成员可以并行编辑不同类别
3. **版本控制**：Git 冲突更少，变更历史更清晰
4. **灵活组合**：可以选择性加载某些类别
5. **便于审核**：按类别审核更高效

---

## 📝 使用方法

### 1. 添加/修改同义词

**直接编辑分类文件**（推荐）：

```bash
# 编辑城市同义词
vim synonyms/city_synonyms.txt

# 编辑设施同义词
vim synonyms/facility_synonyms.txt
```

**格式说明**：

```
# 双向同义词（互相等价）
A,B,C

# 单向同义词（A、B 映射到 C）
A,B => C

# 注释
# 这是注释行
```

---

### 2. 合并生成主文件

编辑完分类文件后，运行合并脚本：

```bash
cd scripts
./merge_synonym_files.sh
```

脚本会自动：
- ✅ 读取所有分类文件
- ✅ 合并成主文件 `hotel_synonyms.txt`
- ✅ 添加时间戳和来源标记
- ✅ 验证文件格式
- ✅ 输出统计信息

---

### 3. 部署到 Elasticsearch

#### 方式一：Docker 部署

```bash
# 1. 确保同义词文件在正确位置
ls -la deploy/elasticsearch/analysis/hotel_synonyms.txt

# 2. 重启 Elasticsearch 容器
cd deploy/elasticsearch
docker-compose restart elasticsearch

# 3. 验证文件已加载
docker exec -it elasticsearch ls -la /usr/share/elasticsearch/config/analysis/
```

#### 方式二：热更新（推荐）

```bash
# 1. 更新同义词文件（已挂载到容器）
./scripts/merge_synonym_files.sh

# 2. 调用 ES API 重新加载分析器
curl -X POST "http://localhost:9200/hotels_v1/_reload_search_analyzers"

# 3. 验证效果
curl -X POST "http://localhost:9200/hotels_v1/_analyze" \
  -H 'Content-Type: application/json' \
  -d '{
    "analyzer": "cn_ik_smart_syn",
    "text": "BJ酒店"
  }'
```

---

## 🔍 分类文件说明

### 1. city_synonyms.txt - 城市同义词

**内容**：
- 直辖市（北京、上海、天津、重庆）
- 省会城市和主要城市
- 特别行政区（香港、澳门）
- 商圈和区域（CBD、三里屯等）

**示例**：
```
BJ,Beijing,Peking => 北京
CBD,中央商务区,Central Business District
```

**更新频率**：低（城市名称变化少）

---

### 2. landmark_synonyms.txt - 地标同义词

**内容**：
- 著名景点（故宫、外滩、西湖等）
- 地标建筑（东方明珠、小蛮腰等）
- 按城市分类组织

**示例**：
```
故宫,紫禁城,Forbidden City
外滩,The Bund
```

**更新频率**：低（地标相对稳定）

---

### 3. transport_synonyms.txt - 交通枢纽同义词

**内容**：
- 机场（含三字码）
- 火车站（高铁站）
- 按城市分类组织

**示例**：
```
首都机场,PEK,Capital Airport,北京首都国际机场
北京南站,Beijing South Railway Station
```

**更新频率**：中（新机场、新车站会增加）

---

### 4. facility_synonyms.txt - 设施同义词

**内容**：
- 酒店类型（酒店、民宿、青旅等）
- 房型（大床房、标准间等）
- 设施（WiFi、停车场、游泳池等）
- 服务（接送机、早餐等）

**示例**：
```
酒店,宾馆,饭店,旅店,旅馆,Hotel
大床房,双人房,King Room,King Bed Room
免费WiFi,Free WiFi,免费无线网络,WIFI
```

**更新频率**：中（新设施、新服务会增加）

---

### 5. brand_synonyms.txt - 品牌同义词

**内容**：
- 国际高端品牌（希尔顿、万豪等）
- 国际中高端品牌（索菲特、诺富特等）
- 国内连锁品牌（如家、汉庭等）

**示例**：
```
希尔顿,Hilton
万豪,Marriott
如家,Home Inn
```

**注意**：
- ⚠️ 品牌识别主要通过 **HanLP 自定义词典**实现
- 此文件仅作为**补充**，用于同义词扩展
- 避免与 HanLP 词典重复

**更新频率**：中（新品牌会增加）

---

## ⚙️ 配置说明

### Elasticsearch 分析器配置

在 `rebuild_hotels_index.json` 中配置：

```json
{
  "settings": {
    "analysis": {
      "filter": {
        "hotel_synonym": {
          "type": "synonym",
          "lenient": true,
          "expand": true,
          "updateable": true,
          "synonyms_path": "analysis/hotel_synonyms.txt"
        }
      },
      "analyzer": {
        "cn_ik_max_syn": {
          "type": "custom",
          "tokenizer": "ik_max_word",
          "filter": ["lowercase", "hotel_synonym"]
        },
        "cn_ik_smart_syn": {
          "type": "custom",
          "tokenizer": "ik_smart",
          "filter": ["lowercase", "hotel_synonym"]
        }
      }
    }
  }
}
```

**关键参数**：
- `updateable: true` - 支持热更新
- `expand: true` - 双向同义词自动扩展
- `lenient: true` - 忽略解析错误

---

## 🔄 工作流程

### 日常维护流程

```mermaid
graph LR
    A[发现需要添加的同义词] --> B[编辑对应分类文件]
    B --> C[运行合并脚本]
    C --> D[Git 提交]
    D --> E[部署到测试环境]
    E --> F[验证效果]
    F --> G{效果OK?}
    G -->|是| H[部署到生产环境]
    G -->|否| B
    H --> I[热更新 ES]
    I --> J[监控效果]
```

### 详细步骤

1. **编辑分类文件**
   ```bash
   vim synonyms/city_synonyms.txt
   ```

2. **合并生成主文件**
   ```bash
   ./scripts/merge_synonym_files.sh
   ```

3. **提交到 Git**
   ```bash
   git add deploy/elasticsearch/analysis/
   git commit -m "feat: 添加城市同义词 XXX"
   git push
   ```

4. **部署到测试环境**
   ```bash
   # 复制文件到测试环境
   scp hotel_synonyms.txt test-server:/path/to/elasticsearch/config/analysis/
   
   # 热更新
   curl -X POST "http://test-es:9200/hotels_v1/_reload_search_analyzers"
   ```

5. **验证效果**
   ```bash
   # 测试分词
   curl -X POST "http://test-es:9200/hotels_v1/_analyze" \
     -H 'Content-Type: application/json' \
     -d '{"analyzer": "cn_ik_smart_syn", "text": "BJ酒店"}'
   
   # 测试搜索
   curl -X GET "http://test-es:9200/hotels_v1/_search?q=BJ"
   ```

6. **部署到生产环境**
   ```bash
   # 备份当前文件
   ssh prod-server "cp /path/to/hotel_synonyms.txt /path/to/backups/hotel_synonyms_$(date +%Y%m%d_%H%M%S).txt"
   
   # 部署新文件
   scp hotel_synonyms.txt prod-server:/path/to/elasticsearch/config/analysis/
   
   # 热更新
   curl -X POST "http://prod-es:9200/hotels_v1/_reload_search_analyzers"
   ```

---

## ✅ 最佳实践

### 1. 命名规范

- ✅ 使用清晰的类别名称
- ✅ 文件名使用下划线分隔
- ✅ 注释使用中文，便于理解

### 2. 内容组织

- ✅ 按地理位置或功能分组
- ✅ 添加分隔符和注释
- ✅ 保持格式一致

### 3. 版本控制

- ✅ 每次修改都提交 Git
- ✅ 写清楚 commit message
- ✅ 重要变更打 tag

### 4. 测试验证

- ✅ 先在测试环境验证
- ✅ 使用 `_analyze` API 测试分词
- ✅ 实际搜索验证效果

### 5. 监控告警

- ✅ 监控零结果率变化
- ✅ 监控同义词命中率
- ✅ 定期审查同义词效果

---

## 🚨 注意事项

### ⚠️ 不要直接编辑主文件

`hotel_synonyms.txt` 是自动生成的，手动修改会被覆盖！

**正确做法**：
```bash
# ✅ 编辑分类文件
vim synonyms/city_synonyms.txt

# ✅ 运行合并脚本
./scripts/merge_synonym_files.sh
```

**错误做法**：
```bash
# ❌ 直接编辑主文件
vim hotel_synonyms.txt
```

---

### ⚠️ 避免循环映射

```bash
# ❌ 错误：循环映射
A => B
B => C
C => A

# ✅ 正确：统一映射到一个词
A,B,C => D
```

---

### ⚠️ 避免过度扩展

```bash
# ❌ 错误：过度扩展
酒店,宾馆,饭店,旅店,旅馆,客栈,民宿,青旅,招待所,度假村,公寓...

# ✅ 正确：合理分组
酒店,宾馆,饭店,旅店,旅馆,Hotel
民宿,客栈,Homestay,B&B
青年旅舍,青旅,Hostel,Youth Hostel
```

---

### ⚠️ 注意性能影响

- 同义词文件过大会影响分词性能
- 建议控制在 **1000 条规则**以内
- 定期清理无效或低频同义词

---

## 📊 统计信息

当前同义词统计（运行 `merge_synonym_files.sh` 查看）：

```
统计信息：
  - 总行数: 192
  - 同义词规则: 150+
  - 注释行数: 40+
```

---

## 🔗 相关文档

- [同义词管理与维护方案](../../../docs/同义词管理与维护方案.md)
- [同义词实现方案](../../../docs/同义词实现方案.md)
- [ES索引配置与实体类对比检查](../../../docs/ES索引配置与实体类对比检查.md)

