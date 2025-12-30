'use client';

import { useState } from 'react';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { Trash2 } from 'lucide-react';

// 应用信息接口
interface AppInfo {
  id: string;
  name: string;
  appId: string;
  secretKey: string;
  status: 'active' | 'inactive' | 'suspended';
  rateLimit: number;
  creator: string;
  createdAt: string;
  lastUsedAt?: string;
  description?: string;
}

interface DeleteAppDialogProps {
  app: AppInfo;
  onDeleteApp: (appId: string) => Promise<void>;
}

export function DeleteAppDialog({ app, onDeleteApp }: DeleteAppDialogProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleDelete = async () => {
    setIsSubmitting(true);
    try {
      await onDeleteApp(app.id);
    } catch (error) {
      console.error('删除应用失败:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const isActive = app.status === 'active';
  const isProduction = app.secretKey.startsWith('sk_live_');

  return (
    <AlertDialog>
      <AlertDialogTrigger asChild>
        <Button variant="outline" size="sm">
          <Trash2 className="h-4 w-4" />
        </Button>
      </AlertDialogTrigger>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>删除应用</AlertDialogTitle>
          <AlertDialogDescription className="space-y-3">
            <p>您确定要删除应用 "{app.name}" 吗？</p>
            
            <div className="bg-gray-50 p-3 rounded-md text-sm">
              <div><strong>应用名称:</strong> {app.name}</div>
              <div><strong>AppID:</strong> {app.appId}</div>
              <div><strong>状态:</strong> {app.status}</div>
            </div>

            {isActive && (
              <div className="bg-red-50 border border-red-200 p-3 rounded-md text-red-800">
                <p className="font-medium">⚠️ 警告</p>
                <p>此应用当前处于活跃状态，删除后可能会影响正在使用此API的应用。</p>
              </div>
            )}

            {isProduction && (
              <div className="bg-orange-50 border border-orange-200 p-3 rounded-md text-orange-800">
                <p className="font-medium">⚠️ 生产环境密钥</p>
                <p>此应用使用生产环境密钥( sk_live_ )，删除前请确保已通知相关方。</p>
              </div>
            )}

            <p className="text-red-600 font-medium">
              此操作不可撤销，所有相关数据将永久删除。
            </p>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel disabled={isSubmitting}>
            取消
          </AlertDialogCancel>
          <AlertDialogAction
            onClick={handleDelete}
            disabled={isSubmitting}
            className="bg-red-600 hover:bg-red-700"
          >
            {isSubmitting ? '删除中...' : '确认删除'}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}