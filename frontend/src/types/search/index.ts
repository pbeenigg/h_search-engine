// 酒店搜索相关类型定义

// 酒店索引文档（字段可能为 null）
export interface HotelIndexDoc {
  id: string;
  tagSource: string; // CN | INTL | HMT
  providerSource: string | null;
  hotelId: number;
  nameCn: string | null;
  nameEn: string | null;
  countryCn: string | null;
  countryEn: string | null;
  countryCode: string | null;
  cityCn: string | null;
  cityEn: string | null;
  regionCn: string | null;
  regionEn: string | null;
  continentCn: string | null;
  continentEn: string | null;
  addressCn: string | null;
  addressEn: string | null;
  lat: number | null;
  lon: number | null;
  location?: { lat: number; lon: number } | null;
  groupCn: string | null;
  groupEn: string | null;
  brandCn: string | null;
  brandEn: string | null;
  nameTokens?: string[] | null;
  addressTokens?: string[] | null;
  nameKeywords?: string[] | null;
  nerPlaces?: string[] | null;
  nerBrands?: string[] | null;
  nameTraditional?: string | null;
  addressTraditional?: string | null;
  brandNames?: string[] | null;
  geoHierarchy?: string[] | null;
  highlightedNameCn?: string | null;
  highlightedNameEn?: string | null;
  highlightedAddressCn?: string | null;
  highlightedAddressEn?: string | null;
  highlightedNameTraditional?: string | null;
}

// 搜索结果（新的响应结构）
export interface HotelSearchResult {
  keyword: string;           // 搜索关键词
  total: number;             // 总数
  hotels: HotelIndexDoc[];   // 酒店列表
  searchLat?: number;        // 搜索定位纬度
  searchLon?: number;        // 搜索定位经度
  searchCountry?: string;    // 搜索定位国家
  searchCity?: string;       // 搜索定位城市
  durationMs?: number;       // 搜索耗时（毫秒）
}

// 搜索请求参数（匹配后端 /search API）
export interface HotelSearchParams {
  q?: string;              // 查询关键词（可选）
  tag?: 'CN' | 'INTL' | 'HMT'; // 业务域：CN 大陆 / INTL 国际 / HMT 港澳台
  country?: string;        // 国家（可选）
  city?: string;           // 城市（可选）
  lat?: number;            // 纬度（可选）
  lon?: number;            // 经度（可选）
  size?: number;           // 返回数量（默认 5）
}

// 热门区域
export interface HotRegion {
  name: string;
  nameEn?: string;
  count?: number;
}

// 热门酒店
export interface HotHotel {
  hotelId: number;
  nameCn: string;
  nameEn: string;
  cityCn: string;
  brandCn?: string;
}

// 热门推荐响应
export interface HotRecommendation {
  regions: HotRegion[];
  hotels: HotHotel[];
}

// 搜索建议
export interface SearchSuggestion {
  type: 'hotel' | 'region' | 'city';
  text: string;
  textEn?: string;
  highlight?: string;
  data?: HotelIndexDoc;
}
