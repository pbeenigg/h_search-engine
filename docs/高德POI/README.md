# 高德POI数据采集系统使用文档

## 系统概述

高德POI数据采集系统是一个基于Apache Camel的分布式数据采集系统，用于从高德地图API采集POI（兴趣点）数据，并存储到MySQL数据库和Elasticsearch索引中。

## 系统架构

```
AmapPoiRoute (主采集路由)
  ├── AmapApiRoute (API调用路由，负责HTTP请求和重试)
  ├── AmapDataCleanRoute (数据清洗路由，负责数据校验和转换)
  ├── AmapDataPersistRoute (数据持久化路由，负责入库)
  └── AmapEsIndexRoute (ES索引路由，负责索引创建)

AmapConnectedDataRoute (关联数据路由)
  ├── 城市代码同步
  └── POI类型同步
```

## 核心功能

### 1. POI数据采集
- **采集策略**：按城市×POI类型的笛卡尔积进行采集
- **数据来源**：高德地图POI 2.0 API
- **采集频率**：每月1次（可配置）
- **触发方式**：定时触发 + 手动触发

### 2. 关联数据同步
- **城市代码同步**：从CSV文件导入城市代码数据（每3个月1次）
- **POI类型同步**：从CSV文件导入POI类型数据（每3个月1次）

### 3. 核心特性
- ✅ **多Key轮询**：支持配置多个API Key轮流使用
- ✅ **故障Key自动剔除**：失败的Key自动加入黑名单
- ✅ **API限流应对**：自动处理429限流响应
- ✅ **数据校验**：经纬度合法性、必填字段完整性校验
- ✅ **增量更新**：支持UPSERT操作，避免重复数据
- ✅ **子POI扁平化**：自动处理API返回的层级结构
- ✅ **批量入库**：1000条一批提交，提升性能
- ✅ **完整日志**：详细的同步日志和错误追踪
- ✅ **Redis Stream事件**：采集完成后发送事件到ES索引服务

## 快速开始

### 1. 环境准备

**必需组件：**
- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Elasticsearch 8.0+

### 2. 数据库初始化

执行SQL脚本初始化数据库：

```bash
mysql -u root -p hotel_search < docs/高德POI/amap_poi_init.sql
```

该脚本会创建以下表：
- `amap_poi` - POI数据表
- `amap_citycode` - 城市代码表
- `amap_poitype` - POI类型表
- `map_data_sync_log` - 同步日志表

并插入3条job_schedule定时任务配置。

### 3. 配置API Key

编辑 `application.yml` 或通过环境变量配置：

```yaml
amap:
  poi:
    api:
      keys:
        - YOUR_API_KEY_1
        - YOUR_API_KEY_2
        - YOUR_API_KEY_3
```

或环境变量：

```bash
export AMAP_API_KEY_1=your_key_1
export AMAP_API_KEY_2=your_key_2
export AMAP_API_KEY_3=your_key_3
```

### 4. 启动应用

```bash
cd backend/app
mvn spring-boot:run
```

### 5. 手动触发采集

#### 5.1 触发POI采集

```bash
curl -X POST http://localhost:18080/api/amap/poi/collect
```

#### 5.2 触发城市代码同步

```bash
curl -X POST http://localhost:18080/api/amap/citycode/sync
```

#### 5.3 触发POI类型同步

```bash
curl -X POST http://localhost:18080/api/amap/poitype/sync
```

## 配置说明

### 完整配置项

```yaml
amap:
  poi:
    # API配置
    api:
      base-url: https://restapi.amap.com
      keys: []                          # API Key列表
      key-rotation-strategy: ROUND_ROBIN # 轮询策略: ROUND_ROBIN/RANDOM
      key-blacklist-duration: 3600       # 黑名单时长(秒)
      max-retry-per-key: 3               # 每个Key最大重试次数
      daily-quota-per-key: 1000          # 每个Key每日配额
      
    # 采集配置
    collect:
      enabled: true                      # 是否启用采集
      enabled-cities: []                 # 城市白名单(空=全部)
      enabled-types: []                  # POI类型白名单(空=全部)
      page-size: 25                      # API单页大小
      db-commit-size: 1000               # 数据库批量提交大小
      city-limit: true                   # 是否严格限制城市范围
      
    # 性能配置
    performance:
      max-concurrent-requests: 10        # 最大并发请求数
      request-delay-ms: 200              # 请求间隔(毫秒)
      timeout-ms: 30000                  # HTTP超时(毫秒)
      page-delay-ms: 1000                # 页间延迟(毫秒)
      
    # 重试配置
    retry:
      max-attempts: 3                    # 最大重试次数
      base-delay-ms: 1000                # 基础延迟(毫秒)
      max-delay-ms: 10000                # 最大延迟(毫秒)
      backoff-multiplier: 2.0            # 退避乘数
      base429-delay-ms: 5000             # 429延迟(毫秒)
      
    # 数据校验配置
    validation:
      check-location: true               # 校验经纬度
      check-required-fields: true        # 校验必填字段
      location-bounds:                   # 经纬度范围
        min-lng: 73.0
        max-lng: 136.0
        min-lat: 3.0
        max-lat: 54.0
        
    # 定时任务配置
    schedule:
      poi-collect-cron: "0 0 0 1 * ?"   # POI采集Cron
      city-code-cron: "0 0 0 1 */3 ?"   # 城市代码同步Cron
      poi-type-cron: "0 0 0 1 */3 ?"    # POI类型同步Cron
```

