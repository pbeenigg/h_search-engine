## 酒店数据源接口 API 文档

### 签名认证机制
鉴权方式就是简单的md5加密一下，由服务端给调用者颁发app和secret，然后调用者每次请求都在http headers里面携带三个参数：sign（每次调用的签名）、app（调用者的应用编号）、timestamp（秒级时间戳）
sign=Md5("app"+调用者的应用编号+"secret"+调用者的应用秘钥+"timestamp"+秒级时间戳)
比如： app123secret456timestamp1234567890
MD5后的值应该是： d1f495b9a6c39ff5467226e554887cc7

### 接口规定
- 所有请求头必须携带 app、sign、timestamp 三个参数
- sign 计算规则见上面的签名认证机制
- timestamp 允许误差范围为 10 分钟，超过则拒绝请求
- 返回数据格式均为 JSON
- 错误码说明：
  - code=0 表示成功
  - code!=0 表示失败，具体错误信息见 message 字段
  - bizCode 为业务错误码，0表示成功，非0表示业务失败
  - isOk 布尔值，true表示请求成功，false表示失败
  - hasData 布尔值，true表示有数据返回，false表示无数据
  - costTime 接口处理耗时，单位秒
- 所有接口超时时间设置为 20 秒
- 所有接口限制并发数为 10个
- 获取在线酒店编号:分页查询均采用 maxHotelId 方式进行分页
- 获取在线酒店编号：单次请求返回数据量不超过 10000 条
- 获取酒店详情原文：单次请求不超过 20 个酒店编号
- 没有提供变更标识和更新时间字段


## 获取在线酒店编号   GET  /api/StandardHotel/GetOnlineHotelIds
### 参数：
- maxHotelId  上一次请求返回最大酒店Id
- pageSize    每次返回数量,默认1000
- timestamp  酒店创建时间,格式长度为10的时间戳：1603415966；默认0，返回所有酒店Id;否则返回改时间创建之后的酒店Id

### 说明
分页查询，返回的酒店ID包含国内酒店｜国际酒店， 港澳台酒店 ID 从国际酒店规则里区分出来，CN=国内（艺龙）；INTL=国际（Agoda）； HMT = 港澳台（Agoda）  
返回数据字段说明：如果newHotelNameCn，newHotelNameEn不为空，说明人工修改过酒店名称  
 - newHotelNameCn:人工修改酒店名称(中文); 
 - newHotelNameEn:人工修改酒店名称(英文);

ID规则：  
- 国内酒店ID >=  2000万
- 国际酒店ID <  2000万

http 请求示例：
```http
curl --location --request GET 'http://39.108.194.81:9031/api/StandardHotel/GetOnlineHotelIds?pageSize=100&timestamp=1761535071' \
--header 'app: app' \
--header 'sign: c5ef0874b9ae25eba445b4dbdcc67269' \
--header 'timestamp: 1718257052' \
--header 'User-Agent: Apifox/1.0.0 (https://apifox.com)' \
--header 'app: Pax' \
--header 'timestamp: 1761535071' \
--header 'sign: 389c478d3dddc69dd36f6a8f661c7fc0' \
--header 'Accept: */*' \
--header 'Host: 39.108.194.81:9031' \
--header 'Connection: keep-alive'
```

返回示例：
```json
{
  "data": {
    "maxHotelId": 100022,
    "hotelIds": [
      100003,
      100004,
      100005,
      100007,
      100011,
      100012,
      100015,
      100018,
      100019,
      100022
    ]
  },
  "page": null,
  "hasData": true,
  "code": 0,
  "bizCode": 0,
  "message": null,
  "costTime": "0.122s",
  "isOk": true
}
```

## 获取酒店详情原文 GET /api/StandardHotel/GetHotelOrigContent

### 参数：
- hotelIds   酒店编号 (只支持多个，用逗号分隔，单次请求不超过20个)
- 
### 说明
返回酒店的原始数据内容，包含国内酒店和国际酒店 

