# ES 索引配置与实体类对比检查报告


**检查文件**:
- `backend/infra/scripts/rebuild_hotels_index.json`
- `backend/infra/src/main/java/com/heytrip/hotel/search/infra/search/doc/HotelIndexDoc.java`

---
## 📊 完整字段对比表

| 字段名 | Java 类型 | ES 类型 | 分析器 (索引/查询) | 子字段 | 状态 |
|--------|-----------|---------|-------------------|--------|------|
| **id** | String | - | - | - | ✅ |
| **tagSource** | String | keyword | - | - | ✅ |
| **providerSource** | String | keyword | lowercase_normalizer | - | ✅ |
| **hotelId** | Long | long | - | - | ✅ |
| **nameCn** | String | text | cn_ik_max_syn / cn_ik_smart_syn | keyword, pinyin | ✅ |
| **nameEn** | String | text | std_lc / std_lc | keyword | ✅ |
| **countryCn** | String | keyword | - | - | ✅ |
| **countryEn** | String | keyword | - | - | ✅ |
| **countryCode** | String | keyword | - | - | ✅ |
| **cityCn** | String | keyword | - | - | ✅ |
| **cityEn** | String | keyword | - | - | ✅ |
| **regionCn** | String | keyword | - | - | ✅ |
| **regionEn** | String | keyword | - | - | ✅ |
| **continentCn** | String | keyword | - | - | ✅ |
| **continentEn** | String | keyword | - | - | ✅ |
| **addressCn** | String | text | cn_ik_max_syn / cn_ik_smart_syn | pinyin | ✅ |
| **addressEn** | String | text | std_lc / std_lc | - | ✅ |
| **lat** | Double | double | - | - | ✅ |
| **lon** | Double | double | - | - | ✅ |
| **location** | Object | geo_point | - | - | ✅ |
| **groupCn** | String | keyword | - | - | ✅ |
| **groupEn** | String | keyword | - | - | ✅ |
| **brandCn** | String | keyword | lowercase_normalizer | - | ✅ |
| **brandEn** | String | keyword | lowercase_normalizer | - | ✅ |
| **descriptionCn** | String | keyword | - | - | ✅ |
| **descriptionEn** | String | keyword | - | - | ✅ |
| **nameTokens** | List\<String\> | keyword | - | - | ✅ |
| **addressTokens** | List\<String\> | keyword | - | - | ✅ |
| **nameKeywords** | List\<String\> | keyword | - | - | ✅ |
| **nerPlaces** | List\<String\> | keyword | - | - | ✅ |
| **nerBrands** | List\<String\> | keyword | - | - | ✅ |
| **descriptionKeywords** | List\<String\> | keyword | - | - | ✅ |
| **nameTraditional** | String | text | ik_max_word / ik_smart | - | ✅ 新增 |
| **addressTraditional** | String | text | ik_max_word / ik_smart | - | ✅ 新增 |
| **brandNames** | List\<String\> | keyword | - | - | ✅ 新增 |
| **geoHierarchy** | List\<String\> | keyword | - | - | ✅ 新增 |

---

## 🔍 分析器定义

### ES 配置中定义的分析器

| 分析器名称 | 类型 | Tokenizer | Filter | 用途 |
|-----------|------|-----------|--------|------|
| **std_lc** | custom | standard | lowercase, asciifolding | 英文分词 |
| **pinyin_analyzer** | custom | pinyin | lowercase | 拼音分词 |
| **cn_ik_max_syn** | custom | ik_max_word | lowercase, hotel_synonym | 中文索引（细粒度+同义词） |
| **cn_ik_smart_syn** | custom | ik_smart | lowercase, hotel_synonym | 中文查询（粗粒度+同义词） |

### Normalizer

| Normalizer 名称 | Filter | 用途 |
|----------------|--------|------|
| **lowercase_normalizer** | lowercase | keyword 字段小写化 |

### Filter

| Filter 名称 | 类型 | 配置 | 用途 |
|------------|------|------|------|
| **hotel_synonym** | synonym | synonyms_path: analysis/hotel_synonyms.txt | 同义词扩展 |





---

## 🚀 部署步骤

### 1. 删除旧索引
```bash
curl -X DELETE "http://localhost:9200/hotels_v1"
```

### 2. 创建新索引
```bash
curl -X PUT "http://localhost:9200/hotels_v1" \
  -H 'Content-Type: application/json' \
  -d @backend/infra/scripts/rebuild_hotels_index.json
```

### 3. 验证索引映射
```bash
curl -X GET "http://localhost:9200/hotels_v1/_mapping?pretty"
```

---

## ⚠️ 注意事项

### 1. 同义词文件
确保同义词文件存在：
```bash
/path/to/elasticsearch/analysis/hotel_synonyms.txt
```

### 2. 分析器插件
确保已安装必要的插件：
- IK 分词器插件
- Pinyin 分词器插件


