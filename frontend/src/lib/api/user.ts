// 用户管理API服务
// 对接后端 /user 相关接口

import apiClient from './client';
import type {
  ApiResponse,
  PageResponse,
  User,
  CreateUserRequest,
  UpdateUserRequest,
  ChangePasswordRequest,
} from '@/types/auth';

// API 端点
const USER_ENDPOINTS = {
  BASE: '/user',
  BY_ID: (id: number) => `/user/${id}`,
  CHANGE_PASSWORD: (id: number) => `/user/${id}/change-password`,
};

/**
 * 获取用户列表
 * GET /user?page=0&size=20
 */
export async function getUsers(
  page: number = 0,
  size: number = 20
): Promise<PageResponse<User>> {
  const response = await apiClient.get<ApiResponse<PageResponse<User>>>(
    USER_ENDPOINTS.BASE,
    { params: { page, size } }
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '获取用户列表失败');
  }
  
  return response.data.data;
}

/**
 * 获取单个用户
 * GET /user/{userId}
 */
export async function getUser(userId: number): Promise<User> {
  const response = await apiClient.get<ApiResponse<User>>(
    USER_ENDPOINTS.BY_ID(userId)
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '获取用户信息失败');
  }
  
  return response.data.data;
}

/**
 * 创建用户
 * POST /user
 */
export async function createUser(data: CreateUserRequest): Promise<User> {
  const response = await apiClient.post<ApiResponse<User>>(
    USER_ENDPOINTS.BASE,
    data
  );
  
  if (response.data.code !== 200 && response.data.code !== 201) {
    throw new Error(response.data.msg || '创建用户失败');
  }
  
  return response.data.data;
}

/**
 * 更新用户
 * PUT /user/{userId}
 */
export async function updateUser(
  userId: number,
  data: UpdateUserRequest
): Promise<User> {
  const response = await apiClient.put<ApiResponse<User>>(
    USER_ENDPOINTS.BY_ID(userId),
    data
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '更新用户失败');
  }
  
  return response.data.data;
}

/**
 * 删除用户
 * DELETE /user/{userId}
 */
export async function deleteUser(userId: number): Promise<void> {
  const response = await apiClient.delete<ApiResponse<null>>(
    USER_ENDPOINTS.BY_ID(userId)
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '删除用户失败');
  }
}

/**
 * 修改密码
 * POST /user/{userId}/change-password
 */
export async function changePassword(
  userId: number,
  data: ChangePasswordRequest
): Promise<void> {
  const response = await apiClient.post<ApiResponse<null>>(
    USER_ENDPOINTS.CHANGE_PASSWORD(userId),
    data
  );
  
  if (response.data.code !== 200) {
    throw new Error(response.data.msg || '修改密码失败');
  }
}

// 导出用户API服务
export const userApi = {
  getUsers,
  getUser,
  createUser,
  updateUser,
  deleteUser,
  changePassword,
};

export default userApi;
