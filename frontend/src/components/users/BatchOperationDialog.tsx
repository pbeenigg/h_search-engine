'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from '@/components/ui/alert-dialog';
import { Trash2, Users } from 'lucide-react';

// 用户信息接口
interface UserInfo {
  id: string;
  username: string;
  email: string;
  role: 'admin' | 'user' | 'viewer';
  createdAt: string;
}

interface BatchOperationDialogProps {
  selectedUsers: UserInfo[];
  onBatchDelete: (userIds: string[]) => Promise<void>;
  onBatchUpdateRole: (userIds: string[], role: UserInfo['role']) => Promise<void>;
  onBatchOperationComplete?: () => void;
}

const batchOperationSchema = z.object({
  operation: z.enum(['delete', 'updateRole']),
  newRole: z.enum(['admin', 'user', 'viewer']).optional(),
});

type BatchOperationFormData = z.infer<typeof batchOperationSchema>;

export function BatchOperationDialog({ 
  selectedUsers, 
  onBatchDelete, 
  onBatchUpdateRole, 
  onBatchOperationComplete 
}: BatchOperationDialogProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [operation, setOperation] = useState<'delete' | 'updateRole'>('updateRole');

  const form = useForm<BatchOperationFormData>({
    resolver: zodResolver(batchOperationSchema),
    defaultValues: {
      operation: 'updateRole',
      newRole: 'user',
    },
  });

  const handleSubmit = async (data: BatchOperationFormData) => {
    setIsSubmitting(true);
    try {
      const userIds = selectedUsers.map(user => user.id);
      
      if (data.operation === 'delete') {
        await onBatchDelete(userIds);
      } else if (data.operation === 'updateRole' && data.newRole) {
        await onBatchUpdateRole(userIds, data.newRole);
      }
      
      setIsOpen(false);
      form.reset();
      onBatchOperationComplete?.();
    } catch (error) {
      console.error('批量操作失败:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      setIsOpen(false);
      form.reset();
    }
  };

  const handleOperationChange = (value: 'delete' | 'updateRole') => {
    setOperation(value);
    form.setValue('operation', value);
  };

  const getOperationDescription = () => {
    if (selectedUsers.length === 0) return '请先选择要操作的用户';
    
    const userNames = selectedUsers.map(user => user.username).join(', ');
    
    if (operation === 'delete') {
      return `将删除以下 ${selectedUsers.length} 个用户：${userNames}`;
    } else {
      const newRole = form.watch('newRole');
      const roleName = newRole === 'admin' ? '管理员' : newRole === 'user' ? '用户' : '查看者';
      return `将把以下 ${selectedUsers.length} 个用户的角色修改为${roleName}：${userNames}`;
    }
  };

  const hasAdmins = selectedUsers.some(user => user.role === 'admin');

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogTrigger asChild>
        <Button 
          variant="outline" 
          disabled={selectedUsers.length === 0}
        >
          <Users className="h-4 w-4 mr-2" />
          批量操作 ({selectedUsers.length})
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>批量操作</DialogTitle>
          <DialogDescription>
            对选中的 {selectedUsers.length} 个用户进行批量操作。
          </DialogDescription>
        </DialogHeader>
        
        <div className="space-y-4">
          <div className="bg-gray-50 p-4 rounded-lg">
            <h4 className="font-medium mb-2">已选用户：</h4>
            <div className="text-sm text-gray-600 max-h-32 overflow-y-auto">
              {selectedUsers.map(user => (
                <div key={user.id} className="flex items-center justify-between py-1">
                  <span>{user.username} ({user.email})</span>
                  <span className="text-xs bg-gray-200 px-2 py-1 rounded">
                    {user.role === 'admin' ? '管理员' : user.role === 'user' ? '用户' : '查看者'}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="operation"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>操作类型</FormLabel>
                    <Select onValueChange={(value: 'delete' | 'updateRole') => {
                      field.onChange(value);
                      handleOperationChange(value);
                    }} value={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="选择操作类型" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="updateRole">批量修改角色</SelectItem>
                        <SelectItem value="delete" className="text-red-600">
                          批量删除用户
                        </SelectItem>
                      </SelectContent>
                    </Select>
                    <FormDescription>
                      选择要对选中的用户执行的操作
                    </FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {operation === 'updateRole' && (
                <FormField
                  control={form.control}
                  name="newRole"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>新角色 *</FormLabel>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="选择新角色" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="admin">管理员</SelectItem>
                          <SelectItem value="user">用户</SelectItem>
                          <SelectItem value="viewer">查看者</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormDescription>
                        选择要修改的目标角色
                      </FormDescription>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}

              {operation === 'delete' && hasAdmins && (
                <div className="bg-red-50 border border-red-200 p-3 rounded-md">
                  <p className="text-red-800 text-sm">
                    ⚠️ <strong>警告：</strong>选中的用户中包含管理员账户。删除管理员账户可能会影响系统管理功能。
                  </p>
                </div>
              )}

              <div className="bg-blue-50 border border-blue-200 p-3 rounded-md">
                <p className="text-blue-800 text-sm">
                  {getOperationDescription()}
                </p>
              </div>
            </form>
          </Form>
        </div>

        <DialogFooter>
          <Button
            type="button"
            variant="outline"
            onClick={handleClose}
            disabled={isSubmitting}
          >
            取消
          </Button>
          
          {operation === 'delete' ? (
            <AlertDialog>
              <AlertDialogTrigger asChild>
                <Button 
                  type="button" 
                  variant="destructive" 
                  disabled={isSubmitting || selectedUsers.length === 0}
                >
                  <Trash2 className="h-4 w-4 mr-2" />
                  {isSubmitting ? '删除中...' : '确认删除'}
                </Button>
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>确认批量删除</AlertDialogTitle>
                  <AlertDialogDescription>
                    您确定要删除选中的 {selectedUsers.length} 个用户吗？此操作不可撤销。
                    {hasAdmins && " 请注意，选中的用户中包含管理员账户。"}
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel>取消</AlertDialogCancel>
                  <AlertDialogAction
                    onClick={form.handleSubmit(handleSubmit)}
                    className="bg-red-600 hover:bg-red-700"
                    disabled={isSubmitting}
                  >
                    确认删除
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          ) : (
            <Button 
              type="submit" 
              onClick={form.handleSubmit(handleSubmit)}
              disabled={isSubmitting || selectedUsers.length === 0}
            >
              {isSubmitting ? '处理中...' : '确认修改'}
            </Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}