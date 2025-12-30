# 高德POI系统 - Redis缓存配置说明

## 缓存实现变更

系统已从**Caffeine本地缓存**升级为**Redis分布式缓存**。

### 变更原因

| 对比项 | Caffeine | Redis |
|--------|----------|-------|
| **适用场景** | 单实例应用 | 集群/分布式应用 |
| **数据共享** | 进程内独立 | 多实例共享 |
| **持久化** | 不支持 | 支持持久化 |
| **容量限制** | 受JVM内存限制 | 可扩展至TB级 |
| **缓存一致性** | 仅本地 | 全局一致 |

## 缓存配置

### 缓存名称和过期时间

```java
// 城市代码缓存：24小时
CACHE_CITYCODE = "amap:citycode"

// POI类型缓存：24小时  
CACHE_POITYPE = "amap:poitype"

// 采集状态缓存：1小时
CACHE_COLLECT_STATUS = "amap:collect:status"
```

### Redis Key格式

```
amap:citycode::all                    # 所有城市代码
amap:citycode::cities                 # 城市级别代码
amap:citycode::adcode:110000          # 按adcode查询
amap:citycode::citycode:0110          # 按citycode查询
amap:citycode::name:北京市             # 按名称查询

amap:poitype::all                     # 所有POI类型
amap:poitype::typecode:141201         # 按typecode查询
amap:poitype::bigcat:住宿服务          # 按大类查询

amap:collect:status::status           # 采集任务状态
```

## 序列化配置

- **Key序列化**: StringRedisSerializer
- **Value序列化**: GenericJackson2JsonRedisSerializer（JSON格式）
- **支持类型**: 复杂对象、集合、日期时间等

## 使用方式

### 1. 服务层自动缓存

```java
@Service
public class AmapCachedDataService {
    
    // 自动缓存查询结果
    @Cacheable(value = AmapCacheConfig.CACHE_CITYCODE, key = "'all'")
    public List<AmapCitycode> findAllCitycodes() {
        return citycodeRepository.findAll();
    }
    
    // 清除指定缓存
    @CacheEvict(value = AmapCacheConfig.CACHE_CITYCODE, key = "'all'")
    public void evictAllCitycodeCaches() {
        log.info("清除所有城市代码缓存");
    }
}
```

### 2. REST API管理缓存

```bash
# 清除所有缓存
curl -X POST http://localhost:18080/api/amap/poi/cache/clear

# 清除城市代码缓存
curl -X POST http://localhost:18080/api/amap/poi/cache/clear?type=citycode

# 清除POI类型缓存
curl -X POST http://localhost:18080/api/amap/poi/cache/clear?type=poitype
```

### 3. Redis CLI查看缓存

```bash
# 连接Redis
redis-cli

# 查看所有高德POI相关缓存key
KEYS amap:*

# 查看城市代码缓存
GET "amap:citycode::all"

# 查看缓存TTL
TTL "amap:citycode::all"

# 手动删除缓存
DEL "amap:citycode::all"

# 查看所有缓存key的数量
KEYS amap:* | wc -l
```

## 缓存预热

系统首次启动时，建议手动触发数据同步以预热缓存：

```bash
# 1. 同步城市代码（会自动缓存）
curl -X POST http://localhost:18080/api/amap/poi/sync/citycode

# 2. 同步POI类型（会自动缓存）
curl -X POST http://localhost:18080/api/amap/poi/sync/poitype
```

## 缓存清理策略

### 自动清理

数据同步完成后自动清除相关缓存：

```java
// 城市代码同步完成后
cachedDataService.evictAllCitycodeCaches();

// POI类型同步完成后
cachedDataService.evictAllPoitypeCaches();
```

### 手动清理

通过REST API或直接操作Redis。

## 监控和调优

### 查看缓存命中率

可通过Redis监控工具查看：
- 缓存命中次数
- 缓存未命中次数
- 缓存命中率

### 调整过期时间

修改`AmapCacheConfig.java`中的TTL配置：

```java
// 城市代码缓存：调整为48小时
cacheConfigurations.put(CACHE_CITYCODE, 
    defaultConfig.entryTtl(Duration.ofHours(48)));
```

## 注意事项

1. **Redis连接**：确保Redis服务正常运行
2. **序列化兼容**：实体类需可序列化
3. **内存管理**：合理设置Redis内存上限
4. **缓存穿透**：已禁用null值缓存
5. **事务支持**：已启用事务感知

## 故障排查

### 问题1：缓存不生效

检查项：
- Redis连接是否正常
- `@EnableCaching`注解是否启用
- 方法是否被Spring代理

### 问题2：缓存数据格式错误

检查项：
- 实体类是否有无参构造函数
- Jackson序列化配置是否正确

### 问题3：缓存占用过多内存

解决方案：
- 减少缓存TTL
- 限制缓存的数据量
- 配置Redis内存淘汰策略

## 总结

✅ Redis分布式缓存支持集群环境  
✅ 自动缓存和清理机制  
✅ 支持REST API管理  
✅ 合理的过期时间配置  
✅ 完善的监控和故障排查手段
