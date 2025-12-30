// 受保护的路由组件
'use client';

import { useAuthState } from '@/hooks/auth/useAuth';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { APP_NAME } from '@/lib/constants';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: string;
  fallbackPath?: string;
}

export function ProtectedRoute({ 
  children, 
  requiredRole, 
  fallbackPath = '/login' 
}: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, user } = useAuthState();
  const router = useRouter();

  useEffect(() => {
    // 如果还在加载认证状态，不做处理
    if (isLoading) {
      return;
    }

    // 如果未认证，重定向到登录页
    if (!isAuthenticated) {
      router.push(fallbackPath);
      return;
    }

    // 如果需要特定角色权限，检查用户角色
    if (requiredRole && user?.role !== requiredRole) {
      router.push('/unauthorized');
      return;
    }
  }, [isAuthenticated, isLoading, user, requiredRole, router, fallbackPath]);

  // 加载状态显示
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <Card className="w-96">
          <CardHeader className="text-center">
            <CardTitle className="text-xl">{APP_NAME}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-center space-y-4">
              <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
              <p className="text-gray-600">正在验证用户身份...</p>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  // 未认证状态显示（短暂显示，重定向时）
  if (!isAuthenticated) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <Card className="w-96">
          <CardHeader className="text-center">
            <CardTitle className="text-xl">{APP_NAME}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-center space-y-4">
              <p className="text-gray-600">正在跳转到登录页面...</p>
              <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto"></div>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  // 权限不足显示
  if (requiredRole && user?.role !== requiredRole) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <Card className="w-96">
          <CardHeader className="text-center">
            <CardTitle className="text-xl text-red-600">访问权限不足</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-center space-y-4">
              <p className="text-gray-600">
                您没有权限访问此页面
              </p>
              <p className="text-sm text-gray-500">
                当前角色: {user?.role} | 所需角色: {requiredRole}
              </p>
              <button 
                onClick={() => router.back()}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
              >
                返回上一页
              </button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  // 认证成功且有权限，显示子组件
  return <>{children}</>;
}
