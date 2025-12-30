// 用户管理 Hooks
'use client';

import { useState, useCallback, useEffect } from 'react';
import { userApi } from '@/lib/api';
import type {
  User,
  CreateUserRequest,
  UpdateUserRequest,
  ChangePasswordRequest,
  FrontendUser,
} from '@/types/auth';
import { DEBUG_MODE } from '@/lib/constants';

// 用户列表状态
interface UseUsersState {
  users: FrontendUser[];
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
  };
}

// 用户列表 Hook
export function useUsers(initialPage: number = 0, initialSize: number = 20) {
  const [state, setState] = useState<UseUsersState>({
    users: [],
    loading: false,
    error: null,
    pagination: {
      page: initialPage,
      size: initialSize,
      totalElements: 0,
      totalPages: 0,
    },
  });

  // 将后端用户转换为前端格式
  const mapUserToFrontendLocal = (user: User): FrontendUser => ({
    id: user.userId.toString(),
    username: user.userName,
    userNick: user.userNick,
    sex: user.sex,
    role: 'user', // 后端暂未返回角色
    createdAt: user.createAt || new Date().toISOString(),
    email: `${user.userName}@heytrip.com`, // 生成默认邮箱
  });

  // 获取用户列表
  const fetchUsers = useCallback(async (page?: number, size?: number) => {
    const targetPage = page ?? state.pagination.page;
    const targetSize = size ?? state.pagination.size;

    setState(prev => ({ ...prev, loading: true, error: null }));

    try {
      const response = await userApi.getUsers(targetPage, targetSize);
      
      setState(prev => ({
        ...prev,
        users: response.content.map(mapUserToFrontendLocal),
        loading: false,
        pagination: {
          page: response.page.number,
          size: response.page.size,
          totalElements: response.page.totalElements,
          totalPages: response.page.totalPages,
        },
      }));

      if (DEBUG_MODE) {
        console.log('Users fetched:', response);
      }
    } catch (error) {
      const message = error instanceof Error ? error.message : '获取用户列表失败';
      setState(prev => ({ ...prev, loading: false, error: message }));
      console.error('Failed to fetch users:', error);
    }
  }, [state.pagination.page, state.pagination.size]);

  // 创建用户
  const createUser = useCallback(async (data: CreateUserRequest): Promise<boolean> => {
    try {
      await userApi.createUser(data);
      // 刷新列表
      await fetchUsers();
      return true;
    } catch (error) {
      console.error('Failed to create user:', error);
      throw error;
    }
  }, [fetchUsers]);

  // 更新用户
  const updateUser = useCallback(async (
    userId: number,
    data: UpdateUserRequest
  ): Promise<boolean> => {
    try {
      await userApi.updateUser(userId, data);
      // 刷新列表
      await fetchUsers();
      return true;
    } catch (error) {
      console.error('Failed to update user:', error);
      throw error;
    }
  }, [fetchUsers]);

  // 删除用户
  const deleteUser = useCallback(async (userId: number): Promise<boolean> => {
    try {
      await userApi.deleteUser(userId);
      // 刷新列表
      await fetchUsers();
      return true;
    } catch (error) {
      console.error('Failed to delete user:', error);
      throw error;
    }
  }, [fetchUsers]);

  // 修改密码
  const changePassword = useCallback(async (
    userId: number,
    data: ChangePasswordRequest
  ): Promise<boolean> => {
    try {
      await userApi.changePassword(userId, data);
      return true;
    } catch (error) {
      console.error('Failed to change password:', error);
      throw error;
    }
  }, []);

  // 切换页码
  const setPage = useCallback((page: number) => {
    fetchUsers(page, state.pagination.size);
  }, [fetchUsers, state.pagination.size]);

  // 切换每页数量
  const setPageSize = useCallback((size: number) => {
    fetchUsers(0, size);
  }, [fetchUsers]);

  // 初始加载
  useEffect(() => {
    const loadUsers = async () => {
      await fetchUsers();
    };
    loadUsers();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return {
    ...state,
    fetchUsers,
    createUser,
    updateUser,
    deleteUser,
    changePassword,
    setPage,
    setPageSize,
    refresh: () => fetchUsers(),
  };
}

// 单个用户 Hook
export function useUser(userId: number | null) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchUser = useCallback(async () => {
    if (!userId) return;

    setLoading(true);
    setError(null);

    try {
      const data = await userApi.getUser(userId);
      setUser(data);
    } catch (err) {
      const message = err instanceof Error ? err.message : '获取用户信息失败';
      setError(message);
      console.error('Failed to fetch user:', err);
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    if (userId) {
      fetchUser();
    }
  }, [userId, fetchUser]);

  return {
    user,
    loading,
    error,
    refresh: fetchUser,
  };
}

export default useUsers;