http 请求示例：
```http
curl --location --request GET 'http://39.108.194.81:9031/api/StandardHotel/GetHotelOrigContent?hotelIds=100004' \
--header 'User-Agent: Apifox/1.0.0 (https://apifox.com)' \
--header 'app: Pax' \
--header 'timestamp: 1761639179' \
--header 'sign: e0554b4eec61f77654991b3eebf2b9a0' \
--header 'Accept: */*' \
--header 'Host: 39.108.194.81:9031' \
--header 'Connection: keep-alive'
```

### origContent 字段说明：
- origContent: 酒店原始数据内容，JSON字符串格式，需要进行二次反序列化处理, 有转义字符，需要进行处理。
- origContent 有两种结构：
  - 国内酒店数据结构 (来源艺龙)
  - 国际酒店数据结构（来源Agoda）

整体返回示例：
```json
{
  "data": [
    {
      "hotelId": 100004,
      "origContent": "{\"propertyId\":2061174,\"summary\":{\"propertyName\":{\"englishName\":\"\",\"localName\":\"\",\"displayName\":\"\"},\"address\":{\"address1\":null,\"address2\":null,\"areaName\":null,\"cityName\":null,\"regionName\":null,\"stateName\":null,\"stateId\":0,\"countryName\":null,\"postalCode\":null},\"gmtOffset\":null,\"cityId\":0,\"cityName\":\"\",\"countryNameEn\":\"\",\"countryId\":0,\"countryCode\":\"\",\"starRating\":{\"rating\":0.0,\"type\":1,\"text\":\"This star rating is provided to Agoda by the property, and is usually determined by an official hotel ratings organisation or a third party.\"},\"rating\":{\"score\":0.0,\"type\":1},\"coordinate\":null,\"accommodationType\":{\"id\":0,\"englishName\":\"None\",\"localName\":\"\"},\"propertyType\":0,\"hasHostExperience\":false,\"blockedNationalities\":[],\"propertyUrl\":\"/zh-cn/deface-victory-suites-kuala-lumpur/hotel/kuala-lumpur-my.html\",\"awardsAndAccolades\":{\"advanceGuaranteeProgram\":{\"isEligible\":false}},\"localLanguage\":{\"languageId\":20,\"name\":\"Chinese (Taiwan)\",\"locale\":\"zh-tw\",\"isSuggested\":false,\"shouldShowAmountBeforeCurrency\":false},\"sharingUrl\":null,\"topSellingPoints\":[{\"id\":8,\"text\":\"Best seller\",\"value\":0.0}],\"supplierHotelId\":null,\"renovation\":null,\"isEasyCancel\":false,\"isExcellentCleanliness\":false,\"nhaSummary\":{\"hostType\":null,\"arrivalGuide\":{\"checkinMethod\":null,\"selfCheckin\":false,\"managedCheckin\":false}},\"isInclusivePricePolicy\":false},\"images\":[],\"imageCategories\":[],\"bundlePrice\":null,\"cheapestChildRoom\":null,\"cheapestChildRoomWithFreeBreakfastUid\":null,\"masterRooms\":[],\"soldOutRooms\":[],\"featureGroups\":[{\"id\":41,\"name\":\"Internet access\",\"features\":[]},{\"id\":38,\"name\":\"Services and conveniences\",\"features\":[]},{\"id\":37,\"name\":\"Access\",\"features\":[]}],\"features\":{\"facilities\":[{\"id\":41,\"name\":\"Internet access\",\"features\":[{\"symbol\":\"free-wifi-in-all-rooms\",\"name\":\"Free Wi-Fi in all rooms!\",\"available\":false,\"id\":109,\"images\":[]}]},{\"id\":38,\"name\":\"Services and conveniences\",\"features\":[{\"symbol\":\"wheelchair-accessible\",\"name\":\"Elevator\",\"available\":false,\"id\":5,\"images\":[]},{\"symbol\":\"smoking-area\",\"name\":\"Smoking area\",\"available\":false,\"id\":83,\"images\":[]}]},{\"id\":37,\"name\":\"Access\",\"features\":[{\"symbol\":\"pets-allowed\",\"name\":\"Pets allowed\",\"available\":false,\"id\":24,\"images\":[]}]}],\"favoriteFeatures\":[],\"hotelFacilities\":[],\"featureSummary\":null,\"facilityHighlights\":[]},\"usefulGroups\":[{\"id\":1,\"name\":\"Check-in/Check-out\",\"usefulInfos\":[{\"id\":8,\"name\":\"Check-in from\",\"description\":\"15:00\",\"symbol\":\"express-check-in-check-out\"},{\"id\":9,\"name\":\"Check-out until\",\"description\":\"11:00\",\"symbol\":\"express-check-in-check-out\"},{\"id\":45,\"name\":\"Check-in until\",\"description\":\"00:00\",\"symbol\":\"express-check-in-check-out\"}]},{\"id\":4,\"name\":\"The property\",\"usefulInfos\":[{\"id\":46,\"name\":\"License Id / Local Tax ID/ Entity Name\",\"description\":\"臺北市旅館174-2號/94251211/板橋旅居文旅股份有限公司中山分公司\",\"symbol\":\"tax-id\"}]}],\"hygieneCertificates\":[],\"vaccinationInfo\":null,\"description\":{\"long\":\"Conveniently located between MRT Minquan West Road Station and MRT Shuanglian Station, only 3 minutes’ walk from both stations, Hub Hotel - Zhongshan Branch offers accommodation in Taipei. Free WiFi is accessible in every guestroom.\\n\\nHub Hotel - Zhongshan Branch is 2 metro stops away from Taipei Main Station and Taipei Bus Station. It takes 15 minutes ‘drive from Taipei Songshan Airport to the property, while Taoyuan International Airport can be reached in 60 minutes’ by car. Airport shuttle service is possible based on request.\\n\\nDecorated in simple and elegant style, each carpeted guest room features air conditioning, wardrobe, minibar, refrigerator and flat-screen TV. The private bathroom comes with bath and shower facilities, free toiletries and a hairdryer.\\n\\nLuggage storage service can be founded at the 24-hour front desk.\",\"short\":\"Conveniently located in Taipei, Bailee Hotel is a great base from which to explore this vibrant city\"},\"nearbyEssentialGroups\":[],\"policyGroups\":[{\"type\":2,\"policies\":[{\"title\":\"Children 0-5 year(s)\",\"description\":\"Stay for free if using existing bedding.\",\"isInfant\":false}]},{\"type\":3,\"policies\":[{\"title\":null,\"description\":\"Extra beds are dependent on the room you choose. Please check the individual room capacity for more details.\"}]},{\"type\":4,\"policies\":[{\"title\":null,\"description\":\"When booking more than 5 rooms, different policies and additional supplements may apply.\"}]}],\"reviews\":[],\"thirdPartyReviews\":[],\"reviewPageUrl\":\"\",\"combinedReview\":{\"score\":null},\"positiveMentions\":null,\"facilityMentionSentiments\":null,\"importantNotes\":[\"Guests are required to show a photo identification and credit card upon check-in. Please note that all Special Requests are subject to availability and additional charges may apply.\\nPlease inform  in advance of your expected arrival time. You can use the Special Requests box when booking, or contact the property directly with the contact details provided in your confirmation.\\nGuests under the age of 18 can only check in with a parent or official guardian.\"],\"mapsData\":{\"previewLocation\":null,\"mapsSettings\":null},\"highlights\":{\"locations\":[],\"atfPropertyHighlights\":[]},\"distances\":[],\"engagement\":{\"todayBooking\":\"\",\"peopleLooking\":0,\"lastBooking\":\"1900-01-01T06:43:00+06:43\"},\"interestPoints\":[],\"localInformation\":{\"topPlaces\":[],\"nearbyPlaces\":[],\"walkablePlaces\":null,\"interestingPlaces\":[],\"essentialPlaces\":[]},\"recommendedProperties\":null,\"urlMappingId\":252267188,\"needOccupancySearch\":false,\"availabilityStatus\":0,\"agePolicy\":{\"infantAges\":{\"min\":0,\"max\":0},\"childAges\":{\"min\":0,\"max\":5},\"stayFreeAges\":{\"min\":0,\"max\":5},\"minGuestAge\":0,\"isChildStayFree\":true},\"messaging\":{\"allowedChatTypes\":[]},\"appUrl\":\"http://agoda.onelink.me/1640755593?pid=redirect&c=MobileAppPrice&af_dp=agoda%3A%2F%2Fhome&adults=2&children=0&rooms=1&checkIn=2025-08-27&checkOut=2025-08-29&site_id=-1&tag=&af_siteid=-1&af_sub1=EXP-ID-0&af_sub2=EXP-RUN-ID-0&af_sub3=20c10fc9-850c-40bf-8755-1d625216565a&af_force_dp=true&af_click_lookback=1d\",\"area\":{\"highlights\":[]},\"filter\":{\"title\":\"Filter room options by:\",\"tags\":[]},\"breadcrumbs\":[],\"nonHotelAccommodationInformation\":{\"houseRules\":[]},\"hostInfo\":null,\"numberOfVisitors\":0,\"productType\":1,\"stayOccupancy\":null,\"maxStayOccupancy\":null,\"transportationInformation\":{\"propertyId\":2061174,\"waypoints\":[],\"isPickUpServiceAvailable\":false,\"isPickUpServiceRequiredPriorContact\":\"\"},\"transportationNotes\":[],\"bathInformation\":{\"description\":null,\"others\":[],\"bathTypes\":[],\"bathWaterEfficacy\":[],\"bathWaterTypes\":[],\"indoorBaths\":[],\"outdoorBaths\":[]},\"roomBundles\":null,\"isFavoriteEnabled\":true,\"isFavorite\":false,\"rateCategories\":null,\"rateCategory\":null,\"experiments\":{\"useNewSoldOutMessageLayout\":true,\"useChildWidget\":false,\"injectSoldOutRoom\":false,\"buildLdJsonHotelWhitelabel\":true},\"numberOfSoldOutSimilarProperties\":0,\"isSustainableTravel\":false,\"travelSustainablePractices\":null,\"soldOutRecommendedProperties\":null,\"marketingData\":null,\"benefits\":[],\"searchId\":\"6d722852-9ab5-4298-8a51-0cb62fe7ea41\",\"metaLab\":[],\"childrenStayFreeTypeId\":0,\"alternativeRooms\":null,\"requiredGuestContact\":false,\"morDisclosureType\":0,\"crossSellDetail\":null,\"selectedHourlySlot\":null,\"stayType\":null,\"longStayRoomId\":null,\"searchToken\":null,\"hasPulseProperty\":false,\"cheapestHourlyRoom\":null,\"checkInOutInfo\":{\"checkInFrom\":\"15:00\",\"checkOutUntil\":\"11:00\"},\"externalLoyaltyDisplay\":null,\"hasHourlyRate\":false,\"searchCriteria\":null,\"suggestPriceType\":{\"suggestPrice\":\"NA\",\"applyType\":\"UNKNOWN\"},\"companyTraceabilityInfo\":null,\"companyTraceabilityData\":null,\"growthProgramInfo\":null,\"propertyPriceTrendInfo\":null,\"dsaComplianceInfo\":null,\"status\":null,\"debugInfo\":null}",
      "newHotelNameCn": null,
      "newHotelNameEn": null
    }
  ],
  "page": null,
  "hasData": true,
  "code": 0,
  "bizCode": 0,
  "message": null,
  "costTime": "1.756s",
  "isOk": true
}
```

