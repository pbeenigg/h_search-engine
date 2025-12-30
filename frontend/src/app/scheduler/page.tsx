// 定时任务管理页面
'use client';

import { useState } from 'react';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { Header } from '@/components/layout/Header';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Textarea } from '@/components/ui/textarea';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from '@/components/ui/alert-dialog';
import { 
  Clock, 
  Plus, 
  Edit, 
  Trash2, 
  Search, 
  Filter, 
  Play,
  Pause,
  Square,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Calendar,
  Timer,
  Activity,
  BarChart3,
  LogOut,
  RefreshCw,
  Settings,
  Eye,
  Copy
} from 'lucide-react';

interface ScheduledJob {
  id: string;
  name: string;
  description: string;
  cronExpression: string;
  status: 'running' | 'paused' | 'stopped' | 'error';
  type: 'data-sync' | 'cache-clear' | 'index-rebuild' | 'report-generate' | 'backup' | 'cleanup';
  nextExecution?: string;
  lastExecution?: string;
  lastStatus: 'success' | 'failed' | 'cancelled';
  executionCount: number;
  successCount: number;
  failureCount: number;
  averageDuration: number;
  createdAt: string;
  updatedAt: string;
  config: {
    enabled: boolean;
    maxRetries: number;
    timeout: number;
    parameters: Record<string, unknown>;
  };
  dependencies?: string[];
}

interface JobStats {
  totalJobs: number;
  runningJobs: number;
  pausedJobs: number;
  failedJobs: number;
  totalExecutions: number;
  avgSuccessRate: number;
  avgExecutionTime: number;
}

