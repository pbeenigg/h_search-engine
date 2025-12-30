// 酒店搜索演示页面
'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { 
  Search, 
  MapPin, 
  Building2, 
  ArrowLeft, 
  Loader2, 
  Star,
  Navigation,
  X
} from 'lucide-react';
import { searchApi } from '@/lib/api';
import { ROUTES } from '@/lib/constants';
import type { 
  HotelIndexDoc, 
  HotelSearchParams,
  HotRecommendation, 
  SearchSuggestion 
} from '@/types/search';

// 防抖 Hook
function useDebounce<T>(value: T, delay: number): T {
  const [debouncedValue, setDebouncedValue] = useState<T>(value);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedValue(value);
    }, delay);

    return () => {
      clearTimeout(timer);
    };
  }, [value, delay]);

  return debouncedValue;
}

export default function HotelSearchDemoPage() {
  const router = useRouter();
  const searchInputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // 搜索状态
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<HotelIndexDoc[]>([]);
  const [totalResults, setTotalResults] = useState(0);
  
  // 业务域筛选
  const [tagSource, setTagSource] = useState<'ALL' | 'CN' | 'INTL' | 'HMT'>('ALL');

  // 下拉框状态
  const [showDropdown, setShowDropdown] = useState(false);
  const [hotData, setHotData] = useState<HotRecommendation | null>(null);
  const [suggestions, setSuggestions] = useState<SearchSuggestion[]>([]);
  const [isLoadingHot, setIsLoadingHot] = useState(false);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);

  // 防抖后的搜索关键词 (200ms)
  const debouncedQuery = useDebounce(searchQuery, 200);

  // 加载热门推荐
  const loadHotRecommendations = useCallback(async () => {
    if (hotData) return; // 已加载过则不重复加载
    
    setIsLoadingHot(true);
    try {
      const data = await searchApi.getHotRecommendations();
      setHotData(data);
    } catch (error) {
      console.error('加载热门推荐失败:', error);
    } finally {
      setIsLoadingHot(false);
    }
  }, [hotData]);

  // 搜索建议
  const loadSuggestions = useCallback(async (keyword: string) => {
    if (!keyword || keyword.trim().length === 0) {
      setSuggestions([]);
      return;
    }

    setIsLoadingSuggestions(true);
    try {
      // 传递 tag 参数（非 ALL 时）
      const tagParam = tagSource === 'CN' || tagSource === 'INTL' || tagSource === 'HMT' 
        ? tagSource 
        : undefined;
      const data = await searchApi.getSearchSuggestions(keyword, 8, tagParam);
      setSuggestions(data);
    } catch (error) {
      console.error('加载搜索建议失败:', error);
      setSuggestions([]);
    } finally {
      setIsLoadingSuggestions(false);
    }
  }, [tagSource]);

  // 监听防抖后的关键词变化，实时搜索建议
  useEffect(() => {
    if (debouncedQuery && showDropdown) {
      loadSuggestions(debouncedQuery);
    }
  }, [debouncedQuery, showDropdown, loadSuggestions]);

  // 当业务域变化时，如果有搜索结果，自动重新搜索
  useEffect(() => {
    if (searchQuery.trim() && searchResults.length > 0) {
      handleSearch();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tagSource]);

  // 执行搜索
  const handleSearch = async (query?: string) => {
    const searchKeyword = query ?? searchQuery;
    if (!searchKeyword || searchKeyword.trim().length === 0) {
      return;
    }

    setShowDropdown(false);
    setIsSearching(true);
    
    try {
      // 构建搜索参数 - 使用 HotelSearchParams 类型
      const searchParams: HotelSearchParams = {
        q: searchKeyword,
        size: 20,
      };
      
      // 只有非 ALL 时才添加 tag 参数
      if (tagSource === 'CN' || tagSource === 'INTL' || tagSource === 'HMT') {
        searchParams.tag = tagSource;
      }
      
      console.log('[搜索参数]', JSON.stringify(searchParams));
      
      const result = await searchApi.searchHotels(searchParams);
      setSearchResults(result.hotels);
      setTotalResults(result.total);
    } catch (error) {
      console.error('搜索失败:', error);
      setSearchResults([]);
      setTotalResults(0);
    } finally {
      setIsSearching(false);
    }
  };

  // 搜索框获得焦点 - 加载热门推荐
  const handleFocus = () => {
    setShowDropdown(true);
    loadHotRecommendations();
  };

  // 点击外部关闭下拉框
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node) &&
        searchInputRef.current &&
        !searchInputRef.current.contains(event.target as Node)
      ) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // 处理键盘事件
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSearch();
    } else if (e.key === 'Escape') {
      setShowDropdown(false);
    }
  };

  // 选择热门区域
  const handleSelectRegion = (region: string) => {
    setSearchQuery(region);
    handleSearch(region);
  };

  // 选择热门酒店
  const handleSelectHotel = (hotel: { nameCn: string }) => {
    setSearchQuery(hotel.nameCn);
    handleSearch(hotel.nameCn);
  };

  // 选择搜索建议
  const handleSelectSuggestion = (suggestion: SearchSuggestion) => {
    setSearchQuery(suggestion.text);
    handleSearch(suggestion.text);
  };

  // 清除搜索
  const handleClear = () => {
    setSearchQuery('');
    setSearchResults([]);
    setTotalResults(0);
    setSuggestions([]);
    searchInputRef.current?.focus();
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900">
      {/* 顶部返回按钮 */}
      <div className="fixed top-4 left-4 z-50">
        <Button
          variant="outline"
          size="sm"
          onClick={() => router.push(ROUTES.DASHBOARD)}
          className="bg-white/80 dark:bg-gray-800/80 backdrop-blur-sm shadow-lg hover:shadow-xl transition-all"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          返回管理后台
        </Button>
      </div>

      {/* 主搜索区域 */}
      <div className="flex flex-col items-center justify-start pt-20 px-4">
        {/* Logo 和标题 */}
        <div className="text-center mb-8">
          <div className="w-20 h-20 mx-auto mb-4 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl flex items-center justify-center shadow-lg">
            <Building2 className="w-10 h-10 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-gray-800 dark:text-white mb-2">
            HeyTrip 酒店搜索
          </h1>
          <p className="text-gray-500 dark:text-gray-400">
            探索全球 360 万+ 酒店，找到您的完美住所
          </p>
        </div>

        {/* 业务域筛选 */}
        <div className="flex items-center justify-center gap-2 mb-6">
          {[
            { value: 'ALL', label: '全部', icon: '🌍' },
            { value: 'CN', label: '大陆', icon: '🇨🇳' },
            { value: 'HMT', label: '港澳台', icon: '🏝️' },
            { value: 'INTL', label: '国际', icon: '✈️' },
          ].map((item) => (
            <button
              key={item.value}
              onClick={() => setTagSource(item.value as 'ALL' | 'CN' | 'INTL' | 'HMT')}
              className={`
                flex items-center gap-2 px-5 py-2.5 rounded-full text-sm font-medium transition-all duration-200
                ${tagSource === item.value
                  ? 'bg-gradient-to-r from-blue-500 to-purple-600 text-white shadow-lg scale-105'
                  : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 border border-gray-200 dark:border-gray-600'
                }
              `}
            >
              <span>{item.icon}</span>
              <span>{item.label}</span>
            </button>
          ))}
        </div>

        {/* 搜索框 */}
        <div className="w-full max-w-3xl relative">
          <div className="relative">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400">
              <Search className="w-5 h-5" />
            </div>
            <Input
              ref={searchInputRef}
              type="text"
              placeholder="搜索酒店名称、城市或区域..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onFocus={handleFocus}
              onKeyDown={handleKeyDown}
              className="w-full h-14 pl-12 pr-24 text-lg rounded-2xl border-2 border-gray-200 dark:border-gray-700 focus:border-blue-500 dark:focus:border-blue-400 shadow-lg focus:shadow-xl transition-all bg-white dark:bg-gray-800"
            />
            {searchQuery && (
              <button
                onClick={handleClear}
                className="absolute right-20 top-1/2 -translate-y-1/2 p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              >
                <X className="w-5 h-5" />
              </button>
            )}
            <Button
              onClick={() => handleSearch()}
              disabled={isSearching || !searchQuery.trim()}
              className="absolute right-2 top-1/2 -translate-y-1/2 h-10 px-6 rounded-xl bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700"
            >
              {isSearching ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                '搜索'
              )}
            </Button>
          </div>

          {/* 下拉框 */}
          {showDropdown && (
            <div
              ref={dropdownRef}
              className="absolute top-full left-0 right-0 mt-2 bg-white dark:bg-gray-800 rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-700 overflow-hidden z-50"
            >
              {/* 有搜索关键词时显示搜索建议 */}
              {searchQuery.trim() && (
                <div className="p-4">
                  <h3 className="text-sm font-semibold text-gray-500 dark:text-gray-400 mb-3">
                    搜索建议
                  </h3>
                  {isLoadingSuggestions ? (
                    <div className="flex items-center justify-center py-8">
                      <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
                    </div>
                  ) : suggestions.length > 0 ? (
                    <div className="space-y-1">
                      {suggestions.map((suggestion, index) => (
                        <button
                          key={index}
                          onClick={() => handleSelectSuggestion(suggestion)}
                          className="w-full flex items-center gap-3 p-3 rounded-xl hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-left"
                        >
                          <div className="w-10 h-10 bg-blue-100 dark:bg-blue-900/30 rounded-lg flex items-center justify-center flex-shrink-0">
                            <Building2 className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                          </div>
                          <div className="flex-1 min-w-0">
                            <p className="font-medium text-gray-800 dark:text-white">
                              {/* 优先使用高亮名称 */}
                              {suggestion.data?.highlightedNameCn ? (
                                <span className="[&_em]:text-blue-600 dark:[&_em]:text-blue-400 [&_em]:not-italic" dangerouslySetInnerHTML={{ __html: suggestion.data.highlightedNameCn }} />
                              ) : suggestion.data?.highlightedNameTraditional ? (
                                <span className="[&_em]:text-blue-600 dark:[&_em]:text-blue-400 [&_em]:not-italic" dangerouslySetInnerHTML={{ __html: suggestion.data.highlightedNameTraditional }} />
                              ) : (
                                <>{suggestion.text}   {suggestion.textEn && ` , ${suggestion.textEn}`}</>
                              )}
                            </p>
                            {suggestion.data && (
                              <p className="text-sm text-gray-500 dark:text-gray-400 truncate">
                                {suggestion.data.cityCn || suggestion.data.cityEn || ''} 
                                {(suggestion.data.regionCn || suggestion.data.regionEn) && ` · ${suggestion.data.regionCn || suggestion.data.regionEn}`}
                              </p>
                            )}
                          </div>
                        </button>
                      ))}
                    </div>
                  ) : (
                    <p className="text-center py-4 text-gray-500">
                      暂无搜索建议
                    </p>
                  )}
                </div>
              )}

              {/* 无搜索关键词时显示热门推荐 */}
              {!searchQuery.trim() && (
                <div className="grid grid-cols-2 divide-x divide-gray-200 dark:divide-gray-700">
                  {/* 左侧：热门区域 */}
                  <div className="p-4">
                    <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-500 dark:text-gray-400 mb-3">
                      <MapPin className="w-4 h-4" />
                      热门目的地
                    </h3>
                    {isLoadingHot ? (
                      <div className="flex items-center justify-center py-8">
                        <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
                      </div>
                    ) : (
                      <div className="space-y-1">
                        {hotData?.regions.map((region, index) => (
                          <button
                            key={index}
                            onClick={() => handleSelectRegion(region.name)}
                            className="w-full flex items-center justify-between p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                          >
                            <span className="font-medium text-gray-800 dark:text-white">
                              {region.name}
                            </span>
                            {region.count && (
                              <span className="text-xs text-gray-400">
                                {region.count.toLocaleString()} 家
                              </span>
                            )}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* 右侧：爆火酒店 */}
                  <div className="p-4">
                    <h3 className="flex items-center gap-2 text-sm font-semibold text-gray-500 dark:text-gray-400 mb-3">
                      <Star className="w-4 h-4" />
                      热门酒店
                    </h3>
                    {isLoadingHot ? (
                      <div className="flex items-center justify-center py-8">
                        <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
                      </div>
                    ) : (
                      <div className="space-y-1">
                        {hotData?.hotels.map((hotel, index) => (
                          <button
                            key={index}
                            onClick={() => handleSelectHotel(hotel)}
                            className="w-full flex items-center gap-2 p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors text-left"
                          >
                            <div className="w-8 h-8 bg-gradient-to-br from-orange-400 to-pink-500 rounded-lg flex items-center justify-center flex-shrink-0">
                              <Building2 className="w-4 h-4 text-white" />
                            </div>
                            <div className="flex-1 min-w-0">
                              <p className="text-sm font-medium text-gray-800 dark:text-white truncate">
                                {hotel.nameCn}
                              </p>
                              <p className="text-xs text-gray-400 truncate">
                                {hotel.cityCn} {hotel.brandCn && `· ${hotel.brandCn}`}
                              </p>
                            </div>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>

        {/* 搜索结果 */}
        {(searchResults.length > 0 || isSearching) && (
          <div className="w-full max-w-5xl mt-8">
            {/* 结果统计 */}
            <div className="flex items-center justify-between mb-4 px-2">
              <p className="text-gray-600 dark:text-gray-400">
                {isSearching ? (
                  '搜索中...'
                ) : (
                  <>
                    找到 <span className="font-semibold text-blue-600">{totalResults.toLocaleString()}</span> 家酒店
                  </>
                )}
              </p>
            </div>

            {/* 酒店列表 */}
            {isSearching ? (
              <div className="flex items-center justify-center py-20">
                <Loader2 className="w-10 h-10 animate-spin text-blue-500" />
              </div>
            ) : (
              <div className="grid gap-4">
                {searchResults.map((hotel) => (
                  <HotelCard key={hotel.id} hotel={hotel} />
                ))}
              </div>
            )}
          </div>
        )}

        {/* 无结果提示 */}
        {!isSearching && searchQuery && searchResults.length === 0 && totalResults === 0 && (
          <div className="text-center py-20">
            <div className="w-24 h-24 mx-auto mb-4 bg-gray-100 dark:bg-gray-800 rounded-full flex items-center justify-center">
              <Search className="w-10 h-10 text-gray-400" />
            </div>
            <p className="text-gray-500 dark:text-gray-400">
              未找到相关酒店，请尝试其他关键词
            </p>
          </div>
        )}
      </div>
    </div>
  );
}

// 酒店卡片组件
function HotelCard({ hotel }: { hotel: HotelIndexDoc }) {
  return (
    <Card className="overflow-hidden hover:shadow-xl transition-all duration-300 border-0 bg-white dark:bg-gray-800">
      <CardContent className="p-0">
        <div className="flex">
          {/* 酒店图片占位 */}
          <div className="w-48 h-36 bg-gradient-to-br from-blue-100 to-purple-100 dark:from-blue-900/30 dark:to-purple-900/30 flex items-center justify-center flex-shrink-0">
            <Building2 className="w-12 h-12 text-blue-400" />
          </div>

          {/* 酒店信息 */}
          <div className="flex-1 p-4">
            <div className="flex items-start justify-between">
              <div className="flex-1 min-w-0">
                {/* 酒店名称：优先使用高亮版本 */}
                <h3 className="text-lg font-semibold text-gray-800 dark:text-white">
                  {hotel.highlightedNameCn ? (
                    <span className="[&_em]:text-blue-600 dark:[&_em]:text-blue-400 [&_em]:not-italic" dangerouslySetInnerHTML={{ __html: hotel.highlightedNameCn }} />
                  ) : hotel.highlightedNameTraditional ? (
                    <span className="[&_em]:text-blue-600 dark:[&_em]:text-blue-400 [&_em]:not-italic" dangerouslySetInnerHTML={{ __html: hotel.highlightedNameTraditional }} />
                  ) : (
                    hotel.nameCn || hotel.nameEn || `酒店 #${hotel.hotelId}`
                  )}
                </h3>
                {/* 英文名称或高亮英文版本 */}
                {(hotel.highlightedNameEn || hotel.nameEn) && (
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    {hotel.highlightedNameEn ? (
                      <span className="[&_em]:text-blue-600 dark:[&_em]:text-blue-400 [&_em]:not-italic" dangerouslySetInnerHTML={{ __html: hotel.highlightedNameEn }} />
                    ) : (
                      hotel.nameEn
                    )}
                  </p>
                )}
              </div>
              {(hotel.brandCn || hotel.brandEn) && (
                <span className="ml-2 px-2 py-1 bg-blue-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 text-xs font-medium rounded-full flex-shrink-0">
                  {hotel.brandCn || hotel.brandEn}
                </span>
              )}
            </div>

            {/* 位置信息 */}
            <div className="flex items-center gap-4 mt-2 text-sm text-gray-500 dark:text-gray-400">
              <span className="flex items-center gap-1">
                <MapPin className="w-4 h-4" />
                {hotel.cityCn || hotel.cityEn || '未知城市'}
                {(hotel.regionCn || hotel.regionEn) && ` · ${hotel.regionCn || hotel.regionEn}`}
              </span>
              {hotel.lat && hotel.lon && (
                <span className="flex items-center gap-1">
                  <Navigation className="w-4 h-4" />
                  {hotel.lat.toFixed(4)}, {hotel.lon.toFixed(4)}
                </span>
              )}
            </div>

            {/* 地址：优先使用高亮版本 */}
            {(hotel.highlightedAddressCn || hotel.highlightedAddressEn || hotel.addressCn || hotel.addressEn) && (
              <p className="mt-2 text-sm text-gray-500 dark:text-gray-400">
                {hotel.highlightedAddressCn ? (
                  <span className="[&_em]:text-blue-600 dark:[&_em]:text-blue-400 [&_em]:not-italic" dangerouslySetInnerHTML={{ __html: hotel.highlightedAddressCn }} />
                ) : hotel.highlightedAddressEn ? (
                  <span className="[&_em]:text-blue-600 dark:[&_em]:text-blue-400 [&_em]:not-italic" dangerouslySetInnerHTML={{ __html: hotel.highlightedAddressEn }} />
                ) : (
                  hotel.addressCn || hotel.addressEn
                )}
              </p>
            )}
            {/* 高亮繁体地址（如果有） */}
            {hotel.addressTraditional && !hotel.highlightedAddressCn && !hotel.highlightedAddressEn && (
              <p className="mt-1 text-sm text-gray-400 dark:text-gray-500">
                {hotel.addressTraditional}
              </p>
            )}

            {/* 标签 */}
            <div className="flex items-center gap-2 mt-3">
              {(hotel.hotelId ?? hotel.id) && (
                <span className="px-2 py-0.5 bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 text-xs font-medium rounded">
                  酒店ID: {hotel.hotelId ?? hotel.id}
                </span>
              )}
              <span className="px-2 py-0.5 bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 text-xs rounded">
                {hotel.tagSource === 'CN' ? '国内' : hotel.tagSource === 'INTL' ? '国际' : hotel.tagSource === 'HMT' ? '港澳台' : hotel.tagSource}
              </span>
              {hotel.providerSource && (
                <span className="px-2 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 text-xs rounded">
                  {hotel.providerSource}
                </span>
              )}
              {(hotel.countryCn || hotel.countryEn) && (
                <span className="px-2 py-0.5 bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-400 text-xs rounded">
                  {hotel.countryCn || hotel.countryEn}
                </span>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