### 配置示例

#### 示例1：只采集特定城市和类型

```yaml
amap:
  poi:
    collect:
      enabled-cities:
        - "110000"  # 北京
        - "310000"  # 上海
      enabled-types:
        - "141201"  # 高等院校
        - "010100"  # 加油站
```

#### 示例2：提高采集性能

```yaml
amap:
  poi:
    performance:
      max-concurrent-requests: 20
      request-delay-ms: 100
      page-delay-ms: 500
    collect:
      db-commit-size: 2000
```

## 监控和日志

### 1. 查看同步日志

```sql
-- 查看最近的同步日志
SELECT 
    id,
    source,
    data_type,
    total_count,
    success_count,
    failure_count,
    status,
    start_time,
    end_time,
    duration,
    error_message
FROM map_data_sync_log
ORDER BY start_time DESC
LIMIT 10;

-- 查看正在运行的任务
SELECT * FROM map_data_sync_log WHERE status = 'RUNNING';

-- 查看失败的任务
SELECT * FROM map_data_sync_log WHERE status = 'FAILED';
```

### 2. 查看POI数据统计

```sql
-- 按城市统计POI数量
SELECT cityname, COUNT(*) as count
FROM amap_poi
GROUP BY cityname
ORDER BY count DESC
LIMIT 20;

-- 按类型统计POI数量
SELECT typecode, type, COUNT(*) as count
FROM amap_poi
GROUP BY typecode, type
ORDER BY count DESC
LIMIT 20;

-- 查看最新采集的批次
SELECT DISTINCT source_batch
FROM amap_poi
ORDER BY created_at DESC
LIMIT 5;
```

### 3. 查看API Key状态

应用启动后，可以通过日志查看Key状态：

```log
[AmapApiKeyManager] 初始化完成，共3个API Key，每日配额：1000
[AmapApiKeyManager] 轮询策略选中Key: d878**** (剩余配额: 998)
[AmapApiKeyManager] Key使用成功: d878****, 今日已使用: 2/1000
```

## 故障排查

### 问题1：API返回10003错误（配额不足）

**原因**：当前Key的每日配额已用完

**解决方案**：
1. 等待到第二天自动重置
2. 配置更多的API Key
3. 手动重置Key状态（重启应用）

### 问题2：所有Key都不可用

**现象**：日志显示"所有API Key都不可用！"

**排查步骤**：
1. 检查配置文件中的Key是否正确
2. 查看Key状态日志，确认是否都在黑名单中
3. 等待黑名单过期（默认1小时）或重启应用

### 问题3：数据校验失败率高

**原因**：经纬度超出中国境内范围

**解决方案**：
调整经纬度范围配置或禁用校验：

```yaml
amap:
  poi:
    validation:
      check-location: false
```

### 问题4：POI数据重复

**原因**：UPSERT未生效或主键冲突

**排查步骤**：
1. 检查数据库主键约束
2. 查看日志中的错误信息
3. 手动清理重复数据：

```sql
-- 查找重复POI
SELECT id, COUNT(*) as count
FROM amap_poi
GROUP BY id
HAVING count > 1;

-- 删除旧批次数据
DELETE FROM amap_poi WHERE source_batch = 'old_batch_id';
```

## 性能优化建议

### 1. 数据库优化
- 为常用查询字段创建索引
- 定期清理历史数据
- 调整批量提交大小

### 2. 采集优化
- 增加并发请求数
- 减小页间延迟
- 配置多个API Key

### 3. 网络优化
- 调整HTTP超时时间
- 使用HTTP连接池
- 配置合理的重试策略

## 注意事项

1. **API配额限制**：每个Key每天1000次，合理规划采集任务
2. **数据更新频率**：POI数据每月更新一次即可，避免频繁调用
3. **黑名单机制**：失败的Key会被加入黑名单1小时，注意监控
4. **经纬度范围**：默认限制为中国境内，海外POI需调整配置
5. **批量入库**：建议每批1000条，过大会导致事务超时
6. **子POI处理**：系统会自动扁平化子POI，无需额外处理
7. **ES索引**：采集完成后自动发送事件，需确保Redis Stream消费者正常运行

## 技术支持

如有问题，请联系：
- 技术文档：`docs/高德POI/`
- 需求文档：`docs/高德POI/高德POI采集需求.md`
- API文档：`docs/高德POI/AmapAPI-POI-2.0.md`
