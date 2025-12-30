### 需求描述

- 1、在路径：com.heytrip.hotel.search.ingest.route  创建一个名为 AmapPoiRoute.java 的Camel路由类，用于处理高德POI采集需求，入库在创建索引。
- 2、高德POI API文档：[AmapAPI-POI-2.0.md](AmapAPI-POI-2.0.md)
- 3、高德POI采集需求文档：[高德POI采集需求.md](高德POI采集需求.md)
- 4、高德POI采集API所需的关联数据文件：需要在 com.heytrip.hotel.search.ingest.route  创建一个名为 AmapConnectedDataRoute.java 的Camel路由类，用于处理入库和更新
   - 高德城市代码： [amap_adcode_citycode.csv](../../backend/app/src/main/resources/poi/amap_adcode_citycode.csv)    （格式：UTF-8）
   - 高德POI类型代码： [amap_poi_type.csv](../../backend/app/src/main/resources/poi/amap_poi_type.csv)      （格式：UTF-8）
- 5、高德POI-2.0接口返回：
   - 字段：
     - id：POI唯一标识
     - name：POI名称
     - type：POI类型
     - typecode：POI类型代码
     - address：地址
     - location：经纬度坐标
     - pcode：省份代码
     - pname：省份名称
     - citycode：城市代码
     - cityname：城市名称
     - adcode：区县代码
     - adname：区县名称
     - parent：上级行政区划
     - distance：距离（可选）
     - children：返回子 POI 信息
         - id：子 poi 唯一标识
         - name：子POI名称
         - subtype：子 poi 所属类型
         - typecode：子 poi 分类编码
         - address：子 poi 详细地址
         - location：子 poi 经纬度
         - sname：子 poi 分类信息
     - business：返回 poi 商业信息
         - business_area： poi 所属商圈
         - rating： poi 评分
         - cost： poi 人均消费
         - keytag： poi 标识，用于确认poi信息类型
         - rectag：用于再次确认信息类型

- 6、高德城市编码表:
    - 表名：amap_citycode
    - 字段：
      - id : 序号  
      - name: 名称
      - adcode：区县代码
      - citycode：城市代码
      - created_at：创建时间
      - updated_at：更新时间
- 7、 高德POI分类编码:      
    - 表名：amap_poitype
    - 字段：
      - id : 序号
      - typecode：POI类型代码
      - big_category_cn : 大类中文名称
      - mid_category_cn : 中类中文名称
      - sub_category_cn : 小类中文名称
      - big_category_en : 大类英文名称
      - mid_category_en : 中类英文名称
      - sub_category_en : 小类英文名称
      - created_at：创建时间
      - updated_at：更新时间
- 8、高德POI数据采集频率：1月一次、城市编码采集频率：3个月、POI类型编码采集频率：3个月  （均需要支持定时触发和手动触发两种方式）
- 9、高德POI API调用频率限制：默认每个Key每天日配额为：1000次，建议使用多个Key轮流调用以提高采集效率。（需要支持配置多个Key）
- 10、POI数据采集范围：全国所有城市（需要支持配置采集的城市列表）
- 11、POI数据采集类型：支持配置采集的POI类型列表
- 12、POI数据去重规则：根据POI的唯一标识 id 进行去重
- 13、POI数据存储：将采集到的POI数据存储到 amap_poi 表中
- 14、错误处理：
   - API调用失败时，记录错误日志并重试(最多重试3次)
   - 数据存储失败时，记录错误日志并跳过该条数据
   - 采集过程中出现异常时，记录错误日志并继续采集其他数据