- 国内酒店数据结构 (来源艺龙)：  
因为原文数据较大复杂，这里只展示部分字段说明，具体参考 json 文件：[酒店数据源接口API-国内酒店数据结构.json](json/%E9%85%92%E5%BA%97%E6%95%B0%E6%8D%AE%E6%BA%90%E6%8E%A5%E5%8F%A3API-%E5%9B%BD%E5%86%85%E9%85%92%E5%BA%97%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84.json)
 
主要字段路径说明：   
  - 中文名路径：Result.HotelName  
  - 英文名路径：Result.HotelNameEn
  - 地址中文路径：Result.Address
  - 地址英文路径：Result.AddressEn
  - 酒店集团中文路径：Result.GroupName  
  - 酒店集团英文路径：Result.GroupNameEn
  - 酒店品牌中文路径：Result.BrandName  
  - 酒店品牌英文路径：Result.BrandNameEn
  - 国家中文路径：Result.CountryName  
  - 国家英文路径：Result.CountryNameEn
  - 城市中文路径：Result.countryCode    默认  CN
  - 城市中文路径：Result.CityName  
  - 城市英文路径：Result.CityNameEn
  - 区域中文路径：Result.DistrictName  
  - 区域英文路径：Result.DistrictNameEn
  - 洲区域中文路径： Result.ContinentName   默认 亚洲
  - 洲区域英文路径： Result.ContinentNameEn   默认 Asia
  - 谷歌经度路径：Result.GoogleLon  
  - 谷歌纬度路径：Result.GoogleLat
