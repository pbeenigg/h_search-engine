## 高德地图 API

### 高德搜索 POI 2.0 GET

https://restapi.amap.com/v5/place/text

**API key: d87806426ef460a525c695566f207ff4**

注意：

- types 参数需要通过加载 [amap_poi_type.csv](amap_poi_type.csv) 文件来获取选择 （NEW_TYPE 字段）
- region 参数需要通过加载 [amap_adcode_citycode.csv](amap_adcode_citycode.csv) 文件来获取选择 （citycode，adcode，cityname；cityname 字段都行）
- key 参数需要通过高德地图 API Key 获取
- 参数 keyword 或者 types 二选一必填

#### 参数

| 参数名    | 是否必填 | 类型   | 说明       |
| --------- | -------- | ------ | ---------- |
| key       | 是       | String | API Key    |
| keywords  | 是       | String | 搜索关键词 |
| types     | 否       | String | POI 类型   |
| region    | 否       | String | 城市名称   |
| page_size | 否       | Int    | 每页数量   |
| page_num  | 否       | Int    | 页码       |

#### 请求示例

```url
curl --location --request GET 'https://restapi.amap.com/v5/place/text?key=d87806426ef460a525c695566f207ff4&keywords=KFC&types=141201&region=广州&page_size=20&page_num=1' \
--header 'User-Agent: Apifox/1.0.0 (https://apifox.com)' \
--header 'Accept: */*' \
--header 'Host: restapi.amap.com' \
--header 'Connection: keep-alive'
```

#### 返回结构

```json
{
  "count": "2",
  "infocode": "10000",
  "pois": [
    {
      "parent": "",
      "address": "五山路483号",
      "distance": "",
      "pcode": "440000",
      "adcode": "440106",
      "pname": "广东省",
      "cityname": "广州市",
      "type": "科教文化服务;学校;高等院校",
      "typecode": "141201",
      "adname": "天河区",
      "citycode": "020",
      "name": "华南农业大学",
      "location": "113.357553,23.158017",
      "id": "B00140TVEV"
    },
    {
      "parent": "",
      "address": "新港西路135号",
      "distance": "",
      "pcode": "440000",
      "adcode": "440105",
      "pname": "广东省",
      "cityname": "广州市",
      "type": "科教文化服务;学校;高等院校",
      "typecode": "141201",
      "adname": "海珠区",
      "citycode": "020",
      "name": "中山大学(广州校区南校园)",
      "location": "113.298214,23.096746",
      "id": "B00141IHRZ"
    }
  ],
  "status": "1",
  "info": "OK"
}
```

#### 返回字段说明

| 字段名   | 类型   | 说明     |
| -------- | ------ | -------- |
| count    | Int    | POI 数量 |
| infocode | String | 状态码   |
| pois     | Array  | POI 列表 |
| status   | String | 状态     |
| info     | String | 信息     |

| pois 字段说明 | 类型   | 说明         |
| ------------- | ------ | ------------ |
| parent        | String | 父级         |
| address       | String | 地址         |
| distance      | String | 距离         |
| pcode         | String | 省份代码     |
| adcode        | String | 城市代码     |
| pname         | String | 省份名称     |
| cityname      | String | 城市名称     |
| type          | String | POI 类型     |
| typecode      | String | POI 类型代码 |
| adname        | String | 区县名称     |
| citycode      | String | 城市代码     |
| name          | String | POI 名称     |
| location      | String | 位置         |
| id            | String | POI ID       |

### 高德搜索 POI-2.0