- 15、日志记录：记录采集开始时间、结束时间、采集总数、成功数、失败数、耗时，采集总结信息等信息
- 16、配置文件：支持通过配置文件设置API Key列表、采集城市列表、采集POI类型列表、采集频率等参数
- 17、高德POI采集路由类 AmapPoiRoute.java 需要实现以下功能：
   - 读取配置文件，获取API Key列表、采集城市列表、采集POI类型列表
   - 单次请求的最大重试次数配置
   - 高德API Key轮询策略 （故障Key自动剔除)
   - 遍历采集城市列表和POI类型列表，构建API请求参数
   - 调用高德POI API，获取POI数据
   - 解析API返回的POI数据，进行去重处理
   - 将去重后的POI数据批量存储到 amap_poi 表中 （1000条提交一次） （支持增量更新）（使用事务确保关联数据的原子性更新）
   - API限流应对策略：：默认每个Key每天日配额为：1000次，如果实在没有请求配额了，则等待到第二天继续请求，或者切换到下一个Key继续请求，如果所有Key都没有请求配额了，则等待到第二天继续请求
   - 批量保存成功后，更新已采集POI的总数和成功数
   - 处理API调用失败和数据存储失败的错误情况
   - 然后通过Redis Stream事件将本次集采的POI数据发送到下游方法进行ES索引创建和清洗 (Index: hotels_amap_poi_${yyyyMMdd_HHmmss})
   - ES索引定义参考：
     - 索引名称：hotels_amap_poi_${yyyyMMdd_HHmmss}  （动态索引名称，包含时间戳）
     - 字段映射：
       - id：keyword
       - name：text + keyword
       - type：keyword
       - typecode：keyword
       - address：text + keyword
       - location：geo_point
       - pcode：keyword
       - pname：keyword
       - citycode：keyword
       - cityname：keyword
       - adcode：keyword
       - adname：keyword
       - parent：keyword
       - distance：float
       - created_at：date
       - updated_at：date
   - AmapPoiRoute (主采集路由)
     - ├── AmapApiRoute (API调用路由，负责HTTP请求和重试)
     - ├── AmapDataCleanRoute (数据清洗路由，负责数据校验和转换)
     - ├── AmapDataPersistRoute (数据持久化路由，负责入库)
     - └── AmapEsIndexRoute (ES索引路由，负责索引创建)
   - 注意POI数据是多层级的结构，需要进行扁平化处理后再存储到ES和数据库中（API响应数据： children：返回子 POI 信息）
   - 采集到没有数据返回，就是某一个采集指标完成采集，进入下一个采集指标
   - 采集指标定义：每一个城市+每一个POI类型 作为一个采集指标， 例如：北京市 + 餐饮服务，就是一个采集指标
   - 在job_schedule表中创建一条定时任务配置信息 （每月执行1次）
   - 将采集日志存入日志表 map_data_sync_log 中，字段包括：
       - id：日志唯一标识
       - source：数据来源（amap/openStreetMap）
       - data_type：数据类型（citycode/typecode/poi）
       - total_count：总记录数
       - success_count：成功记录数
       - failure_count：失败记录数
       - start_time：开始时间
       - end_time：结束时间
       - duration：耗时
       - status：状态（success/failure）
       - error_message：错误信息（如果有的话）
       - created_at：创建时间
       - updated_at：更新时间
   - 需要支持定时触发和手动触发两种方式
   - POI 2.0 API参数说明：
        - key：API Key  必填
        - keywords：地点关键字  必填（keyword 或者 types 二选一必填）
        - types：POI 类型代码   必填（keyword 或者 types 二选一必填）
        - region：搜索区划 可选 (加指定区域内数据召回权重，如需严格限制召回数据在区域内，请搭配使用 city_limit 参数，可输入 citycode，adcode，cityname；cityname 仅支持城市级别和中文，如“北京市”)
        - city_limit: 指定城市数据召回限制 可选  (可选值：true/false 为 true 时，仅召回 region 对应区域内数据。)
        - page_size：当前分页展示的数据条数  (age_size 的取值1-25，page_size 默认为10)
        - page_num： 请求第几分页 （默认值为1）