// 模拟定时任务数据
const mockScheduledJobs: ScheduledJob[] = [
  {
    id: '1',
    name: '酒店数据同步',
    description: '每日凌晨2点同步最新酒店数据',
    cronExpression: '0 2 * * *',
    status: 'running',
    type: 'data-sync',
    nextExecution: '2025-11-19 02:00:00',
    lastExecution: '2025-11-18 02:00:12',
    lastStatus: 'success',
    executionCount: 127,
    successCount: 125,
    failureCount: 2,
    averageDuration: 2850,
    createdAt: '2025-11-01 10:30:00',
    updatedAt: '2025-11-18 02:00:15',
    config: {
      enabled: true,
      maxRetries: 3,
      timeout: 3600,
      parameters: {
        source: 'suppliers',
        batchSize: 1000,
        enableIncremental: true
      }
    },
    dependencies: ['供应商健康检查'],
  },
  {
    id: '2',
    name: '缓存清理任务',
    description: '每小时清理过期缓存数据',
    cronExpression: '0 * * * *',
    status: 'running',
    type: 'cache-clear',
    nextExecution: '2025-11-18 04:00:00',
    lastExecution: '2025-11-18 03:00:05',
    lastStatus: 'success',
    executionCount: 456,
    successCount: 454,
    failureCount: 2,
    averageDuration: 120,
    createdAt: '2025-11-05 14:20:00',
    updatedAt: '2025-11-18 03:00:08',
    config: {
      enabled: true,
      maxRetries: 2,
      timeout: 300,
      parameters: {
        cacheTypes: ['search', 'hotel', 'user'],
        expireThreshold: 3600
      }
    },
  },
  {
    id: '3',
    name: '索引重建任务',
    description: '每周日凌晨3点重建搜索索引',
    cronExpression: '0 3 * * 0',
    status: 'paused',
    type: 'index-rebuild',
    nextExecution: '2025-11-24 03:00:00',
    lastExecution: '2025-11-17 03:15:42',
    lastStatus: 'failed',
    executionCount: 12,
    successCount: 10,
    failureCount: 2,
    averageDuration: 4200,
    createdAt: '2025-11-10 09:45:00',
    updatedAt: '2025-11-17 03:25:18',
    config: {
      enabled: false,
      maxRetries: 2,
      timeout: 7200,
      parameters: {
        indexes: ['hotels_prod_v2'],
        rebuildStrategy: 'full'
      }
    },
  },
  {
    id: '4',
    name: '日报生成任务',
    description: '每日上午8点生成运营日报',
    cronExpression: '0 8 * * *',
    status: 'running',
    type: 'report-generate',
    nextExecution: '2025-11-19 08:00:00',
    lastExecution: '2025-11-18 08:00:45',
    lastStatus: 'success',
    executionCount: 45,
    successCount: 43,
    failureCount: 2,
    averageDuration: 180,
    createdAt: '2025-11-08 16:30:00',
    updatedAt: '2025-11-18 08:00:52',
    config: {
      enabled: true,
      maxRetries: 2,
      timeout: 600,
      parameters: {
        reportType: 'daily',
        recipients: ['ops@heytrip.com'],
        includeMetrics: ['search', 'user', 'revenue']
      }
    },
  },
  {
    id: '5',
    name: '数据备份任务',
    description: '每日凌晨1点执行数据备份',
    cronExpression: '0 1 * * *',
    status: 'stopped',
    type: 'backup',
    nextExecution: '-',
    lastExecution: '2025-11-17 01:00:30',
    lastStatus: 'cancelled',
    executionCount: 30,
    successCount: 28,
    failureCount: 2,
    averageDuration: 1200,
    createdAt: '2025-11-01 11:15:00',
    updatedAt: '2025-11-17 01:05:12',
    config: {
      enabled: false,
      maxRetries: 1,
      timeout: 1800,
      parameters: {
        backupType: 'full',
        retentionDays: 30,
        compress: true
      }
    },
  },
  {
    id: '6',
    name: '日志清理任务',
    description: '每周一凌晨4点清理旧日志',
    cronExpression: '0 4 * * 1',
    status: 'error',
    type: 'cleanup',
    nextExecution: '2025-11-25 04:00:00',
    lastExecution: '2025-11-11 04:00:15',
    lastStatus: 'failed',
    executionCount: 18,
    successCount: 15,
    failureCount: 3,
    averageDuration: 890,
    createdAt: '2025-11-03 13:20:00',
    updatedAt: '2025-11-11 04:10:30',
    config: {
      enabled: true,
      maxRetries: 3,
      timeout: 1800,
      parameters: {
        logTypes: ['access', 'error', 'audit'],
        retentionDays: 90,
        archiveBeforeDelete: true
      }
    },
  },
];

const mockJobStats: JobStats = {
  totalJobs: 15,
  runningJobs: 8,
  pausedJobs: 4,
  failedJobs: 3,
  totalExecutions: 688,
  avgSuccessRate: 96.8,
  avgExecutionTime: 1560,
};

const getStatusColor = (status: string) => {
  switch (status) {
    case 'running':
      return 'bg-green-100 text-green-800';
    case 'paused':
      return 'bg-yellow-100 text-yellow-800';
    case 'stopped':
      return 'bg-gray-100 text-gray-800';
    case 'error':
      return 'bg-red-100 text-red-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getStatusName = (status: string) => {
  switch (status) {
    case 'running':
      return '运行中';
    case 'paused':
      return '已暂停';
    case 'stopped':
      return '已停止';
    case 'error':
      return '错误';
    default:
      return status;
  }
};

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'running':
      return <Play className="h-4 w-4" />;
    case 'paused':
      return <Pause className="h-4 w-4" />;
    case 'stopped':
      return <Square className="h-4 w-4" />;
    case 'error':
      return <XCircle className="h-4 w-4" />;
    default:
      return <Clock className="h-4 w-4" />;
  }
};

