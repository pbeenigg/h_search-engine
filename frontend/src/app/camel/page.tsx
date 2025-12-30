// Apache Camel 路由管理页面
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
  GitBranch, 
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
  Activity,
  BarChart3,
  Timer,
  RefreshCw,
  Settings,
  Eye,
  Copy,
  Code,
  Route,
  Zap,
  Target,
  Layers,
  TrendingUp,
  AlertCircle
} from 'lucide-react';

interface CamelRoute {
  id: string;
  name: string;
  description: string;
  routeId: string;
  status: 'running' | 'stopped' | 'starting' | 'stopping' | 'error';
  type: 'data-sync' | 'message-routing' | 'transformation' | 'aggregation' | 'split' | 'filter';
  source: string;
  target: string;
  processors: string[];
  lastMessageTime?: string;
  totalMessages: number;
  successfulMessages: number;
  failedMessages: number;
  averageProcessingTime: number;
  errorRate: number;
  createdAt: string;
  updatedAt: string;
  config: {
    enabled: boolean;
    maxConcurrent: number;
    timeout: number;
    retryAttempts: number;
    circuitBreaker: boolean;
  };
  endpoints: {
    input: string;
    output: string;
    error?: string;
  };
  statistics: {
    messagesPerMinute: number;
    successRate: number;
    lastHourMessages: number;
    errorsLastHour: number;
    uptime: string;
  };
}

interface RouteStats {
  totalRoutes: number;
  runningRoutes: number;
  stoppedRoutes: number;
  totalMessages: number;
  avgSuccessRate: number;
  avgProcessingTime: number;
  totalEndpoints: number;
}

