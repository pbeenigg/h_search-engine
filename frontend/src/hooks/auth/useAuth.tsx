// 认证状态管理 Hook
'use client';

import { useState, useEffect, createContext, useContext, ReactNode } from 'react';
import { tokenManager, UserInfo } from '@/lib/auth/token';
import { authApi } from '@/lib/api';
import { DEBUG_MODE } from '@/lib/constants';

// 认证状态类型
export interface AuthState {
  user: UserInfo | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

// 认证方法类型
export interface AuthActions {
  login: (token: string, userInfo: UserInfo, expiresIn?: number) => void;
  logout: () => void;
  refreshToken: () => Promise<boolean>;
  clearError: () => void;
}

// 完整的认证 Context 类型
export interface AuthContextType extends AuthState, AuthActions {}

// 创建认证 Context
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// 认证 Provider 组件
interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [state, setState] = useState<AuthState>({
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: true,
    error: null,
  });

  // 初始化认证状态
  useEffect(() => {
    initializeAuth();
  }, []);

  // 初始化认证状态
  const initializeAuth = async () => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));

      const token = tokenManager.get();
      const userInfo = tokenManager.getUser();

      if (token && userInfo) {
        setState({
          user: userInfo,
          token,
          isAuthenticated: true,
          isLoading: false,
          error: null,
        });

        if (DEBUG_MODE) {
          console.log('Auth initialized with existing token', {
            user: userInfo.username,
            tokenValid: true,
          });
        }
      } else {
        setState({
          user: null,
          token: null,
          isAuthenticated: false,
          isLoading: false,
          error: null,
        });
      }
    } catch (error) {
      console.error('Failed to initialize auth:', error);
      setState({
        user: null,
        token: null,
        isAuthenticated: false,
        isLoading: false,
        error: '认证初始化失败',
      });
    }
  };

  // 登录
  const login = (token: string, userInfo: UserInfo, expiresIn: number = 3600) => {
    try {
      tokenManager.set(token, userInfo, expiresIn);
      
      setState({
        user: userInfo,
        token,
        isAuthenticated: true,
        isLoading: false,
        error: null,
      });

      if (DEBUG_MODE) {
        console.log('User logged in successfully', {
          username: userInfo.username,
          role: userInfo.role,
        });
      }
    } catch (error) {
      console.error('Login failed:', error);
      setState(prev => ({
        ...prev,
        error: '登录失败',
        isLoading: false,
      }));
    }
  };

  // 登出
  const logout = async () => {
    try {
      // 调用后端登出 API
      try {
        await authApi.logout();
      } catch (apiError) {
        // 即使后端登出失败，也继续执行本地登出
        console.warn('Backend logout failed:', apiError);
      }

      tokenManager.clear();
      
      setState({
        user: null,
        token: null,
        isAuthenticated: false,
        isLoading: false,
        error: null,
      });

      // 重定向到登录页面
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }

      if (DEBUG_MODE) {
        console.log('User logged out successfully');
      }
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  // 刷新 Token
  const refreshToken = async (): Promise<boolean> => {
    try {
      setState(prev => ({ ...prev, isLoading: true, error: null }));

      const success = await tokenManager.refresh();
      
      if (success) {
        const token = tokenManager.get();
        const userInfo = tokenManager.getUser();
        
        if (token && userInfo) {
          setState({
            user: userInfo,
            token,
            isAuthenticated: true,
            isLoading: false,
            error: null,
          });
        } else {
          // Token 刷新失败，执行登出
          logout();
        }
      } else {
        // Token 刷新失败，执行登出
        logout();
      }

      return success;
    } catch (error) {
      console.error('Token refresh failed:', error);
      logout();
      return false;
    }
  };

  // 清除错误
  const clearError = () => {
    setState(prev => ({ ...prev, error: null }));
  };

  const contextValue: AuthContextType = {
    ...state,
    login,
    logout,
    refreshToken,
    clearError,
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

// 使用认证 Context 的 Hook
export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

// 便捷的 Hook - 只获取状态
export function useAuthState() {
  const { user, token, isAuthenticated, isLoading, error } = useAuth();
  return { user, token, isAuthenticated, isLoading, error };
}

// 便捷的 Hook - 只获取方法
export function useAuthActions() {
  const { login, logout, refreshToken, clearError } = useAuth();
  return { login, logout, refreshToken, clearError };
}

// 检查用户权限的 Hook
export function usePermissions() {
  const { user } = useAuthState();

  const hasPermission = (permission: string): boolean => {
    if (!user) return false;
    
    // 根据用户角色判断权限
    switch (user.role) {
      case 'admin':
        return true; // 管理员拥有所有权限
      case 'user':
        return permission === 'read' || permission === 'write';
      case 'viewer':
        return permission === 'read';
      default:
        return false;
    }
  };

  const hasRole = (role: string): boolean => {
    return user?.role === role;
  };

  const isAdmin = (): boolean => {
    return hasRole('admin');
  };

  const canManageUsers = (): boolean => {
    return hasPermission('manage_users') || isAdmin();
  };

  return {
    hasPermission,
    hasRole,
    isAdmin,
    canManageUsers,
    userRole: user?.role || null,
  };
}

export default useAuth;
