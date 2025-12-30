// 索引管理页面
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
  Database, 
  Plus, 
  Edit, 
  Trash2, 
  Search, 
  Filter, 
  Download,
  Upload,
  RefreshCw,
  GitBranch,
  CheckCircle,
  XCircle,
  Clock,
  Layers,
  BarChart3,
  Settings,
  Eye,
  EyeOff,
  Zap,
  AlertTriangle
} from 'lucide-react';

interface Index {
  id: string;
  name: string;
  status: 'active' | 'inactive' | 'rebuilding' | 'maintenance' | 'error';
  type: 'hotel' | 'search' | 'log' | 'analytics';
  version: string;
  size: string;
  documentCount: number;
  shardCount: number;
  replicaCount: number;
  health: 'green' | 'yellow' | 'red';
  primaryShards: number;
  totalShards: number;
  openShards: number;
  unassignedShards: number;
  createdAt: string;
  lastWriteAt?: string;
  lastOptimizedAt?: string;
  config: {
    maxResultWindow: number;
    numberOfShards: number;
    numberOfReplicas: number;
    refreshInterval: string;
    maxDocValueFields: number;
  };
  aliases: string[];
  mappings: {
    totalFields: number;
    size: string;
    lastUpdated: string;
  };
}

interface IndexStats {
  totalIndexes: number;
  activeIndexes: number;
  totalDocuments: number;
  totalSize: string;
  avgDocumentsPerIndex: number;
  avgSizePerIndex: string;
}

// 模拟索引数据
const mockIndexes: Index[] = [
  {
    id: '1',
    name: 'hotels_prod_v2',
    status: 'active',
    type: 'hotel',
    version: 'v2.3.1',
    size: '45.7 GB',
    documentCount: 3600000,
    shardCount: 5,
    replicaCount: 1,
    health: 'green',
    primaryShards: 5,
    totalShards: 10,
    openShards: 10,
    unassignedShards: 0,
    createdAt: '2025-11-10 14:30:00',
    lastWriteAt: '2025-11-18 03:25:00',
    lastOptimizedAt: '2025-11-17 22:15:00',
    config: {
      maxResultWindow: 10000,
      numberOfShards: 5,
      numberOfReplicas: 1,
      refreshInterval: '1s',
      maxDocValueFields: 1000,
    },
    aliases: ['hotels_current', 'search_primary'],
    mappings: {
      totalFields: 67,
      size: '2.3 MB',
      lastUpdated: '2025-11-18 02:15:00',
    },
  },
  {
    id: '2',
    name: 'hotels_staging_v2',
    status: 'maintenance',
    type: 'hotel',
    version: 'v2.3.1',
    size: '12.3 GB',
    documentCount: 1200000,
    shardCount: 3,
    replicaCount: 1,
    health: 'yellow',
    primaryShards: 3,
    totalShards: 6,
    openShards: 6,
    unassignedShards: 0,
    createdAt: '2025-11-12 09:20:00',
    lastWriteAt: '2025-11-18 01:45:00',
    config: {
      maxResultWindow: 5000,
      numberOfShards: 3,
      numberOfReplicas: 1,
      refreshInterval: '5s',
      maxDocValueFields: 500,
    },
    aliases: ['hotels_testing'],
    mappings: {
      totalFields: 65,
      size: '1.8 MB',
      lastUpdated: '2025-11-12 09:20:00',
    },
  },
  {
    id: '3',
    name: 'search_logs_2025_11',
    status: 'active',
    type: 'log',
    version: 'v1.0.5',
    size: '8.9 GB',
    documentCount: 2456789,
    shardCount: 2,
    replicaCount: 1,
    health: 'green',
    primaryShards: 2,
    totalShards: 4,
    openShards: 4,
    unassignedShards: 0,
    createdAt: '2025-11-01 00:00:00',
    lastWriteAt: '2025-11-18 03:24:00',
    lastOptimizedAt: '2025-11-17 23:00:00',
    config: {
      maxResultWindow: 1000,
      numberOfShards: 2,
      numberOfReplicas: 1,
      refreshInterval: '30s',
      maxDocValueFields: 100,
    },
    aliases: ['search_logs_current'],
    mappings: {
      totalFields: 23,
      size: '0.8 MB',
      lastUpdated: '2025-11-01 00:00:00',
    },
  },
  {
    id: '4',
    name: 'analytics_data_2025',
    status: 'rebuilding',
    type: 'analytics',
    version: 'v1.2.0',
    size: '0 GB',
    documentCount: 0,
    shardCount: 3,
    replicaCount: 1,
    health: 'red',
    primaryShards: 3,
    totalShards: 6,
    openShards: 6,
    unassignedShards: 0,
    createdAt: '2025-11-18 02:30:00',
    config: {
      maxResultWindow: 5000,
      numberOfShards: 3,
      numberOfReplicas: 1,
      refreshInterval: '5s',
      maxDocValueFields: 500,
    },
    aliases: [],
    mappings: {
      totalFields: 0,
      size: '0 KB',
      lastUpdated: '2025-11-18 02:30:00',
    },
  },
  {
    id: '5',
    name: 'search_suggestions_v1',
    status: 'active',
    type: 'search',
    version: 'v1.1.2',
    size: '234 MB',
    documentCount: 567890,
    shardCount: 2,
    replicaCount: 1,
    health: 'green',
    primaryShards: 2,
    totalShards: 4,
    openShards: 4,
    unassignedShards: 0,
    createdAt: '2025-11-08 16:45:00',
    lastWriteAt: '2025-11-18 03:20:00',
    lastOptimizedAt: '2025-11-16 18:30:00',
    config: {
      maxResultWindow: 1000,
      numberOfShards: 2,
      numberOfReplicas: 1,
      refreshInterval: '1s',
      maxDocValueFields: 200,
    },
    aliases: ['suggestions_current'],
    mappings: {
      totalFields: 15,
      size: '156 KB',
      lastUpdated: '2025-11-08 16:45:00',
    },
  },
];

