# Elasticsearch Docker 单节点部署指南

## 目录结构
```
deploy/elasticsearch/
├── docker-compose.yml          # Docker Compose 配置文件
├── config/
│   ├── elasticsearch.yml       # ES 主配置文件
│   └── jvm.options             # JVM 配置
├── plugins/                    # 自定义分词器插件目录
│   ├── elasticsearch-analysis-ik/
│   ├── elasticsearch-analysis-pinyin/
│   └── 其他插件...
├── deploy.sh                   # 一键部署脚本
└── README.md                   # 本文件
```

## 部署说明

### 1. 准备插件
将自定义分词器插件放入 `./plugins` 目录，例如：
- IK 分词器
- 拼音分词器
- 其他自定义插件

### 2. 系统优化（重要）
在宿主机上执行以下命令：

```bash
# 设置虚拟内存
echo 'vm.max_map_count=262144' >> /etc/sysctl.conf
sysctl -p

# 设置文件描述符限制
echo '* soft nofile 65536' >> /etc/security/limits.conf
echo '* hard nofile 65536' >> /etc/security/limits.conf
```

### 3. 启动服务
```bash
# 启动 Elasticsearch
./deploy.sh start

# 启动 Web 管理工具（可选）
./deploy.sh elasticvue    # 启动 Elasticvue
./deploy.sh elastic-hd    # 启动 ElasticHD

# 查看日志
./deploy.sh logs

# 查看集群状态
./deploy.sh health
```

### 4. 验证部署
```bash
# 检查 Elasticsearch 健康状态
curl http://localhost:9200/_cluster/health

# 检查节点信息
curl http://localhost:9200/_nodes

# 检查插件
curl http://localhost:9200/_nodes/plugins
```

## 服务访问

### Elasticsearch
- **单节点访问**: http://localhost:9200

### Web 管理工具（按需启动）
- **Elasticvue**: http://localhost:8080
- **ElasticHD**: http://localhost:9800

## 管理工具说明

### Elasticvue
- 现代化的 ES 管理界面
- 支持索引管理、查询、监控
- 响应式设计，支持移动端
- 启动方式：`./deploy.sh elasticvue`

### ElasticHD
- 轻量级的 ES 管理工具
- 简洁的界面，快速操作
- 适合日常运维管理
- 启动方式：`./deploy.sh elastic-hd`

## 配置说明

### 单节点配置
- **集群名称**: hotel-search-cluster
- **节点名称**: elasticsearch
- **发现模式**: single-node
- **内存配置**: 4GB JVM 堆内存
- **安全认证**: 已禁用（开发环境）

### 性能优化
- **GC算法**: G1GC
- **索引刷新**: 30秒
- **缓存配置**: 已优化内存分配
- **线程池**: 已调优队列大小

## 部署命令

### 基础命令
```bash
./deploy.sh start       # 启动 Elasticsearch
./deploy.sh stop        # 停止所有服务
./deploy.sh restart     # 重启 Elasticsearch
./deploy.sh status      # 查看服务状态
./deploy.sh health      # 检查健康状态
./deploy.sh logs        # 查看日志
```

### Web 管理工具
```bash
./deploy.sh elasticvue  # 启动 Elasticvue
./deploy.sh elastic-hd  # 启动 ElasticHD
```

### 维护命令
```bash
./deploy.sh cleanup     # 清理环境
./deploy.sh optimize    # 优化系统参数
./deploy.sh help        # 查看帮助
```

## profiles 使用说明

### Docker Compose Profiles
本配置使用 profiles 功能来选择性启动 Web 管理工具：

```bash
# 只启动 Elasticsearch
docker-compose up -d elasticsearch

# 启动 ES + Elasticvue
docker-compose --profile elasticvue up -d

# 启动 ES + ElasticHD
docker-compose --profile elastic-hd up -d

# 启动所有服务
docker-compose --profile elasticvue --profile elastic-hd up -d
```

### 推荐使用部署脚本
```bash
# 启动基础 ES 服务
./deploy.sh start

# 根据需要启动管理工具
./deploy.sh elasticvue    # 现代化界面
./deploy.sh elastic-hd    # 轻量级工具
```

## 监控命令

```bash
# 查看集群状态
curl http://localhost:9200/_cluster/health?pretty

# 查看节点统计
curl http://localhost:9200/_nodes/stats?pretty

# 查看索引状态
curl http://localhost:9200/_cat/indices?v

# 查看插件列表
curl http://localhost:9200/_cat/plugins?v

# 查看分片信息
curl http://localhost:9200/_cat/shards?v
```

## 故障排查

### 常见问题

1. **JVM启动失败 - Invalid -Xlog option**
   ```bash
   # 问题: GC日志配置不兼容
   # 解决: 已修复config/jvm.options，使用简化的JVM配置
   
   # 检查当前配置
   cat config/jvm.options
   

   ```

2. **启动失败 - 内存不足**
   ```bash
   # 调整JVM内存 (如果系统内存不足)
   # 修改 config/jvm.options 中的参数:
   -Xms2g  # 减少到2GB
   -Xmx2g
   ```

3. **集群状态为RED**
   ```bash
   # 检查磁盘空间
   docker exec elasticsearch df -h
   
   # 查看集群状态详情
   curl http://localhost:9200/_cluster/health?pretty
   
   # 查看ES日志
   docker logs elasticsearch
   ```

4. **插件加载失败**
   ```bash
   # 检查插件兼容性
   docker exec elasticsearch bin/elasticsearch-plugin list
   
   # 查看插件日志
   docker logs elasticsearch | grep plugin
   
   # 确保插件版本与ES版本匹配（8.19.6）
   ```

5. **连接超时**
   ```bash
   # 检查容器状态
   docker ps
   
   # 检查网络配置
   docker network ls
   docker network inspect elastic-network
   
   # 检查端口占用
   lsof -i :9200
   ```

6. **权限问题**
   ```bash
   # 运行预检查脚本
   ./pre-check.sh
   
   # 检查文件权限
   ls -la config/
   
   # 重新设置权限
   chmod 644 config/*.yml config/*.options
   ```

## 插件管理

### 推荐插件下载
```bash
cd plugins

# 下载 IK 分词器
wget https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.10.0/elasticsearch-analysis-ik-8.10.0.zip

# 下载拼音分词器
wget https://github.com/medcl/elasticsearch-analysis-pinyin/releases/download/v8.10.0/elasticsearch-analysis-pinyin-8.10.0.zip
```

### 插件测试
```bash
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

## 生产环境建议

### 资源配置
- **CPU**: 最少4核心
- **内存**: 16GB以上（ES堆内存设置为8GB）
- **存储**: SSD，至少200GB
- **网络**: 稳定网络连接

### 安全加固
```yaml
# 启用安全认证（生产环境）
xpack.security.enabled: true
xpack.security.transport.ssl.enabled: true
```

### 监控集成
- 集成 Prometheus + Grafana
- 配置告警规则
- 设置日志收集

## 备份策略
```bash
# 配置快照仓库
curl -X PUT "localhost:9200/_snapshot/my_backup" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/usr/share/elasticsearch/backup"
  }
}'

# 创建快照
curl -X PUT "localhost:9200/_snapshot/my_backup/snapshot_1?wait_for_completion=true"
```
