// 删除用户对话框组件
'use client';

import { useState } from 'react';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { UserInfo } from '@/lib/auth/token';

interface DeleteUserDialogProps {
  user: UserInfo | null;
  onDeleteUser: (userId: string) => Promise<void>;
  trigger?: React.ReactNode;
}

export function DeleteUserDialog({ user, onDeleteUser, trigger }: DeleteUserDialogProps) {
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const handleDelete = async () => {
    if (!user) return;
    
    try {
      setIsDeleting(true);
      setDeleteError(null);
      
      // 模拟API调用延迟
      await new Promise(resolve => setTimeout(resolve, 800));
      
      await onDeleteUser(user.id);
      
      console.log('User deleted successfully:', user.username);
      
    } catch (error) {
      console.error('Delete user failed:', error);
      setDeleteError(error instanceof Error ? error.message : '删除用户失败，请重试');
    } finally {
      setIsDeleting(false);
    }
  };

  const defaultTrigger = (
    <Button variant="outline" size="sm">
      删除
    </Button>
  );

  // 如果没有用户数据，禁用对话框
  if (!user) {
    return <>{defaultTrigger}</>;
  }

  const isAdmin = user.role === 'admin';

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        {trigger || defaultTrigger}
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle className="text-red-600">
            {isAdmin ? '⚠️ 删除管理员账户' : '删除用户'}
          </AlertDialogTitle>
          <AlertDialogDescription>
            {isAdmin ? (
              <div className="space-y-2">
                <p>您即将删除管理员账户 <strong>{user.username}</strong>，此操作将：</p>
                <ul className="list-disc list-inside space-y-1 text-sm">
                  <li>永久删除该管理员账户</li>
                  <li>无法恢复所有相关权限</li>
                  <li>可能影响系统管理功能</li>
                </ul>
                <p className="font-medium text-red-600">请确认您真的需要删除此管理员账户吗？</p>
              </div>
            ) : (
              <div className="space-y-2">
                <p>您即将删除用户 <strong>{user.username}</strong>，此操作将：</p>
                <ul className="list-disc list-inside space-y-1 text-sm">
                  <li>永久删除该用户账户</li>
                  <li>清除该用户的所有数据</li>
                  <li>无法恢复此操作</li>
                </ul>
                <p className="font-medium">请确认您真的需要删除此用户吗？</p>
              </div>
            )}
          </AlertDialogDescription>
        </AlertDialogHeader>
        
        {/* 错误提示 */}
        {deleteError && (
          <div className="p-3 rounded-md bg-red-50 border border-red-200">
            <p className="text-sm text-red-600">{deleteError}</p>
          </div>
        )}
        
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isDeleting}>
            取消
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleDelete}
            disabled={isDeleting}
            className={isAdmin ? "bg-red-600 hover:bg-red-700" : ""}
          >
            {isDeleting ? (
              <div className="flex items-center space-x-2">
                <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                <span>删除中...</span>
              </div>
            ) : isAdmin ? (
              '⚠️ 确认删除管理员'
            ) : (
              '确认删除'
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