const mockIndexStats: IndexStats = {
  totalIndexes: 12,
  activeIndexes: 8,
  totalDocuments: 6824679,
  totalSize: '67.2 GB',
  avgDocumentsPerIndex: 568723,
  avgSizePerIndex: '5.6 GB',
};

const getStatusColor = (status: string) => {
  switch (status) {
    case 'active':
      return 'bg-green-100 text-green-800';
    case 'inactive':
      return 'bg-gray-100 text-gray-800';
    case 'rebuilding':
      return 'bg-blue-100 text-blue-800';
    case 'maintenance':
      return 'bg-yellow-100 text-yellow-800';
    case 'error':
      return 'bg-red-100 text-red-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getStatusName = (status: string) => {
  switch (status) {
    case 'active':
      return '活跃';
    case 'inactive':
      return '停用';
    case 'rebuilding':
      return '重建中';
    case 'maintenance':
      return '维护中';
    case 'error':
      return '错误';
    default:
      return status;
  }
};

const getTypeColor = (type: string) => {
  switch (type) {
    case 'hotel':
      return 'bg-purple-100 text-purple-800';
    case 'search':
      return 'bg-blue-100 text-blue-800';
    case 'log':
      return 'bg-green-100 text-green-800';
    case 'analytics':
      return 'bg-orange-100 text-orange-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getTypeName = (type: string) => {
  switch (type) {
    case 'hotel':
      return '酒店索引';
    case 'search':
      return '搜索索引';
    case 'log':
      return '日志索引';
    case 'analytics':
      return '分析索引';
    default:
      return type;
  }
};

const getHealthColor = (health: string) => {
  switch (health) {
    case 'green':
      return 'text-green-600';
    case 'yellow':
      return 'text-yellow-600';
    case 'red':
      return 'text-red-600';
    default:
      return 'text-muted-foreground';
  }
};

const getHealthIcon = (health: string) => {
  switch (health) {
    case 'green':
      return <CheckCircle className="h-4 w-4" />;
    case 'yellow':
      return <AlertTriangle className="h-4 w-4" />;
    case 'red':
      return <XCircle className="h-4 w-4" />;
    default:
      return <Clock className="h-4 w-4" />;
  }
};

function IndexesContent() {
  const [indexes] = useState<Index[]>(mockIndexes);
  const [indexStats] = useState<IndexStats>(mockIndexStats);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [healthFilter, setHealthFilter] = useState('all');
  const [selectedIndexes, setSelectedIndexes] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // 搜索和筛选逻辑
  const filteredIndexes = indexes.filter(index => {
    const matchesSearch = searchTerm === '' || 
      index.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      index.aliases.some(alias => alias.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesType = typeFilter === 'all' || index.type === typeFilter;
    const matchesStatus = statusFilter === 'all' || index.status === statusFilter;
    const matchesHealth = healthFilter === 'all' || index.health === healthFilter;
    return matchesSearch && matchesType && matchesStatus && matchesHealth;
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

  const handleHealthFilter = (value: string) => {
    setHealthFilter(value);
  };

  // 索引选择处理
  const handleIndexSelect = (indexId: string, checked: boolean) => {
    if (checked) {
      setSelectedIndexes(prev => [...prev, indexId]);
    } else {
      setSelectedIndexes(prev => prev.filter(id => id !== indexId));
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedIndexes(filteredIndexes.map(index => index.id));
    } else {
      setSelectedIndexes([]);
    }
  };

  // 创建索引
  const handleCreateIndex = async () => {
    setIsLoading(true);
    console.log('创建索引');
    // 模拟创建延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    setIsLoading(false);
  };

  // 重建索引
  const handleRebuildIndex = async (indexId: string) => {
    setIsLoading(true);
    console.log('重建索引:', indexId);
    // 模拟重建延迟
    await new Promise(resolve => setTimeout(resolve, 10000));
    setIsLoading(false);
  };

  // 优化索引
  const handleOptimizeIndex = async (indexId: string) => {
    setIsLoading(true);
    console.log('优化索引:', indexId);
    // 模拟优化延迟
    await new Promise(resolve => setTimeout(resolve, 5000));
    setIsLoading(false);
  };

  // 关闭索引
  const handleCloseIndex = async (indexId: string) => {
    setIsLoading(true);
    console.log('关闭索引:', indexId);
    // 模拟关闭延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 打开索引
  const handleOpenIndex = async (indexId: string) => {
    setIsLoading(true);
    console.log('打开索引:', indexId);
    // 模拟打开延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 切换索引别名
  const handleSwitchAlias = async (indexId: string) => {
    setIsLoading(true);
    console.log('切换索引别名:', indexId);
    // 模拟切换延迟
    await new Promise(resolve => setTimeout(resolve, 3000));
    setIsLoading(false);
  };

  // 删除索引
  const handleDeleteIndex = async (indexId: string) => {
    setIsLoading(true);
    console.log('删除索引:', indexId);
    // 模拟删除延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    setIsLoading(false);
  };

  // 批量重建索引
  const handleBatchRebuild = async () => {
    setIsLoading(true);
    console.log('批量重建索引:', selectedIndexes);
    // 模拟批量重建延迟
    await new Promise(resolve => setTimeout(resolve, 15000));
    setIsLoading(false);
    setSelectedIndexes([]);
  };

  // 查看映射详情
  const handleViewMappings = (indexId: string) => {
    console.log('查看映射详情:', indexId);
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 页面头部操作区域 */}
      <div className="bg-card border-b">
        <div className="container px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-card-foreground">索引管理</h1>
              <p className="text-sm text-muted-foreground">
                管理Elasticsearch索引，支持索引重建、别名切换和性能优化
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <Button
                variant="outline"
                onClick={handleBatchRebuild}
                disabled={isLoading || selectedIndexes.length === 0}
                className="flex items-center space-x-2"
              >
                <Zap className="h-4 w-4" />
                <span>批量重建 ({selectedIndexes.length})</span>
              </Button>
              <Button
                onClick={handleCreateIndex}
                disabled={isLoading}
                className="flex items-center space-x-2"
              >
                <Plus className="h-4 w-4" />
                <span>创建索引</span>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6 sm:px-6 lg:px-8">
        {/* 索引统计概览 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总索引数</CardTitle>
              <Database className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{indexStats.totalIndexes}</div>
              <p className="text-xs text-muted-foreground">
                活跃: {indexStats.activeIndexes} | 非活跃: {indexStats.totalIndexes - indexStats.activeIndexes}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总文档数</CardTitle>
              <Layers className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{(indexStats.totalDocuments / 1000000).toFixed(1)}M</div>
              <p className="text-xs text-muted-foreground">
                平均: {indexStats.avgDocumentsPerIndex.toLocaleString()} 每索引
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总存储大小</CardTitle>
              <BarChart3 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{indexStats.totalSize}</div>
              <p className="text-xs text-muted-foreground">
                平均: {indexStats.avgSizePerIndex} 每索引
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">健康状态</CardTitle>
              <CheckCircle className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="flex space-x-1">
                <div className="flex items-center space-x-1 text-green-600">
                  <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                  <span className="text-sm">
                    {indexes.filter(idx => idx.health === 'green').length}
                  </span>
                </div>
                <div className="flex items-center space-x-1 text-yellow-600">
                  <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                  <span className="text-sm">
                    {indexes.filter(idx => idx.health === 'yellow').length}
                  </span>
                </div>
                <div className="flex items-center space-x-1 text-red-600">
                  <div className="w-2 h-2 bg-red-500 rounded-full"></div>
                  <span className="text-sm">
                    {indexes.filter(idx => idx.health === 'red').length}
                  </span>
                </div>
              </div>
              <p className="text-xs text-muted-foreground mt-2">
                健康状态分布
              </p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Search className="h-5 w-5" />
              <span>索引搜索和筛选</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <Input
                placeholder="搜索索引名称或别名..."
                value={searchTerm}
                onChange={(e) => handleSearch(e.target.value)}
              />
              <Select value={typeFilter} onValueChange={handleTypeFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="索引类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有类型</SelectItem>
                  <SelectItem value="hotel">酒店索引</SelectItem>
                  <SelectItem value="search">搜索索引</SelectItem>
                  <SelectItem value="log">日志索引</SelectItem>
                  <SelectItem value="analytics">分析索引</SelectItem>
                </SelectContent>
              </Select>
              <Select value={statusFilter} onValueChange={handleStatusFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="索引状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有状态</SelectItem>
                  <SelectItem value="active">活跃</SelectItem>
                  <SelectItem value="inactive">停用</SelectItem>
                  <SelectItem value="rebuilding">重建中</SelectItem>
                  <SelectItem value="maintenance">维护中</SelectItem>
                  <SelectItem value="error">错误</SelectItem>
                </SelectContent>
              </Select>
              <Select value={healthFilter} onValueChange={handleHealthFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="健康状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有健康状态</SelectItem>
                  <SelectItem value="green">健康</SelectItem>
                  <SelectItem value="yellow">警告</SelectItem>
                  <SelectItem value="red">错误</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 索引列表 */}
        <Card>
          <CardHeader>
            <CardTitle>索引列表</CardTitle>
            <p className="text-sm text-muted-foreground">
              共 {filteredIndexes.length} 个索引
              {selectedIndexes.length > 0 && ` (已选择 ${selectedIndexes.length} 个)`}
              {searchTerm && ` (搜索: "${searchTerm}")`}
              {typeFilter !== 'all' && ` (类型: ${getTypeName(typeFilter)})`}
              {statusFilter !== 'all' && ` (状态: ${getStatusName(statusFilter)})`}
              {healthFilter !== 'all' && ` (健康: ${healthFilter})`}
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
                        checked={filteredIndexes.length > 0 && filteredIndexes.every(index => selectedIndexes.includes(index.id))}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        className="rounded"
                      />
                    </TableHead>
                    <TableHead>索引名称</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>健康</TableHead>
                    <TableHead>文档数</TableHead>
                    <TableHead>大小</TableHead>
                    <TableHead>分片</TableHead>
                    <TableHead>别名</TableHead>
                    <TableHead>最后写入</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredIndexes.map((index) => (
                    <TableRow key={index.id} className={selectedIndexes.includes(index.id) ? 'bg-blue-50' : ''}>
                      <TableCell>
                        <input
                          type="checkbox"
                          checked={selectedIndexes.includes(index.id)}
                          onChange={(e) => handleIndexSelect(index.id, e.target.checked)}
                          className="rounded"
                        />
                      </TableCell>
                      <TableCell>
                        <div>
                          <div className="font-medium font-mono">{index.name}</div>
                          <div className="text-sm text-gray-500">v{index.version}</div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge className={getTypeColor(index.type)}>
                          {getTypeName(index.type)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge className={getStatusColor(index.status)}>
                          {getStatusName(index.status)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className={`flex items-center space-x-1 ${getHealthColor(index.health)}`}>
                          {getHealthIcon(index.health)}
                          <span className="text-sm uppercase font-medium">{index.health}</span>
                        </div>
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {index.documentCount.toLocaleString()}
                      </TableCell>
                      <TableCell className="font-mono text-sm">{index.size}</TableCell>
                      <TableCell className="text-sm">
                        <div>
                          <div>{index.primaryShards} 主分片</div>
                          <div className="text-gray-500">
                            {index.openShards}/{index.totalShards} 开放
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        {index.aliases.length > 0 ? (
                          <div className="flex flex-wrap gap-1">
                            {index.aliases.slice(0, 2).map((alias, idx) => (
                              <Badge key={idx} variant="outline" className="text-xs">
                                {alias}
                              </Badge>
                            ))}
                            {index.aliases.length > 2 && (
                              <Badge variant="outline" className="text-xs">
                                +{index.aliases.length - 2}
                              </Badge>
                            )}
                          </div>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {index.lastWriteAt ? index.lastWriteAt : '-'}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleRebuildIndex(index.id)}
                            disabled={isLoading || index.status === 'rebuilding'}
                            title="重建索引"
                          >
                            <RefreshCw className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleOptimizeIndex(index.id)}
                            disabled={isLoading}
                            title="优化索引"
                          >
                            <Zap className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleViewMappings(index.id)}
                            title="查看映射"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleSwitchAlias(index.id)}
                            disabled={isLoading}
                            title="切换别名"
                          >
                            <GitBranch className="h-4 w-4" />
                          </Button>
                          {index.status === 'active' ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleCloseIndex(index.id)}
                              disabled={isLoading}
                              title="关闭索引"
                            >
                              <EyeOff className="h-4 w-4 text-orange-500" />
                            </Button>
                          ) : (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleOpenIndex(index.id)}
                              disabled={isLoading}
                              title="打开索引"
                            >
                              <Eye className="h-4 w-4 text-green-500" />
                            </Button>
                          )}
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button
                                variant="ghost"
                                size="sm"
                                disabled={isLoading}
                                title="删除索引"
                              >
                                <Trash2 className="h-4 w-4 text-red-500" />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>删除索引</AlertDialogTitle>
                                <AlertDialogDescription>
                                  确定要删除索引 "{index.name}" 吗？此操作不可撤销，将永久删除所有数据和配置。
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>取消</AlertDialogCancel>
                                <AlertDialogAction 
                                  onClick={() => handleDeleteIndex(index.id)}
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

export default function IndexesPage() {
  return (
    <ProtectedRoute>
      <Header />
      <IndexesContent />
    </ProtectedRoute>
  );
}