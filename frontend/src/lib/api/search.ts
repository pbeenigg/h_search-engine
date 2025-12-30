// 酒店搜索 API 服务

import apiClient from './client';
import type { ApiResponse } from '@/types/auth';
import type {
  HotelSearchResult,
  HotelSearchParams,
  HotRecommendation,
  SearchSuggestion,
} from '@/types/search';

/**
 * 智能酒店搜索接口
 * 支持：
 * 1. 关键词搜索
 * 2. IP地址定位
 * 3. 用户指定地理位置（国家、城市、经纬度）
 * 4. 多数据源融合（酒店索引 + POI地标）
 * 5. 智能排序（关键词匹配60% + 距离40%）
 * 
 * GET /search
 */
export async function searchHotels(
  params: HotelSearchParams
): Promise<HotelSearchResult> {
  const response = await apiClient.get<ApiResponse<HotelSearchResult>>(
    '/search/smart',
    { params }
  );

  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '搜索失败');
  }

  return response.data.data;
}

/**
 * 获取热门推荐（区域和酒店）- 使用模拟数据
 */
export async function getHotRecommendations(): Promise<HotRecommendation> {
  // 模拟异步请求
  return Promise.resolve(getMockHotRecommendations());
}

/**
 * 获取搜索建议 - 通过主搜索接口实现
 */
export async function getSearchSuggestions(
  keyword: string,
  limit: number = 10,
  tag?: 'CN' | 'INTL' | 'HMT'
): Promise<SearchSuggestion[]> {
  if (!keyword || keyword.trim().length === 0) {
    return [];
  }

  try {
    const params: HotelSearchParams = {
      q: keyword,
      size: limit,
    };
    if (tag) {
      params.tag = tag;
    }
    
    const result = await searchHotels(params);

    return result.hotels.map((hotel) => ({
      type: 'hotel' as const,
      text: hotel.nameCn || hotel.nameEn || 'Unknown Hotel',
      // 规范为 undefined，而不是可能的 null
      textEn: hotel.nameEn || undefined,
      highlight: hotel.highlightedNameCn || hotel.highlightedNameEn || 'Unknown Hotel',
      data: hotel,
    }));
  } catch {
    return [];
  }
}

/**
 * 模拟热门推荐数据
 */
function getMockHotRecommendations(): HotRecommendation {
  return {
    regions: [
      { name: '上海', nameEn: 'Shanghai', count: 12580 },
      { name: '北京', nameEn: 'Beijing', count: 11200 },
      { name: '杭州', nameEn: 'Hangzhou', count: 8900 },
      { name: '广州', nameEn: 'Guangzhou', count: 7800 },
      { name: '深圳', nameEn: 'Shenzhen', count: 6500 },
      { name: '成都', nameEn: 'Chengdu', count: 5800 },
      { name: '三亚', nameEn: 'Sanya', count: 4500 },
      { name: '厦门', nameEn: 'Xiamen', count: 3200 },
    ],
    hotels: [
      { hotelId: 1, nameCn: '上海外滩W酒店', nameEn: 'W Shanghai - The Bund', cityCn: '上海', brandCn: 'W酒店' },
      { hotelId: 2, nameCn: '北京国贸大酒店', nameEn: 'China World Hotel Beijing', cityCn: '北京', brandCn: '香格里拉' },
      { hotelId: 3, nameCn: '杭州西湖洲际酒店', nameEn: 'InterContinental Hangzhou', cityCn: '杭州', brandCn: '洲际' },
      { hotelId: 4, nameCn: '广州四季酒店', nameEn: 'Four Seasons Guangzhou', cityCn: '广州', brandCn: '四季' },
      { hotelId: 5, nameCn: '三亚亚龙湾丽思卡尔顿', nameEn: 'The Ritz-Carlton Sanya', cityCn: '三亚', brandCn: '丽思卡尔顿' },
      { hotelId: 6, nameCn: '成都华尔道夫酒店', nameEn: 'Waldorf Astoria Chengdu', cityCn: '成都', brandCn: '华尔道夫' },
    ],
  };
}

// 导出搜索API服务
export const searchApi = {
  searchHotels,
  getHotRecommendations,
  getSearchSuggestions,
};

export default searchApi;