const getTypeColor = (type: string) => {
  switch (type) {
    case 'data-sync':
      return 'bg-blue-100 text-blue-800';
    case 'cache-clear':
      return 'bg-green-100 text-green-800';
    case 'index-rebuild':
      return 'bg-purple-100 text-purple-800';
    case 'report-generate':
      return 'bg-orange-100 text-orange-800';
    case 'backup':
      return 'bg-indigo-100 text-indigo-800';
    case 'cleanup':
      return 'bg-red-100 text-red-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getTypeName = (type: string) => {
  switch (type) {
    case 'data-sync':
      return '数据同步';
    case 'cache-clear':
      return '缓存清理';
    case 'index-rebuild':
      return '索引重建';
    case 'report-generate':
      return '报告生成';
    case 'backup':
      return '数据备份';
    case 'cleanup':
      return '清理任务';
    default:
      return type;
  }
};

const getExecutionStatusColor = (status: string) => {
  switch (status) {
    case 'success':
      return 'text-green-600';
    case 'failed':
      return 'text-red-600';
    case 'cancelled':
      return 'text-muted-foreground';
    default:
      return 'text-muted-foreground';
  }
};

const getExecutionStatusIcon = (status: string) => {
  switch (status) {
    case 'success':
      return <CheckCircle className="h-4 w-4" />;
    case 'failed':
      return <XCircle className="h-4 w-4" />;
    case 'cancelled':
      return <Square className="h-4 w-4" />;
    default:
      return <Clock className="h-4 w-4" />;
  }
};

function SchedulerContent() {
  const [scheduledJobs] = useState<ScheduledJob[]>(mockScheduledJobs);
  const [jobStats] = useState<JobStats>(mockJobStats);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedJobs, setSelectedJobs] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // 搜索和筛选逻辑
  const filteredJobs = scheduledJobs.filter(job => {
    const matchesSearch = searchTerm === '' || 
      job.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      job.description.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesType = typeFilter === 'all' || job.type === typeFilter;
    const matchesStatus = statusFilter === 'all' || job.status === statusFilter;
    return matchesSearch && matchesType && matchesStatus;
  });

  // 搜索处理
  const handleSearch = (value: string) => {
    setSearchTerm(value);
  };

  // 筛选处理
  const handleTypeFilter = (value: string) => {
    setTypeFilter(value);
  };

  const handleStatusFilter = (value: string) => {
    setStatusFilter(value);
  };

  // 任务选择处理
  const handleJobSelect = (jobId: string, checked: boolean) => {
    if (checked) {
      setSelectedJobs(prev => [...prev, jobId]);
    } else {
      setSelectedJobs(prev => prev.filter(id => id !== jobId));
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedJobs(filteredJobs.map(job => job.id));
    } else {
      setSelectedJobs([]);
    }
  };

  // 创建任务
  const handleCreateJob = async () => {
    setIsLoading(true);
    console.log('创建定时任务');
    // 模拟创建延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 启动任务
  const handleStartJob = async (jobId: string) => {
    setIsLoading(true);
    console.log('启动任务:', jobId);
    // 模拟启动延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 暂停任务
  const handlePauseJob = async (jobId: string) => {
    setIsLoading(true);
    console.log('暂停任务:', jobId);
    // 模拟暂停延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 停止任务
  const handleStopJob = async (jobId: string) => {
    setIsLoading(true);
    console.log('停止任务:', jobId);
    // 模拟停止延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 手动执行任务
  const handleExecuteJob = async (jobId: string) => {
    setIsLoading(true);
    console.log('手动执行任务:', jobId);
    // 模拟执行延迟
    await new Promise(resolve => setTimeout(resolve, 3000));
    setIsLoading(false);
  };

  // 删除任务
  const handleDeleteJob = async (jobId: string) => {
    setIsLoading(true);
    console.log('删除任务:', jobId);
    // 模拟删除延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 批量启动
  const handleBatchStart = async () => {
    setIsLoading(true);
    console.log('批量启动任务:', selectedJobs);
    // 模拟批量启动延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    setIsLoading(false);
    setSelectedJobs([]);
  };

  // 批量暂停
  const handleBatchPause = async () => {
    setIsLoading(true);
    console.log('批量暂停任务:', selectedJobs);
    // 模拟批量暂停延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    setIsLoading(false);
    setSelectedJobs([]);
  };

  // 查看执行历史
  const handleViewExecutionHistory = (jobId: string) => {
    console.log('查看执行历史:', jobId);
  };

  // 复制Cron表达式
  const handleCopyCron = (cron: string) => {
    navigator.clipboard.writeText(cron);
    console.log('已复制Cron表达式:', cron);
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 页面头部操作区域 */}
      <div className="bg-card border-b">
        <div className="container px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-card-foreground">定时任务管理</h1>
              <p className="text-sm text-muted-foreground">
                管理系统定时任务，支持Cron表达式配置和任务调度
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <Button
                variant="outline"
                onClick={handleBatchStart}
                disabled={isLoading || selectedJobs.length === 0}
                className="flex items-center space-x-2"
              >
                <Play className="h-4 w-4" />
                <span>批量启动 ({selectedJobs.length})</span>
              </Button>
              <Button
                variant="outline"
                onClick={handleBatchPause}
                disabled={isLoading || selectedJobs.length === 0}
                className="flex items-center space-x-2"
              >
                <Pause className="h-4 w-4" />
                <span>批量暂停 ({selectedJobs.length})</span>
              </Button>
              <Button
                onClick={handleCreateJob}
                disabled={isLoading}
                className="flex items-center space-x-2"
              >
                <Plus className="h-4 w-4" />
                <span>创建任务</span>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6 sm:px-6 lg:px-8">
        {/* 任务统计概览 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总任务数</CardTitle>
              <Timer className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{jobStats.totalJobs}</div>
              <p className="text-xs text-muted-foreground">
                运行: {jobStats.runningJobs} | 暂停: {jobStats.pausedJobs}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总执行次数</CardTitle>
              <Activity className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{jobStats.totalExecutions}</div>
              <p className="text-xs text-muted-foreground">
                累计任务执行次数
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">平均成功率</CardTitle>
              <BarChart3 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{jobStats.avgSuccessRate}%</div>
              <p className="text-xs text-muted-foreground">
                {jobStats.avgSuccessRate > 95 ? '优秀' : jobStats.avgSuccessRate > 90 ? '良好' : '需要改进'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">平均执行时间</CardTitle>
              <Clock className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{Math.round(jobStats.avgExecutionTime / 1000)}s</div>
              <p className="text-xs text-muted-foreground">
                任务平均执行耗时
              </p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Search className="h-5 w-5" />
              <span>任务搜索和筛选</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Input
                placeholder="搜索任务名称或描述..."
                value={searchTerm}
                onChange={(e) => handleSearch(e.target.value)}
              />
              <Select value={typeFilter} onValueChange={handleTypeFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="任务类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有类型</SelectItem>
                  <SelectItem value="data-sync">数据同步</SelectItem>
                  <SelectItem value="cache-clear">缓存清理</SelectItem>
                  <SelectItem value="index-rebuild">索引重建</SelectItem>
                  <SelectItem value="report-generate">报告生成</SelectItem>
                  <SelectItem value="backup">数据备份</SelectItem>
                  <SelectItem value="cleanup">清理任务</SelectItem>
                </SelectContent>
              </Select>
              <Select value={statusFilter} onValueChange={handleStatusFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="任务状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有状态</SelectItem>
                  <SelectItem value="running">运行中</SelectItem>
                  <SelectItem value="paused">已暂停</SelectItem>
                  <SelectItem value="stopped">已停止</SelectItem>
                  <SelectItem value="error">错误</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 任务列表 */}
        <Card>
          <CardHeader>
            <CardTitle>任务列表</CardTitle>
            <p className="text-sm text-muted-foreground">
              共 {filteredJobs.length} 个任务
              {selectedJobs.length > 0 && ` (已选择 ${selectedJobs.length} 个)`}
              {searchTerm && ` (搜索: "${searchTerm}")`}
              {typeFilter !== 'all' && ` (类型: ${getTypeName(typeFilter)})`}
              {statusFilter !== 'all' && ` (状态: ${getStatusName(statusFilter)})`}
            </p>
          </CardHeader>
          <CardContent>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-12">
                      <input
                        type="checkbox"
                        checked={filteredJobs.length > 0 && filteredJobs.every(job => selectedJobs.includes(job.id))}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        className="rounded"
                      />
                    </TableHead>
                    <TableHead>任务名称</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>Cron表达式</TableHead>
                    <TableHead>下次执行</TableHead>
                    <TableHead>上次执行</TableHead>
                    <TableHead>成功率</TableHead>
                    <TableHead>执行次数</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredJobs.map((job) => (
                    <TableRow key={job.id} className={selectedJobs.includes(job.id) ? 'bg-blue-50' : ''}>
                      <TableCell>
                        <input
                          type="checkbox"
                          checked={selectedJobs.includes(job.id)}
                          onChange={(e) => handleJobSelect(job.id, e.target.checked)}
                          className="rounded"
                        />
                      </TableCell>
                      <TableCell>
                        <div>
                          <div className="font-medium">{job.name}</div>
                          <div className="text-sm text-gray-500 truncate max-w-xs" title={job.description}>
                            {job.description}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge className={getTypeColor(job.type)}>
                          {getTypeName(job.type)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <Badge className={getStatusColor(job.status)}>
                            <span className="flex items-center space-x-1">
                              {getStatusIcon(job.status)}
                              <span>{getStatusName(job.status)}</span>
                            </span>
                          </Badge>
                          <div className={`flex items-center space-x-1 ${getExecutionStatusColor(job.lastStatus)}`}>
                            {getExecutionStatusIcon(job.lastStatus)}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <code className="text-sm bg-gray-100 px-2 py-1 rounded">
                            {job.cronExpression}
                          </code>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleCopyCron(job.cronExpression)}
                            title="复制Cron表达式"
                          >
                            <Copy className="h-3 w-3" />
                          </Button>
                        </div>
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {job.nextExecution || '-'}
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {job.lastExecution || '-'}
                      </TableCell>
                      <TableCell>
                        <div className="text-sm">
                          <div>{Math.round((job.successCount / job.executionCount) * 100) || 0}%</div>
                          <div className="text-xs text-gray-500">
                            {job.successCount}/{job.executionCount}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {job.executionCount}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          {job.status === 'running' ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handlePauseJob(job.id)}
                              disabled={isLoading}
                              title="暂停任务"
                            >
                              <Pause className="h-4 w-4" />
                            </Button>
                          ) : (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleStartJob(job.id)}
                              disabled={isLoading}
                              title="启动任务"
                            >
                              <Play className="h-4 w-4" />
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleExecuteJob(job.id)}
                            disabled={isLoading}
                            title="手动执行"
                          >
                            <RefreshCw className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleViewExecutionHistory(job.id)}
                            title="执行历史"
                          >
                            <LogOut className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            title="编辑任务"
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button
                                variant="ghost"
                                size="sm"
                                disabled={isLoading}
                                title="删除任务"
                              >
                                <Trash2 className="h-4 w-4 text-red-500" />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>删除定时任务</AlertDialogTitle>
                                <AlertDialogDescription>
                                  确定要删除定时任务 "{job.name}" 吗？此操作不可撤销，将永久删除所有相关配置。
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>取消</AlertDialogCancel>
                                <AlertDialogAction 
                                  onClick={() => handleDeleteJob(job.id)}
                                  className="bg-red-600 hover:bg-red-700"
                                >
                                  确认删除
                                </AlertDialogAction>
                              </AlertDialogFooter>
                            </AlertDialogContent>
                          </AlertDialog>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      </main>
    </div>
  );
}

export default function SchedulerPage() {
  return (
    <ProtectedRoute>
      <Header />
      <SchedulerContent />
    </ProtectedRoute>
  );
}