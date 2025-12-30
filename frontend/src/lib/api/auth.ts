// 认证API服务
// 对接后端 /auth 相关接口

import apiClient from './client';
import type {
  ApiResponse,
  LoginRequest,
  LoginResponseData,
  CurrentUserInfo,
} from '@/types/auth';

// API 端点
const AUTH_ENDPOINTS = {
  LOGIN: '/auth/login',
  LOGOUT: '/auth/logout',
  CURRENT: '/auth/current',
};

/**
 * 用户登录
 * POST /auth/login
 */
export async function login(data: LoginRequest): Promise<LoginResponseData> {
  const response = await apiClient.post<ApiResponse<LoginResponseData>>(
    AUTH_ENDPOINTS.LOGIN,
    data
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '登录失败');
  }
  
  return response.data.data;
}

/**
 * 用户登出
 * POST /auth/logout
 */
export async function logout(): Promise<void> {
  const response = await apiClient.post<ApiResponse<null>>(AUTH_ENDPOINTS.LOGOUT);
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '登出失败');
  }
}

/**
 * 获取当前用户信息
 * GET /auth/current
 */
export async function getCurrentUser(): Promise<CurrentUserInfo> {
  const response = await apiClient.get<ApiResponse<CurrentUserInfo>>(
    AUTH_ENDPOINTS.CURRENT
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '获取用户信息失败');
  }
  
  return response.data.data;
}

// 导出认证API服务
export const authApi = {
  login,
  logout,
  getCurrentUser,
};

export default authApi;