```json
{
  "Code": "0",
  "Result": {
    "HotelId": "52001174",
    "HotelName": "广州正佳广场万豪酒店",
    "HotelNameEn": "Guangzhou Marriott Hotel Tianhe",
    "HotelUsedName": "",
    "HotelUsedNameEn": "",
    "HotelNameLocal": "",
    "HotelNameLocalEn": "",
    "HotelStatus": 0,
    "ShortName": "",
    "ShortNameEn": "",
    "Address": "天河路228号",
    "AddressEn": "No.228 Tianhe Road",
    "PostalCode": "",
    "StarRate": 0,
    "Category": 5,
    "Phone": "020-89928888",
    "Fax": "",
    "Email": "",
    "Timezone": "8",
    "EstablishmentDate": "2012-06-01",
    "RenovationDate": "2012-11-01",
    "RoomTotalAmount": 319,
    "GroupId": "13",
    "GroupName": "万豪(Marriott)",
    "GroupNameEn": "Marriott International Hotel Group",
    "BrandId": "13",
    "BrandName": "万豪酒店(Marriott)",
    "BrandNameEn": "Marriott Hotels",
    "IsEconomic": 0,
    "IsApartment": 0,
    "ArrivalTime": "14:00",
    "DepartureTime": "12:00",
    "GoogleLat": 23.132842,
    "GoogleLon": 113.326281,
    "BaiduLat": 23.138582,
    "BaiduLon": 113.332814,
    "CountryId": "3800",
    "CountryName": "中国",
    "CountryNameEn": "China",
    "CityId": "2001",
    "CityName": "广州市",
    "CityNameEn": "Guangzhou",
    "CityId2": "",
    "District": "20010001",
    "DistrictName": "天河区",
    "DistrictNameEn": "",
    "BusinessZone": "767928",
    "BusinessZoneName": "天河体育中心/太古汇",
    "BusinessZoneNameEn": "",
    "BusinessZone2": "",
    "BusinessZone2Name": "",
    "BusinessZone2NameEn": "",
    "CreditCards": "万事达(Master),威士(VISA),运通(AMEX),大来(Diners Club),JCB,国内发行银联卡",
    "CreditCardsEn": "Master,Visa,AE,Diners,JCB,Union-A",
    "HasCoupon": false,
    "IntroEditor": "广州正佳广场万豪酒店位于天河新兴中央商务区，地理位置得天独厚，毗邻珠江新城，是多家世界跨国企业的所在地。酒店与亚洲体验之都——正佳广场相连。正佳广场拥有过千家零售商铺及众多餐饮场所。酒店连接地铁站，并是多条主要线路交汇点，距广州琶洲国际会展中心仅15分钟车程，可到达市区多处观光景点。 广州正佳广场万豪酒店装修豪华典雅，不仅为中外来宾提供精美舒适的各类客房，同时配备健身室、桑拿浴室、室内游泳池等各类娱乐设施，是您商务和休闲旅游的理想之选。",
    "IntroEditorEn": "Located in the heart of the Tianhe Business District, Guangzhou Marriott Hotel Tianhe (Guangzhou Zhengjia Guangchang Wanhao Jiudian) is close to the Tiyu Xilu Metro Station, steps from the Pearl River New City. The convenient location of the hotel places guests a five-minute drive from the Guangzhou East Railway Station and a 15 -minute drive from Guangzhou International Exhibition Center.Various rooms and suites feature luxurious bedding, individual climate controls and high-speed Internet access.The Man Ho restaurant serves traditional Cantonese cuisine.Guests with leisure time on their hands can pay a visit to the spa room, 24-hour health center or indoor pool.",
    "Description":"为贯彻落实《广州市星级酒店全面推进减少酒店行业一次性用品专项行动方案》相关规定，推进生活垃圾源头减量， 2019年9月1日起，广州市旅游住宿业将不再主动提供牙刷、梳子、浴擦、剃须刀、指甲锉、鞋擦这些一次性日用品，酒店餐厅不主动提供一次性餐具。若住客需要可咨询酒店。;\n2021-09-28至2026-12-31\n停车场指引： B1层酒店入口电梯间，B5区电梯可直达酒店5楼大堂；B2层需搭乘停车场中央电梯至B1层步行至B5区进入酒店。;",
  }, 
  "Guid":"46811ac4-efb4-48b7-a734-aae879f60ee9"   
}
```