// 模拟Camel路由数据
const mockCamelRoutes: CamelRoute[] = [
  {
    id: '1',
    name: '酒店数据同步路由',
    description: '从供应商API同步酒店数据到Elasticsearch',
    routeId: 'hotel-data-sync',
    status: 'running',
    type: 'data-sync',
    source: 'http://suppliers-api/hotels',
    target: 'elasticsearch://hotels',
    processors: ['unmarshal', 'validate', 'transform', 'marshal'],
    lastMessageTime: '2025-11-18 03:27:45',
    totalMessages: 45678,
    successfulMessages: 45432,
    failedMessages: 246,
    averageProcessingTime: 1250,
    errorRate: 0.54,
    createdAt: '2025-11-10 14:30:00',
    updatedAt: '2025-11-18 03:27:50',
    config: {
      enabled: true,
      maxConcurrent: 10,
      timeout: 30000,
      retryAttempts: 3,
      circuitBreaker: true,
    },
    endpoints: {
      input: 'http://suppliers-api/hotels?format=json',
      output: 'elasticsearch://hotels?operation=index',
      error: 'activemq://queue:hotel-sync-errors',
    },
    statistics: {
      messagesPerMinute: 125,
      successRate: 99.46,
      lastHourMessages: 7500,
      errorsLastHour: 8,
      uptime: '5天 12小时',
    },
  },
  {
    id: '2',
    name: '搜索请求路由',
    description: '处理用户搜索请求并返回匹配结果',
    routeId: 'search-request',
    status: 'running',
    type: 'message-routing',
    source: 'direct://search',
    target: 'elasticsearch://hotels/_search',
    processors: ['validate', 'transform', 'search', 'filter'],
    lastMessageTime: '2025-11-18 03:28:00',
    totalMessages: 234567,
    successfulMessages: 234123,
    failedMessages: 444,
    averageProcessingTime: 280,
    errorRate: 0.19,
    createdAt: '2025-11-08 09:15:00',
    updatedAt: '2025-11-18 03:28:05',
    config: {
      enabled: true,
      maxConcurrent: 50,
      timeout: 10000,
      retryAttempts: 2,
      circuitBreaker: true,
    },
    endpoints: {
      input: 'direct://search',
      output: 'elasticsearch://hotels/_search',
    },
    statistics: {
      messagesPerMinute: 1850,
      successRate: 99.81,
      lastHourMessages: 111000,
      errorsLastHour: 15,
      uptime: '10天 3小时',
    },
  },
  {
    id: '3',
    name: '数据清洗路由',
    description: '清洗和标准化原始酒店数据',
    routeId: 'data-cleanup',
    status: 'stopped',
    type: 'transformation',
    source: 'file://data/raw',
    target: 'file://data/clean',
    processors: ['unmarshal', 'filter', 'transform', 'marshal'],
    lastMessageTime: '2025-11-17 22:15:30',
    totalMessages: 12543,
    successfulMessages: 12398,
    failedMessages: 145,
    averageProcessingTime: 890,
    errorRate: 1.16,
    createdAt: '2025-11-12 16:45:00',
    updatedAt: '2025-11-17 22:20:15',
    config: {
      enabled: false,
      maxConcurrent: 5,
      timeout: 60000,
      retryAttempts: 2,
      circuitBreaker: false,
    },
    endpoints: {
      input: 'file://data/raw?noop=true',
      output: 'file://data/clean?fileName=${date:now:yyyyMMddHHmmss}.json',
    },
    statistics: {
      messagesPerMinute: 0,
      successRate: 98.84,
      lastHourMessages: 0,
      errorsLastHour: 0,
      uptime: '2天 8小时',
    },
  },
  {
    id: '4',
    name: '批量更新路由',
    description: '批量处理酒店信息更新请求',
    routeId: 'batch-update',
    status: 'starting',
    type: 'aggregation',
    source: 'activemq://queue:hotel-updates',
    target: 'elasticsearch://hotels/_bulk',
    processors: ['aggregate', 'unmarshal', 'validate', 'bulk-index'],
    totalMessages: 8934,
    successfulMessages: 8921,
    failedMessages: 13,
    averageProcessingTime: 2100,
    errorRate: 0.15,
    createdAt: '2025-11-14 11:20:00',
    updatedAt: '2025-11-18 03:25:30',
    config: {
      enabled: true,
      maxConcurrent: 8,
      timeout: 45000,
      retryAttempts: 3,
      circuitBreaker: true,
    },
    endpoints: {
      input: 'activemq://queue:hotel-updates',
      output: 'elasticsearch://hotels/_bulk',
      error: 'activemq://queue:update-errors',
    },
    statistics: {
      messagesPerMinute: 45,
      successRate: 99.85,
      lastHourMessages: 2700,
      errorsLastHour: 2,
      uptime: '4天 1小时',
    },
  },
  {
    id: '5',
    name: '日志处理路由',
    description: '处理系统日志并存储到Elasticsearch',
    routeId: 'log-processing',
    status: 'error',
    type: 'filter',
    source: 'file://logs/app',
    target: 'elasticsearch://logs',
    processors: ['unmarshal', 'filter', 'enrich', 'marshal'],
    lastMessageTime: '2025-11-18 02:45:20',
    totalMessages: 567890,
    successfulMessages: 567234,
    failedMessages: 656,
    averageProcessingTime: 150,
    errorRate: 0.12,
    createdAt: '2025-11-05 13:30:00',
    updatedAt: '2025-11-18 03:20:15',
    config: {
      enabled: true,
      maxConcurrent: 20,
      timeout: 15000,
      retryAttempts: 1,
      circuitBreaker: false,
    },
    endpoints: {
      input: 'file://logs/app?noop=true',
      output: 'elasticsearch://logs?operation=index',
    },
    statistics: {
      messagesPerMinute: 850,
      successRate: 99.88,
      lastHourMessages: 51000,
      errorsLastHour: 25,
      uptime: '13天 6小时',
    },
  },
];

const mockRouteStats: RouteStats = {
  totalRoutes: 18,
  runningRoutes: 12,
  stoppedRoutes: 4,
  totalMessages: 884612,
  avgSuccessRate: 99.35,
  avgProcessingTime: 890,
  totalEndpoints: 35,
};

