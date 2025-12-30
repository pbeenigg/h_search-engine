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
  id?: string;
  name: string;
  appId?: string;
  secretKey?: string;
  status: 'active' | 'inactive' | 'suspended';
  rateLimit: number;
  creator: string;
  createdAt?: string;
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

interface CreateAppDialogProps {
  onCreateApp: (appData: Omit<AppInfo, 'id' | 'createdAt' | 'appId' | 'secretKey'>) => Promise<void>;
}

export function CreateAppDialog({ onCreateApp }: CreateAppDialogProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const form = useForm<AppFormData>({
    resolver: zodResolver(appSchema),
    defaultValues: {
      name: '',
      description: '',
      status: 'inactive',
      rateLimit: 100,
    },
  });

  const handleSubmit = async (data: AppFormData) => {
    setIsSubmitting(true);
    try {
      await onCreateApp({
        ...data,
        creator: 'admin', // TODO: 从认证状态获取当前用户
      });
      
      form.reset();
      setIsOpen(false);
    } catch (error) {
      console.error('创建应用失败:', error);
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

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogTrigger asChild>
        <Button>创建应用</Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>创建新应用</DialogTitle>
          <DialogDescription>
            创建一个新的API应用，系统会自动生成AppId和密钥。
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
                    为应用起一个易于识别的名称
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
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
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
                    选择应用的初始状态
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
                    设置每分钟允许的API请求次数
                  </FormDescription>
                  <FormMessage />
                </FormItem>
              )}
            />

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
                {isSubmitting ? '创建中...' : '创建应用'}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}