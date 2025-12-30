# Elasticsearch 插件目录说明

此文档从 `deploy/elasticsearch/plugins/README.md` 迁移而来。注意：`plugins/` 目录下不能包含普通文件（如 README.md、.DS_Store 等），否则 Elasticsearch 会将其识别为插件并在启动时报错：

> Plugin [README.md] is missing a descriptor properties file.

请仅在 `plugins/` 目录下放置：
- 有效的插件目录（包含 `plugin-descriptor.properties`）
- 或与 ES 版本完全匹配的插件 zip 包

以下为原插件说明内容：

## 推荐插件

### 1. IK 分词器 (elasticsearch-analysis-ik)
- 作用: 中文分词
- 下载: https://github.com/medcl/elasticsearch-analysis-ik
- 版本: 需与 ES 版本匹配 (示例：8.19.x)

### 2. 拼音分词器 (elasticsearch-analysis-pinyin)
- 作用: 拼音搜索支持
- 下载: https://github.com/medcl/elasticsearch-analysis-pinyin
- 版本: 需与 ES 版本匹配 (示例：8.19.x)

## 安装方式

### 方式一：解压插件到对应目录
```
plugins/
├── elasticsearch-analysis-ik/
│   ├── plugin-descriptor.properties
│   ├── elasticsearch-analysis-ik-<version>.jar
│   └── ...
└── elasticsearch-analysis-pinyin/
    ├── plugin-descriptor.properties
    ├── elasticsearch-analysis-pinyin-<version>.jar
    └── ...
```

### 方式二：放置 zip 文件（推荐）
```
plugins/
├── elasticsearch-analysis-ik-<version>.zip
└── elasticsearch-analysis-pinyin-<version>.zip
```

## 下载命令
```bash
# 创建插件目录
mkdir -p plugins

# 下载 IK 分词器（示例，版本需与 ES 8.19.x 匹配）
wget https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.19.0/elasticsearch-analysis-ik-8.19.0.zip -O plugins/elasticsearch-analysis-ik-8.19.0.zip

# 下载拼音分词器（示例，版本需与 ES 8.19.x 匹配）
wget https://github.com/medcl/elasticsearch-analysis-pinyin/releases/download/v8.19.0/elasticsearch-analysis-pinyin-8.19.0.zip -O plugins/elasticsearch-analysis-pinyin-8.19.0.zip
```

## 验证插件安装
启动 ES 后执行：
```bash
# 查看已安装插件
curl http://localhost:9200/_cat/plugins?v

# 测试 IK 分词器
curl -X POST "localhost:9200/_analyze?pretty" -H 'Content-Type: application/json' -d'
{
  "analyzer": "ik_max_word",
  "text": "北京香格里拉酒店"
}'

# 测试拼音分词器
curl -X POST "localhost:9200/_analyze?pretty" -H 'Content-Type: application/json' -d'
{
  "analyzer": "pinyin",
  "text": "北京"
}'
```

## 注意事项
1. 版本兼容性：插件版本必须与 Elasticsearch 版本完全匹配（当前镜像：8.19.6）
2. 文件权限：确保插件文件具备读取权限
3. 重启要求：安装或更新插件后需要重启 ES 集群
4. 目录整洁：`plugins/` 目录严禁放置 README、.DS_Store 等非插件文件
