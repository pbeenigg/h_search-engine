// 创建用户对话框组件
'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { UserInfo } from '@/lib/auth/token';

// 表单验证schema
const createUserSchema = z.object({
  username: z.string().min(1, '用户名不能为空').min(3, '用户名至少3个字符').max(20, '用户名不能超过20个字符'),
  email: z.string().min(1, '邮箱不能为空').email('请输入有效的邮箱地址'),
  password: z.string().min(1, '密码不能为空').min(6, '密码至少6个字符').max(50, '密码不能超过50个字符'),
  confirmPassword: z.string().min(1, '请确认密码'),
  role: z.string().min(1, '请选择用户角色'),
}).refine((data) => data.password === data.confirmPassword, {
  message: '密码不一致',
  path: ['confirmPassword'],
});

// 表单数据类型
type CreateUserForm = z.infer<typeof createUserSchema>;

interface CreateUserDialogProps {
  onCreateUser: (user: Omit<UserInfo, 'id' | 'createdAt'> & { password: string }) => Promise<void>;
  trigger?: React.ReactNode;
}

export function CreateUserDialog({ onCreateUser, trigger }: CreateUserDialogProps) {
  const [open, setOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isValid },
    reset,
    setValue,
    watch,
  } = useForm<CreateUserForm>({
    resolver: zodResolver(createUserSchema),
    mode: 'onChange',
  });

  const watchedRole = watch('role');

  const onSubmit = async (data: CreateUserForm) => {
    try {
      setIsSubmitting(true);
      setSubmitError(null);

      // 验证角色是否有效
      if (!['admin', 'user', 'viewer'].includes(data.role)) {
        throw new Error('请选择有效的用户角色');
      }

      const newUser = {
        username: data.username,
        email: data.email,
        role: data.role as 'admin' | 'user' | 'viewer',
        password: data.password,
      };

      await onCreateUser(newUser);
      
      // 重置表单
      reset();
      setOpen(false);
      
    } catch (error) {
      console.error('Create user failed:', error);
      setSubmitError(error instanceof Error ? error.message : '创建用户失败，请重试');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDialogOpenChange = (newOpen: boolean) => {
    setOpen(newOpen);
    if (!newOpen) {
      reset();
      setSubmitError(null);
    }
  };

  const roleOptions = [
    { value: 'admin', label: '管理员', description: '拥有系统管理权限' },
    { value: 'user', label: '用户', description: '基本使用权限' },
    { value: 'viewer', label: '查看者', description: '仅可查看数据' },
  ];

  const defaultTrigger = (
    <Button>
      新增用户
    </Button>
  );

  return (
    <Dialog open={open} onOpenChange={handleDialogOpenChange}>
      <DialogTrigger asChild>
        {trigger || defaultTrigger}
      </DialogTrigger>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>创建新用户</DialogTitle>
          <DialogDescription>
            填写以下信息创建一个新的用户账户
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* 用户名输入 */}
          <div className="space-y-2">
            <Label htmlFor="username">用户名 *</Label>
            <Input
              id="username"
              placeholder="请输入用户名"
              {...register('username')}
              className={errors.username ? 'border-red-500' : ''}
            />
            {errors.username && (
              <p className="text-sm text-red-500">{errors.username.message}</p>
            )}
          </div>

          {/* 邮箱输入 */}
          <div className="space-y-2">
            <Label htmlFor="email">邮箱 *</Label>
            <Input
              id="email"
              type="email"
              placeholder="请输入邮箱地址"
              {...register('email')}
              className={errors.email ? 'border-red-500' : ''}
            />
            {errors.email && (
              <p className="text-sm text-red-500">{errors.email.message}</p>
            )}
          </div>

          {/* 密码输入 */}
          <div className="space-y-2">
            <Label htmlFor="password">密码 *</Label>
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

          {/* 确认密码输入 */}
          <div className="space-y-2">
            <Label htmlFor="confirmPassword">确认密码 *</Label>
            <Input
              id="confirmPassword"
              type="password"
              placeholder="请再次输入密码"
              {...register('confirmPassword')}
              className={errors.confirmPassword ? 'border-red-500' : ''}
            />
            {errors.confirmPassword && (
              <p className="text-sm text-red-500">{errors.confirmPassword.message}</p>
            )}
          </div>

          {/* 角色选择 */}
          <div className="space-y-2">
            <Label htmlFor="role">用户角色 *</Label>
            <Select onValueChange={(value) => setValue('role', value)} value={watchedRole}>
              <SelectTrigger className={errors.role ? 'border-red-500' : ''}>
                <SelectValue placeholder="选择用户角色" />
              </SelectTrigger>
              <SelectContent>
                {roleOptions.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    <div>
                      <div className="font-medium">{option.label}</div>
                      <div className="text-sm text-gray-500">{option.description}</div>
                    </div>
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.role && (
              <p className="text-sm text-red-500">{errors.role.message}</p>
            )}
          </div>

          {/* 错误提示 */}
          {submitError && (
            <div className="p-3 rounded-md bg-red-50 border border-red-200">
              <p className="text-sm text-red-600">{submitError}</p>
            </div>
          )}

          {/* 角色说明卡片 */}
          {watchedRole && (
            <Card className="bg-blue-50 border-blue-200">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-blue-800">
                  {roleOptions.find(opt => opt.value === watchedRole)?.label} 权限说明
                </CardTitle>
              </CardHeader>
              <CardContent className="pt-0">
                <p className="text-sm text-blue-700">
                  {roleOptions.find(opt => opt.value === watchedRole)?.description}
                </p>
              </CardContent>
            </Card>
          )}

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => handleDialogOpenChange(false)}
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={!isValid || isSubmitting}
            >
              {isSubmitting ? (
                <div className="flex items-center space-x-2">
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>创建中...</span>
                </div>
              ) : (
                '创建用户'
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
