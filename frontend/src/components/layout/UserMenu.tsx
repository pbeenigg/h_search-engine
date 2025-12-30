// 用户菜单组件
// 包含：查看用户信息、修改密码、退出登录功能

'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { User, LogOut, KeyRound, UserCircle, ChevronDown } from 'lucide-react';
import { useAuth } from '@/hooks/auth/useAuth';
import { authApi, userApi } from '@/lib/api';
import { ROUTES } from '@/lib/constants';
import type { CurrentUserInfo } from '@/types/auth';

export function UserMenu() {
  const router = useRouter();
  const { user, logout: authLogout, isAuthenticated } = useAuth();
  
  // 弹窗状态
  const [showUserInfo, setShowUserInfo] = useState(false);
  const [showChangePassword, setShowChangePassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // 用户详细信息
  const [userInfo, setUserInfo] = useState<CurrentUserInfo | null>(null);
  
  // 修改密码表单
  const [passwordForm, setPasswordForm] = useState({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  // 获取用户信息
  const handleViewUserInfo = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const info = await authApi.getCurrentUser();
      setUserInfo(info);
      setShowUserInfo(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : '获取用户信息失败');
    } finally {
      setIsLoading(false);
    }
  };

  // 退出登录
  const handleLogout = async () => {
    setIsLoading(true);
    try {
      await authApi.logout();
    } catch (err) {
      console.error('Logout API error:', err);
    } finally {
      authLogout();
      router.push(ROUTES.LOGIN);
    }
  };

  // 打开修改密码弹窗
  const handleOpenChangePassword = () => {
    setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
    setError(null);
    setShowChangePassword(true);
  };

  // 验证密码表单
  const validatePasswordForm = (): string | null => {
    const { oldPassword, newPassword, confirmPassword } = passwordForm;
    
    if (!oldPassword || !newPassword || !confirmPassword) {
      return '请填写所有密码字段';
    }
    if (newPassword !== confirmPassword) {
      return '两次输入的新密码不一致';
    }
    if (newPassword.length < 6) {
      return '新密码长度不能少于6位';
    }
    return null;
  };

  // 执行密码修改 API 调用
  const submitPasswordChange = async (userId: number): Promise<void> => {
    await userApi.changePassword(userId, {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    });
    setShowChangePassword(false);
    setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' });
    alert('密码修改成功');
  };

  // 获取用户ID
  const getCurrentUserId = (): number | null => {
    return user?.id ? parseInt(user.id) : null;
  };

  // 处理错误信息
  const getErrorMessage = (err: unknown): string => {
    return err instanceof Error ? err.message : '修改密码失败';
  };

  // 提交修改密码
  const handleChangePassword = async () => {
    const validationError = validatePasswordForm();
    if (validationError) {
      setError(validationError);
      return;
    }

    const userId = getCurrentUserId();
    if (!userId) {
      setError('无法获取当前用户信息');
      return;
    }

    setIsLoading(true);
    setError(null);
    
    try {
      await submitPasswordChange(userId);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setIsLoading(false);
    }
  };

  // 性别显示映射
  const SEX_LABELS: Record<string, string> = {
    M: '男',
    F: '女',
    U: '未知',
    O: '其他',
  };
  
  const getSexLabel = (sex: string): string => SEX_LABELS[sex] ?? sex;

  if (!isAuthenticated) {
    return (
      <Button size="sm" className="btn" onClick={() => router.push(ROUTES.LOGIN)}>
        <User className="mr-2 h-4 w-4" />
        登录
      </Button>
    );
  }

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button size="sm" className="btn">
            <User className="mr-2 h-4 w-4" />
            {user?.username || '用户'}
            <ChevronDown className="ml-1 h-3 w-3" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-48">
          <DropdownMenuItem 
            className="cursor-pointer"
            onClick={handleViewUserInfo}
            disabled={isLoading}
          >
            <UserCircle className="mr-2 h-4 w-4" />
            查看用户信息
          </DropdownMenuItem>
          <DropdownMenuItem 
            className="cursor-pointer"
            onClick={handleOpenChangePassword}
          >
            <KeyRound className="mr-2 h-4 w-4" />
            修改密码
          </DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem 
            className="cursor-pointer text-red-600 focus:text-red-600"
            onClick={handleLogout}
            disabled={isLoading}
          >
            <LogOut className="mr-2 h-4 w-4" />
            退出登录
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      {/* 用户信息弹窗 */}
      <Dialog open={showUserInfo} onOpenChange={setShowUserInfo}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>用户信息</DialogTitle>
            <DialogDescription>
              当前登录用户的详细信息
            </DialogDescription>
          </DialogHeader>
          {userInfo && (
            <div className="space-y-4 py-4">
              <div className="grid grid-cols-3 gap-4 items-center">
                <Label className="text-right text-muted-foreground">用户ID</Label>
                <div className="col-span-2 font-medium">{userInfo.userId}</div>
              </div>
              <div className="grid grid-cols-3 gap-4 items-center">
                <Label className="text-right text-muted-foreground">用户名</Label>
                <div className="col-span-2 font-medium">{userInfo.userName}</div>
              </div>
              <div className="grid grid-cols-3 gap-4 items-center">
                <Label className="text-right text-muted-foreground">昵称</Label>
                <div className="col-span-2 font-medium">{userInfo.userNick}</div>
              </div>
              <div className="grid grid-cols-3 gap-4 items-center">
                <Label className="text-right text-muted-foreground">性别</Label>
                <div className="col-span-2 font-medium">{getSexLabel(userInfo.sex)}</div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowUserInfo(false)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 修改密码弹窗 */}
      <Dialog open={showChangePassword} onOpenChange={setShowChangePassword}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>修改密码</DialogTitle>
            <DialogDescription>
              请输入原密码和新密码
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            {error && (
              <div className="text-sm text-red-600 bg-red-50 p-3 rounded-md">
                {error}
              </div>
            )}
            <div className="space-y-2">
              <Label htmlFor="oldPassword">原密码</Label>
              <Input
                id="oldPassword"
                type="password"
                placeholder="请输入原密码"
                value={passwordForm.oldPassword}
                onChange={(e) => setPasswordForm(prev => ({ ...prev, oldPassword: e.target.value }))}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="newPassword">新密码</Label>
              <Input
                id="newPassword"
                type="password"
                placeholder="请输入新密码（至少6位）"
                value={passwordForm.newPassword}
                onChange={(e) => setPasswordForm(prev => ({ ...prev, newPassword: e.target.value }))}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="confirmPassword">确认新密码</Label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                value={passwordForm.confirmPassword}
                onChange={(e) => setPasswordForm(prev => ({ ...prev, confirmPassword: e.target.value }))}
              />
            </div>
          </div>
          <DialogFooter>
            <Button 
              variant="outline" 
              onClick={() => setShowChangePassword(false)}
              disabled={isLoading}
            >
              取消
            </Button>
            <Button 
              onClick={handleChangePassword}
              disabled={isLoading}
            >
              {isLoading ? '提交中...' : '确认修改'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