const getStatusColor = (status: string) => {
  switch (status) {
    case 'running':
      return 'bg-green-100 text-green-800';
    case 'stopped':
      return 'bg-gray-100 text-gray-800';
    case 'starting':
      return 'bg-blue-100 text-blue-800';
    case 'stopping':
      return 'bg-yellow-100 text-yellow-800';
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
    case 'stopped':
      return '已停止';
    case 'starting':
      return '启动中';
    case 'stopping':
      return '停止中';
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
    case 'stopped':
      return <Square className="h-4 w-4" />;
    case 'starting':
      return <RefreshCw className="h-4 w-4 animate-spin" />;
    case 'stopping':
      return <Pause className="h-4 w-4" />;
    case 'error':
      return <XCircle className="h-4 w-4" />;
    default:
      return <Activity className="h-4 w-4" />;
  }
};

const getTypeColor = (type: string) => {
  switch (type) {
    case 'data-sync':
      return 'bg-blue-100 text-blue-800';
    case 'message-routing':
      return 'bg-green-100 text-green-800';
    case 'transformation':
      return 'bg-purple-100 text-purple-800';
    case 'aggregation':
      return 'bg-orange-100 text-orange-800';
    case 'split':
      return 'bg-indigo-100 text-indigo-800';
    case 'filter':
      return 'bg-red-100 text-red-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getTypeName = (type: string) => {
  switch (type) {
    case 'data-sync':
      return '数据同步';
    case 'message-routing':
      return '消息路由';
    case 'transformation':
      return '数据转换';
    case 'aggregation':
      return '数据聚合';
    case 'split':
      return '数据拆分';
    case 'filter':
      return '数据过滤';
    default:
      return type;
  }
};

function CamelContent() {
  const [camelRoutes] = useState<CamelRoute[]>(mockCamelRoutes);
  const [routeStats] = useState<RouteStats>(mockRouteStats);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [selectedRoutes, setSelectedRoutes] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // 搜索和筛选逻辑
  const filteredRoutes = camelRoutes.filter(route => {
    const matchesSearch = searchTerm === '' || 
      route.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      route.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
      route.routeId.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesType = typeFilter === 'all' || route.type === typeFilter;
    const matchesStatus = statusFilter === 'all' || route.status === statusFilter;
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

  // 路由选择处理
  const handleRouteSelect = (routeId: string, checked: boolean) => {
    if (checked) {
      setSelectedRoutes(prev => [...prev, routeId]);
    } else {
      setSelectedRoutes(prev => prev.filter(id => id !== routeId));
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedRoutes(filteredRoutes.map(route => route.id));
    } else {
      setSelectedRoutes([]);
    }
  };

  // 创建路由
  const handleCreateRoute = async () => {
    setIsLoading(true);
    console.log('创建Camel路由');
    // 模拟创建延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 启动路由
  const handleStartRoute = async (routeId: string) => {
    setIsLoading(true);
    console.log('启动路由:', routeId);
    // 模拟启动延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    setIsLoading(false);
  };

  // 停止路由
  const handleStopRoute = async (routeId: string) => {
    setIsLoading(true);
    console.log('停止路由:', routeId);
    // 模拟停止延迟
    await new Promise(resolve => setTimeout(resolve, 1500));
    setIsLoading(false);
  };

  // 重启路由
  const handleRestartRoute = async (routeId: string) => {
    setIsLoading(true);
    console.log('重启路由:', routeId);
    // 模拟重启延迟
    await new Promise(resolve => setTimeout(resolve, 3000));
    setIsLoading(false);
  };

  // 删除路由
  const handleDeleteRoute = async (routeId: string) => {
    setIsLoading(true);
    console.log('删除路由:', routeId);
    // 模拟删除延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 批量启动
  const handleBatchStart = async () => {
    setIsLoading(true);
    console.log('批量启动路由:', selectedRoutes);
    // 模拟批量启动延迟
    await new Promise(resolve => setTimeout(resolve, 5000));
    setIsLoading(false);
    setSelectedRoutes([]);
  };

  // 批量停止
  const handleBatchStop = async () => {
    setIsLoading(true);
    console.log('批量停止路由:', selectedRoutes);
    // 模拟批量停止延迟
    await new Promise(resolve => setTimeout(resolve, 3000));
    setIsLoading(false);
    setSelectedRoutes([]);
  };

  // 查看路由详情
  const handleViewRouteDetails = (routeId: string) => {
    console.log('查看路由详情:', routeId);
  };

  // 查看执行链路
  const handleViewExecutionTrace = (routeId: string) => {
    console.log('查看执行链路:', routeId);
  };

  // 复制路由ID
  const handleCopyRouteId = (routeId: string) => {
    navigator.clipboard.writeText(routeId);
    console.log('已复制路由ID:', routeId);
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 页面头部操作区域 */}
      <div className="bg-card border-b">
        <div className="container px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-card-foreground">Camel路由管理</h1>
              <p className="text-sm text-muted-foreground">
                管理Apache Camel路由，监控执行链路和性能指标
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <Button
                variant="outline"
                onClick={handleBatchStart}
                disabled={isLoading || selectedRoutes.length === 0}
                className="flex items-center space-x-2"
              >
                <Play className="h-4 w-4" />
                <span>批量启动 ({selectedRoutes.length})</span>
              </Button>
              <Button
                variant="outline"
                onClick={handleBatchStop}
                disabled={isLoading || selectedRoutes.length === 0}
                className="flex items-center space-x-2"
              >
                <Pause className="h-4 w-4" />
                <span>批量停止 ({selectedRoutes.length})</span>
              </Button>
              <Button
                onClick={handleCreateRoute}
                disabled={isLoading}
                className="flex items-center space-x-2"
              >
                <Plus className="h-4 w-4" />
                <span>创建路由</span>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6 sm:px-6 lg:px-8">
        {/* 路由统计概览 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总路由数</CardTitle>
              <GitBranch className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{routeStats.totalRoutes}</div>
              <p className="text-xs text-muted-foreground">
                运行: {routeStats.runningRoutes} | 停止: {routeStats.stoppedRoutes}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总消息数</CardTitle>
              <Target className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{(routeStats.totalMessages / 1000).toFixed(0)}K</div>
              <p className="text-xs text-muted-foreground">
                累计处理消息数量
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">平均成功率</CardTitle>
              <BarChart3 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{routeStats.avgSuccessRate}%</div>
              <p className="text-xs text-muted-foreground">
                {routeStats.avgSuccessRate > 99 ? '优秀' : routeStats.avgSuccessRate > 95 ? '良好' : '需要改进'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">平均处理时间</CardTitle>
              <Timer className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{routeStats.avgProcessingTime}ms</div>
              <p className="text-xs text-muted-foreground">
                消息平均处理耗时
              </p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Search className="h-5 w-5" />
              <span>路由搜索和筛选</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Input
                placeholder="搜索路由名称、描述或ID..."
                value={searchTerm}
                onChange={(e) => handleSearch(e.target.value)}
              />
              <Select value={typeFilter} onValueChange={handleTypeFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="路由类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有类型</SelectItem>
                  <SelectItem value="data-sync">数据同步</SelectItem>
                  <SelectItem value="message-routing">消息路由</SelectItem>
                  <SelectItem value="transformation">数据转换</SelectItem>
                  <SelectItem value="aggregation">数据聚合</SelectItem>
                  <SelectItem value="split">数据拆分</SelectItem>
                  <SelectItem value="filter">数据过滤</SelectItem>
                </SelectContent>
              </Select>
              <Select value={statusFilter} onValueChange={handleStatusFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="路由状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有状态</SelectItem>
                  <SelectItem value="running">运行中</SelectItem>
                  <SelectItem value="stopped">已停止</SelectItem>
                  <SelectItem value="starting">启动中</SelectItem>
                  <SelectItem value="stopping">停止中</SelectItem>
                  <SelectItem value="error">错误</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 路由列表 */}
        <Card>
          <CardHeader>
            <CardTitle>路由列表</CardTitle>
            <p className="text-sm text-muted-foreground">
              共 {filteredRoutes.length} 个路由
              {selectedRoutes.length > 0 && ` (已选择 ${selectedRoutes.length} 个)`}
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
                        checked={filteredRoutes.length > 0 && filteredRoutes.every(route => selectedRoutes.includes(route.id))}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        className="rounded"
                      />
                    </TableHead>
                    <TableHead>路由名称</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>路由ID</TableHead>
                    <TableHead>消息数</TableHead>
                    <TableHead>成功率</TableHead>
                    <TableHead>处理时间</TableHead>
                    <TableHead>最后消息</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredRoutes.map((route) => (
                    <TableRow key={route.id} className={selectedRoutes.includes(route.id) ? 'bg-blue-50' : ''}>
                      <TableCell>
                        <input
                          type="checkbox"
                          checked={selectedRoutes.includes(route.id)}
                          onChange={(e) => handleRouteSelect(route.id, e.target.checked)}
                          className="rounded"
                        />
                      </TableCell>
                      <TableCell>
                        <div>
                          <div className="font-medium">{route.name}</div>
                          <div className="text-sm text-gray-500 truncate max-w-xs" title={route.description}>
                            {route.description}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge className={getTypeColor(route.type)}>
                          {getTypeName(route.type)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge className={getStatusColor(route.status)}>
                          <span className="flex items-center space-x-1">
                            {getStatusIcon(route.status)}
                            <span>{getStatusName(route.status)}</span>
                          </span>
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <code className="text-sm bg-gray-100 px-2 py-1 rounded">
                            {route.routeId}
                          </code>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleCopyRouteId(route.routeId)}
                            title="复制路由ID"
                          >
                            <Copy className="h-3 w-3" />
                          </Button>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="text-sm">
                          <div>{route.totalMessages.toLocaleString()}</div>
                          <div className="text-xs text-gray-500">
                            成功: {route.successfulMessages.toLocaleString()}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <div className="text-sm font-medium">
                            {route.statistics.successRate}%
                          </div>
                          {route.errorRate > 1 && (
                            <AlertTriangle className="h-4 w-4 text-yellow-500" />
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="text-sm">
                          <div>{route.averageProcessingTime}ms</div>
                          <div className="text-xs text-gray-500">
                            平均耗时
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {route.lastMessageTime || '-'}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          {route.status === 'running' ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleStopRoute(route.id)}
                              disabled={isLoading}
                              title="停止路由"
                            >
                              <Pause className="h-4 w-4" />
                            </Button>
                          ) : (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleStartRoute(route.id)}
                              disabled={isLoading}
                              title="启动路由"
                            >
                              <Play className="h-4 w-4" />
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleRestartRoute(route.id)}
                            disabled={isLoading}
                            title="重启路由"
                          >
                            <RefreshCw className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleViewExecutionTrace(route.id)}
                            title="执行链路追踪"
                          >
                            <Layers className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleViewRouteDetails(route.id)}
                            title="查看详情"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            title="编辑路由"
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button
                                variant="ghost"
                                size="sm"
                                disabled={isLoading}
                                title="删除路由"
                              >
                                <Trash2 className="h-4 w-4 text-red-500" />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>删除Camel路由</AlertDialogTitle>
                                <AlertDialogDescription>
                                  确定要删除Camel路由 "{route.name}" 吗？此操作不可撤销，将永久删除所有相关配置。
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>取消</AlertDialogCancel>
                                <AlertDialogAction 
                                  onClick={() => handleDeleteRoute(route.id)}
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

export default function CamelPage() {
  return (
    <ProtectedRoute>
      <Header />
      <CamelContent />
    </ProtectedRoute>
  );
}