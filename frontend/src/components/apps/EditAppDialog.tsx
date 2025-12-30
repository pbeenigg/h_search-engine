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
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';

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

// 表单验证模式
const appSchema = z.object({
  name: z.string().min(1, '应用名称不能为空'),
  description: z.string().optional(),
  status: z.enum(['active', 'inactive', 'suspended']),
  rateLimit: z.number().min(1, '限流值必须大于0'),
});

type AppFormData = z.infer<typeof appSchema>;

interface EditAppDialogProps {
  app: AppInfo;
  onEditApp: (appData: AppInfo) => Promise<void>;
}

export function EditAppDialog({ app, onEditApp }: EditAppDialogProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useForm<AppFormData>({
    resolver: zodResolver(appSchema),
    defaultValues: {
      name: app.name,
      description: app.description || '',
      status: app.status,
      rateLimit: app.rateLimit,
    },
  });

  const handleSubmit = async (data: AppFormData) => {
    setIsSubmitting(true);
    try {
      await onEditApp({
        ...app,
        ...data,
      });
      
      setIsOpen(false);
    } catch (error) {
      console.error('编辑应用失败:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    if (!isSubmitting) {
      setIsOpen(false);
      // 重置表单到原始值
      form.reset({
        name: app.name,
        description: app.description || '',
        status: app.status,
        rateLimit: app.rateLimit,
      });
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm">编辑</Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>编辑应用</DialogTitle>
          <DialogDescription>
            修改应用的基本信息和配置。
          </DialogDescription>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(handleSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="name"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>应用名称 *</FormLabel>
                  <FormControl>
                    <Input placeholder="输入应用名称" {...field} />
                  </FormControl>
                  <FormDescription>
                    应用的可识别名称
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />
            
            <FormField
              control={form.control}
              name="description"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>应用描述</FormLabel>
                  <FormControl>
                    <Textarea 
                      placeholder="输入应用描述（可选）"
                      {...field}
                      rows={3}
                    />
                  </FormControl>
                  <FormDescription>
                    描述应用的用途和功能
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="status"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>应用状态 *</FormLabel>
                  <Select onValueChange={field.onChange} value={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="选择应用状态" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      <SelectItem value="active">活跃</SelectItem>
                      <SelectItem value="inactive">未激活</SelectItem>
                      <SelectItem value="suspended">已暂停</SelectItem>
                    </SelectContent>
                  </Select>
                  <FormDescription>
                    更改应用的状态
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <FormField
              control={form.control}
              name="rateLimit"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>限流设置 *</FormLabel>
                  <FormControl>
                    <Input 
                      type="number" 
                      placeholder="输入限流值（请求/分钟）"
                      {...field}
                      onChange={(e) => field.onChange(Number(e.target.value))}
                    />
                  </FormControl>
                  <FormDescription>
                    每分钟允许的API请求次数
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

            <div className="bg-gray-50 p-3 rounded-md">
              <div className="text-sm text-gray-600">
                <div><strong>AppID:</strong> {app.appId}</div>
                <div><strong>密钥:</strong> {app.secretKey}</div>
                <div><strong>创建时间:</strong> {new Date(app.createdAt).toLocaleString('zh-CN')}</div>
                <div><strong>创建者:</strong> {app.creator}</div>
              </div>
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
              <Button type="submit" disabled={isSubmitting}>
                {isSubmitting ? '保存中...' : '保存更改'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}