- 18、高德关联数据路由类 AmapConnectedDataRoute.java 需要实现以下功能：
   - 读取高德城市代码文件 src/main/resources/poi/amap_adcode_citycode.csv，将数据存储到 amap_citycode 表中 （支持增量更新）（使用事务确保关联数据的原子性更新）
   - 读取高德POI类型代码文件 src/main/resources/poi/amap_poi_type.csv，将数据存储到 amap_poitype 表中  （支持增量更新）（使用事务确保关联数据的原子性更新）
   - 支持定期更新关联数据表中的数据
   - 处理数据存储失败的错误情况
   - 记录数据更新的开始时间、结束时间、总数、成功数、失败数、耗时等信息
   - 支持通过Redis Stream事件触发关联数据的更新操作
   - 确保关联数据表中的数据始终保持最新状态
   - 需要支持定时触发和手动触发两种方式
   - 在job_schedule表中创建2条定时任务配置信息 （每3个月执行1次）
   - 将同步日志存入日志表 map_data_sync_log 中，字段包括：
       - id：日志唯一标识
       - source：数据来源（amap/openStreetMap）
       - data_type：数据类型（citycode/typecode/poi）
       - total_count：总记录数
       - success_count：成功记录数
       - failure_count：失败记录数
       - start_time：开始时间
       - end_time：结束时间
       - duration：耗时
       - status：状态（success/failure）
       - error_message：错误信息（如果有的话）
       - created_at：创建时间
       - updated_at：更新时间


- 20、amap_poi 表结构
```sql
-- 建议的表结构设计
CREATE TABLE amap_poi (
    id VARCHAR(50) PRIMARY KEY COMMENT 'POI唯一标识',
    name VARCHAR(255) NOT NULL COMMENT '地标名称',
    type VARCHAR(255) COMMENT 'POI类型',
    typecode VARCHAR(20) COMMENT 'POI类型代码',
    address VARCHAR(500) COMMENT '地址',
    location VARCHAR(50) COMMENT '经纬度坐标 格式:lng,lat',
    -- 建议拆分经纬度便于空间查询
    longitude DECIMAL(10, 6) COMMENT '经度',
    latitude DECIMAL(9, 6) COMMENT '纬度',
    pcode VARCHAR(10) COMMENT '省份代码',
    pname VARCHAR(50) COMMENT '省份名称',
    citycode VARCHAR(10) COMMENT '城市代码',
    cityname VARCHAR(50) COMMENT '城市名称',
    adcode VARCHAR(10) COMMENT '区县代码',
    adname VARCHAR(50) COMMENT '区县名称',
    parent VARCHAR(50) COMMENT '上级行政区划',
    distance VARCHAR(50) COMMENT '距离',
    data_version VARCHAR(20) COMMENT '数据版本号',
    source_batch VARCHAR(50) COMMENT '采集批次标识',
    is_deleted TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_citycode (citycode),
    INDEX idx_adcode (adcode),
    INDEX idx_typecode (typecode),
    INDEX idx_location (longitude, latitude),
    INDEX idx_name (name),
) COMMENT='高德POI数据表';
```

- 21、amap_citycode 表结构
```sql
-- 建议的表结构设计
CREATE TABLE amap_citycode (
   id VARCHAR(50) PRIMARY KEY COMMENT 'POI唯一标识',
    name VARCHAR(100) NOT NULL COMMENT '名称',
    adcode VARCHAR(10) NOT NULL COMMENT '区县代码',
    citycode VARCHAR(10) NOT NULL COMMENT '城市代码',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, 
    INDEX idx_citycode (citycode),
    INDEX idx_adcode (adcode),
    INDEX idx_name (name)
) COMMENT='高德城市代码表';
```

- 22、amap_poitype 表结构
```sql
-- 建议的表结构设计
CREATE TABLE amap_poitype (
    id VARCHAR(50) PRIMARY KEY COMMENT 'POI类型唯一标识',
    typecode VARCHAR(20) NOT NULL COMMENT 'POI类型代码',
    big_category_cn VARCHAR(100) COMMENT '大类中文名称',
    mid_category_cn VARCHAR(100) COMMENT '中类中文名称',
    sub_category_cn VARCHAR(100) COMMENT '小类中文名称',
    big_category_en VARCHAR(100) COMMENT '大类英文名称',
    mid_category_en VARCHAR(100) COMMENT '中类英文名称',
    sub_category_en VARCHAR(100) COMMENT '小类英文名称',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_typecode (typecode)
) COMMENT='高德POI类型代码表';
```


