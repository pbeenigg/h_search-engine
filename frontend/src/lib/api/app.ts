// 应用管理API服务
// 对接后端 /app 相关接口

import apiClient from './client';
import type {
  ApiResponse,
  PageResponse,
  App,
  UpdateAppRequest,
  RefreshSecretResponse,
} from '@/types/auth';

// API 端点
const APP_ENDPOINTS = {
  BASE: '/app',
  BY_ID: (appId: string) => `/app/${appId}`,
  REFRESH_SECRET: (appId: string) => `/app/${appId}/refresh-secret`,
};

/**
 * 获取应用列表
 * GET /app?page=0&size=20
 */
export async function getApps(
  page: number = 0,
  size: number = 20
): Promise<PageResponse<App>> {
  const response = await apiClient.get<ApiResponse<PageResponse<App>>>(
    APP_ENDPOINTS.BASE,
    { params: { page, size } }
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '获取应用列表失败');
  }
  
  return response.data.data;
}

/**
 * 获取单个应用
 * GET /app/{appId}
 */
export async function getApp(appId: string): Promise<App> {
  const response = await apiClient.get<ApiResponse<App>>(
    APP_ENDPOINTS.BY_ID(appId)
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '获取应用信息失败');
  }
  
  return response.data.data;
}

/**
 * 更新应用配置
 * PUT /app/{appId}
 */
export async function updateApp(
  appId: string,
  data: UpdateAppRequest
): Promise<App> {
  const response = await apiClient.put<ApiResponse<App>>(
    APP_ENDPOINTS.BY_ID(appId),
    data
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '更新应用失败');
  }
  
  return response.data.data;
}

/**
 * 刷新应用密钥
 * POST /app/{appId}/refresh-secret
 */
export async function refreshSecret(appId: string): Promise<RefreshSecretResponse> {
  const response = await apiClient.post<ApiResponse<RefreshSecretResponse>>(
    APP_ENDPOINTS.REFRESH_SECRET(appId)
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '刷新密钥失败');
  }
  
  return response.data.data;
}

// 导出应用API服务
export const appApi = {
  getApps,
  getApp,
  updateApp,
  refreshSecret,
};

export default appApi;
