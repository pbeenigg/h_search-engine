// 登录页面
'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useAuth } from '@/hooks/auth/useAuth';
import { authApi } from '@/lib/api';
import { APP_NAME, ROUTES } from '@/lib/constants';

// 表单验证schema
const loginSchema = z.object({
  username: z.string().min(1, '用户名不能为空').min(3, '用户名至少3个字符'),
  password: z.string().min(1, '密码不能为空').min(6, '密码至少6个字符'),
});

// 表单数据类型
type LoginForm = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const { login, error, isLoading } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
    reset,
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    mode: 'onChange',
  });

  const onSubmit = async (data: LoginForm) => {
    try {
      setIsSubmitting(true);
      setSubmitError(null);
      
      // 调用后端登录API
      const result = await authApi.login({
        username: data.username,
        password: data.password,
      });
      
      // 执行登录 - 将后端返回的数据转换为前端需要的格式
      login(
        result.tokenValue,
        {
          id: result.userId.toString(),
          username: result.userName,
          email: `${result.userName}@heytrip.com`, // 后端暂未返回email，使用默认值
          role: 'admin', // 后端暂未返回角色，默认admin
          createdAt: new Date().toISOString(),
        },
        86400 // 24小时过期
      );
      
      // 重置表单
      reset();
      
      // 登录成功后跳转到仪表盘
      router.push(ROUTES.DASHBOARD);
      
    } catch (error) {
      console.error('Login failed:', error);
      setSubmitError(error instanceof Error ? error.message : '登录失败，请检查用户名和密码');
    } finally {
      setIsSubmitting(false);
    }
  };

  const displayError = submitError || error;

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        {/* Logo和标题 */}
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">{APP_NAME}</h1>
          <h2 className="text-xl font-semibold text-gray-700">后台管理系统</h2>
          <p className="mt-2 text-sm text-gray-600">
            请使用您的账户信息登录
          </p>
        </div>

        {/* 登录表单 */}
        <Card>
          <CardHeader>
            <CardTitle>登录</CardTitle>
            <CardDescription>
              输入您的用户名和密码来访问系统
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              {/* 用户名输入 */}
              <div className="space-y-2">
                <Label htmlFor="username">用户名</Label>
                <Input
                  id="username"
                  type="text"
                  placeholder="请输入用户名"
                  {...register('username')}
                  className={errors.username ? 'border-red-500' : ''}
                />
                {errors.username && (
                  <p className="text-sm text-red-500">{errors.username.message}</p>
                )}
              </div>

              {/* 密码输入 */}
              <div className="space-y-2">
                <Label htmlFor="password">密码</Label>
                <Input
                  id="password"
                  type="password"
                  placeholder="请输入密码"
                  {...register('password')}
                  className={errors.password ? 'border-red-500' : ''}
                />
                {errors.password && (
                  <p className="text-sm text-red-500">{errors.password.message}</p>
                )}
              </div>

              {/* 错误提示 */}
              {displayError && (
                <div className="p-3 rounded-md bg-red-50 border border-red-200">
                  <p className="text-sm text-red-600">{displayError}</p>
                </div>
              )}

              {/* 提交按钮 */}
              <Button
                type="submit"
                className="w-full"
                disabled={!isValid || isSubmitting || isLoading}
              >
                {isSubmitting || isLoading ? (
                  <div className="flex items-center space-x-2">
                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>登录中...</span>
                  </div>
                ) : (
                  '登录'
                )}
              </Button>
            </form>
          </CardContent>
        </Card>

        {/* 演示账户信息 */}
        
      </div>
    </div>
  );
}