- 国际酒店数据结构（来源Agoda）：    
因为原文数据较大复杂，这里只展示部分字段说明，具体参考 json 文件：[酒店数据源接口API-国际酒店数据结构.json](json/%E9%85%92%E5%BA%97%E6%95%B0%E6%8D%AE%E6%BA%90%E6%8E%A5%E5%8F%A3API-%E5%9B%BD%E9%99%85%E9%85%92%E5%BA%97%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84.json)
  - 中文名路径：暂无 需要翻译  
  - 英文名路径：summary.propertyName.englishName
  - 地址路径：summary.address.address1 , summary.address.address2 
  - 城市路径： summary.address.cityName
  - 国家路径：summary.address.countryName
  - 国家代码路径：summary.countryCode
  - 洲区域路径： summary.address.regionName
  - 星级路径： summary.starRating.rating
  - 纬度路径： summary.coordinate.lat
  - 经度路径： summary.coordinate.lng
```json
 "propertyId": 280374,
    "summary": {
        "propertyName": {
            "englishName": "Song Thu Hotel",
            "localName": "Song Thu Hotel",
            "displayName": "Song Thu Hotel"
        },
        "address": {
            "address1": "30 Tran Phu Street",
            "address2": "",
            "areaName": "Hải Châu",
            "cityName": "Da Nang",
            "regionName": "Asia",
            "stateName": "Da Nang",
            "stateId": 897,
            "countryName": "Vietnam",
            "postalCode": ""
        },
        "gmtOffset": 7,
        "cityId": 16440,
        "cityName": "Da Nang",
        "countryNameEn": "Vietnam",
        "countryId": 38,
        "countryCode": "vn",
        "starRating": {
            "rating": 2,
            "type": 1,
            "text": "This star rating is provided to Agoda by the property, and is usually determined by an official hotel ratings organisation or a third party."
        },
        "rating": {
            "score": 2,
            "type": 1
        },
        "coordinate": {
            "lat": 16.0741590166048,
            "lng": 108.22346496582
        },
        "accommodationType": {
            "id": 34,
            "englishName": "Hotel",
            "localName": "Hotel"
        },
        "propertyType": 0,
        "hasHostExperience": false,
        "blockedNationalities": [

        ],
        "propertyUrl": "/zh-cn/deface-victory-suites-kuala-lumpur/hotel/kuala-lumpur-my.html",
        "awardsAndAccolades": {
            "advanceGuaranteeProgram": {
                "isEligible": true
            }
        },
        "localLanguage": {
            "languageId": 24,
            "name": "Vietnamese",
            "locale": "vi-vn",
            "isSuggested": false,
            "shouldShowAmountBeforeCurrency": true
        },
        "sharingUrl": null,
        "topSellingPoints": [
            {
                "id": 9,
                "text": null,
                "value": 14
            },
            {
                "id": 8,
                "text": "Best seller",
                "value": 0
            }
        ],
        "supplierHotelId": null,
        "renovation": null,
        "isEasyCancel": true,
        "isExcellentCleanliness": false,
        "nhaSummary": {
            "hostType": null,
            "arrivalGuide": {
                "checkinMethod": null,
                "selfCheckin": false,
                "managedCheckin": false
            }
        },
        "isInclusivePricePolicy": false
    },
    "images": []
}
```