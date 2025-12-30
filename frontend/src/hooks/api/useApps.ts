// 应用管理 Hooks
'use client';

import { useState, useCallback, useEffect } from 'react';
import { appApi } from '@/lib/api';
import type {
  App,
  UpdateAppRequest,
  FrontendApp,
} from '@/types/auth';
import { DEBUG_MODE } from '@/lib/constants';

// 应用列表状态
interface UseAppsState {
  apps: FrontendApp[];
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// 将后端应用转换为前端格式
const mapAppToFrontendLocal = (app: App): FrontendApp => ({
  id: app.appId,
  name: app.appId.split('_')[0] + '的应用',
  appId: app.appId,
  secretKey: app.secretKey,
  status: app.timeout === 0 ? 'inactive' : 'active',
  rateLimit: app.rateLimit,
  timeout: app.timeout,
  creator: app.appId.split('_')[0],
  createdAt: app.createAt || new Date().toISOString(),
});

// 应用列表 Hook
export function useApps(initialPage: number = 0, initialSize: number = 20) {
  const [state, setState] = useState<UseAppsState>({
    apps: [],
    loading: false,
    error: null,
    pagination: {
      page: initialPage,
      size: initialSize,
      totalElements: 0,
      totalPages: 0,
    },
  });

  // 获取应用列表
  const fetchApps = useCallback(async (page?: number, size?: number) => {
    const targetPage = page ?? state.pagination.page;
    const targetSize = size ?? state.pagination.size;

    setState(prev => ({ ...prev, loading: true, error: null }));

    try {
      const response = await appApi.getApps(targetPage, targetSize);
      
      setState(prev => ({
        ...prev,
        apps: response.content.map(mapAppToFrontendLocal),
        loading: false,
        pagination: {
          page: response.page.number,
          size: response.page.size,
          totalElements: response.page.totalElements,
          totalPages: response.page.totalPages,
        },
      }));

      if (DEBUG_MODE) {
        console.log('Apps fetched:', response);
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : '获取应用列表失败';
      setState(prev => ({ ...prev, loading: false, error: message }));
      console.error('Failed to fetch apps:', error);
    }
  }, [state.pagination.page, state.pagination.size]);

  // 更新应用
  const updateApp = useCallback(async (
    appId: string,
    data: UpdateAppRequest
  ): Promise<boolean> => {
    try {
      await appApi.updateApp(appId, data);
      // 刷新列表
      await fetchApps();
      return true;
    } catch (error) {
      console.error('Failed to update app:', error);
      throw error;
    }
  }, [fetchApps]);

  // 刷新密钥
  const refreshSecret = useCallback(async (appId: string): Promise<string> => {
    try {
      const result = await appApi.refreshSecret(appId);
      // 刷新列表
      await fetchApps();
      return result.secretKey;
    } catch (error) {
      console.error('Failed to refresh secret:', error);
      throw error;
    }
  }, [fetchApps]);

  // 切换页码
  const setPage = useCallback((page: number) => {
    fetchApps(page, state.pagination.size);
  }, [fetchApps, state.pagination.size]);

  // 切换每页数量
  const setPageSize = useCallback((size: number) => {
    fetchApps(0, size);
  }, [fetchApps]);

  // 初始加载
  useEffect(() => {
    const loadApps = async () => {
      await fetchApps();
    };
    loadApps();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return {
    ...state,
    fetchApps,
    updateApp,
    refreshSecret,
    setPage,
    setPageSize,
    refresh: () => fetchApps(),
  };
}

// 单个应用 Hook
export function useApp(appId: string | null) {
  const [app, setApp] = useState<App | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchApp = useCallback(async () => {
    if (!appId) return;

    setLoading(true);
    setError(null);

    try {
      const data = await appApi.getApp(appId);
      setApp(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : '获取应用信息失败';
      setError(message);
      console.error('Failed to fetch app:', err);
    } finally {
      setLoading(false);
    }
  }, [appId]);

  useEffect(() => {
    if (appId) {
      fetchApp();
    }
  }, [appId, fetchApp]);

  return {
    app,
    loading,
    error,
    refresh: fetchApp,
  };
}

export default useApps;
