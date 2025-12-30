// 首页 - 自动重定向到登录页面或仪表盘
'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthState } from '@/hooks/auth/useAuth';
import { APP_NAME } from '@/lib/constants';
import { Loader2 } from 'lucide-react';

export default function HomePage() {
  const router = useRouter();
  const { isAuthenticated, user } = useAuthState();

  useEffect(() => {
    // 根据认证状态进行重定向
    if (isAuthenticated) {
      // 已登录，重定向到仪表盘
      router.push('/dashboard');
    } else {
      // 未登录，重定向到登录页面
      router.push('/login');
    }
  }, [isAuthenticated, router]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="flex items-center justify-center mb-4">
          <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
        </div>
        <h1 className="text-2xl font-bold text-gray-900 mb-2">{APP_NAME}</h1>
        <p className="text-gray-600">正在加载...</p>
      </div>
    </div>
  );
}
