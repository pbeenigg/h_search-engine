// 缓存管理页面
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
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from '@/components/ui/alert-dialog';
import { RefreshCw, Database, Clock, HardDrive, Trash2, Search } from 'lucide-react';

interface CacheItem {
  id: string;
  name: string;
  type: 'user' | 'search' | 'hotel' | 'index' | 'api';
  size: string;
  hits: number;
  lastAccess: string;
  ttl: number; // 生存时间（秒）
  status: 'active' | 'expired' | 'cleared';
}

interface CacheStats {
  totalSize: string;
  hitRate: number;
  memoryUsage: number;
  activeItems: number;
  expiredItems: number;
}

// 模拟缓存数据
const mockCacheItems: CacheItem[] = [
  {
    id: '1',
    name: '热门城市搜索缓存',
    type: 'search',
    size: '12.5 MB',
    hits: 15678,
    lastAccess: '2025-11-18 02:20:00',
    ttl: 3600,
    status: 'active',
  },
  {
    id: '2',
    name: '酒店详情数据缓存',
    type: 'hotel',
    size: '245.8 MB',
    hits: 8934,
    lastAccess: '2025-11-18 02:19:30',
    ttl: 1800,
    status: 'active',
  },
  {
    id: '3',
    name: '用户会话缓存',
    type: 'user',
    size: '8.2 MB',
    hits: 23567,
    lastAccess: '2025-11-18 02:18:45',
    ttl: 7200,
    status: 'active',
  },
  {
    id: '4',
    name: '索引映射缓存',
    type: 'index',
    size: '45.3 MB',
    hits: 1234,
    lastAccess: '2025-11-18 02:10:15',
    ttl: 86400,
    status: 'active',
  },
  {
    id: '5',
    name: 'API响应缓存',
    type: 'api',
    size: '67.9 MB',
    hits: 3456,
    lastAccess: '2025-11-17 22:30:20',
    ttl: 1800,
    status: 'expired',
  },
  {
    id: '6',
    name: '搜索建议缓存',
    type: 'search',
    size: '3.7 MB',
    hits: 9876,
    lastAccess: '2025-11-18 01:45:10',
    ttl: 900,
    status: 'active',
  },
];

const mockCacheStats: CacheStats = {
  totalSize: '383.4 MB',
  hitRate: 87.3,
  memoryUsage: 65.2,
  activeItems: 89,
  expiredItems: 12,
};

const getTypeColor = (type: string) => {
  switch (type) {
    case 'user':
      return 'bg-blue-100 text-blue-800';
    case 'search':
      return 'bg-green-100 text-green-800';
    case 'hotel':
      return 'bg-purple-100 text-purple-800';
    case 'index':
      return 'bg-orange-100 text-orange-800';
    case 'api':
      return 'bg-gray-100 text-gray-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getTypeName = (type: string) => {
  switch (type) {
    case 'user':
      return '用户缓存';
    case 'search':
      return '搜索缓存';
    case 'hotel':
      return '酒店缓存';
    case 'index':
      return '索引缓存';
    case 'api':
      return 'API缓存';
    default:
      return type;
  }
};

const getStatusColor = (status: string) => {
  switch (status) {
    case 'active':
      return 'bg-green-100 text-green-800';
    case 'expired':
      return 'bg-red-100 text-red-800';
    case 'cleared':
      return 'bg-gray-100 text-gray-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getStatusName = (status: string) => {
  switch (status) {
    case 'active':
      return '活跃';
    case 'expired':
      return '已过期';
    case 'cleared':
      return '已清理';
    default:
      return status;
  }
};

const formatBytes = (bytes: number) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

function CacheContent() {
  const [cacheItems, setCacheItems] = useState<CacheItem[]>(mockCacheItems);
  const [cacheStats] = useState<CacheStats>(mockCacheStats);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [isLoading, setIsLoading] = useState(false);

  // 搜索缓存
  const handleSearch = (value: string) => {
    setSearchTerm(value);
  };

  // 筛选缓存
  const handleTypeFilter = (value: string) => {
    setTypeFilter(value);
  };

  const handleStatusFilter = (value: string) => {
    setStatusFilter(value);
  };

  // 筛选和搜索逻辑
  const filteredCacheItems = cacheItems
    .filter(item => {
      const matchesSearch = item.name.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesType = typeFilter === 'all' || item.type === typeFilter;
      const matchesStatus = statusFilter === 'all' || item.status === statusFilter;
      return matchesSearch && matchesType && matchesStatus;
    });

  // 清理单个缓存
  const handleClearCache = (cacheId: string) => {
    setCacheItems(prevItems =>
      prevItems.map(item =>
        item.id === cacheId ? { ...item, status: 'cleared' as const, hits: 0 } : item
      )
    );
    console.log('缓存已清理:', cacheId);
  };

  // 清理过期缓存
  const handleClearExpiredCache = () => {
    setCacheItems(prevItems =>
      prevItems.map(item =>
        item.status === 'expired' ? { ...item, status: 'cleared' as const, hits: 0 } : item
      )
    );
    console.log('过期缓存已清理');
  };

  // 全量清理缓存
  const handleClearAllCache = async () => {
    setIsLoading(true);
    // 模拟清理延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    setCacheItems(prevItems =>
      prevItems.map(item => ({
        ...item,
        status: 'cleared' as const,
        hits: 0,
        lastAccess: new Date().toISOString().replace('T', ' ').slice(0, 19)
      }))
    );
    setIsLoading(false);
    console.log('所有缓存已清理');
  };

  // 刷新缓存统计
  const handleRefreshStats = async () => {
    setIsLoading(true);
    // 模拟刷新延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
    console.log('缓存统计已刷新');
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 页面头部操作区域 */}
      <div className="bg-card border-b">
        <div className="container px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-card-foreground">缓存管理</h1>
              <p className="text-sm text-muted-foreground">
                管理系统缓存，优化性能和响应速度
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <Button
                variant="outline"
                onClick={handleRefreshStats}
                disabled={isLoading}
                className="flex items-center space-x-2"
              >
                <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
                <span>刷新统计</span>
              </Button>
              <Button
                variant="outline"
                onClick={handleClearExpiredCache}
                disabled={isLoading}
                className="flex items-center space-x-2"
              >
                <Trash2 className="h-4 w-4" />
                <span>清理过期</span>
              </Button>
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button variant="destructive" disabled={isLoading}>
                    全量清理
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>确认全量清理缓存</AlertDialogTitle>
                    <AlertDialogDescription>
                      此操作将清理所有缓存数据，包括活跃缓存。清理后可能导致系统性能暂时下降，
                      建议在低峰期操作。是否继续？
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>取消</AlertDialogCancel>
                    <AlertDialogAction
                      onClick={handleClearAllCache}
                      className="bg-red-600 hover:bg-red-700"
                    >
                      确认清理
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6 sm:px-6 lg:px-8">
        {/* 缓存统计概览 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总缓存大小</CardTitle>
              <HardDrive className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{cacheStats.totalSize}</div>
              <p className="text-xs text-muted-foreground">
                当前系统缓存占用
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">命中率</CardTitle>
              <Database className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{cacheStats.hitRate}%</div>
              <p className="text-xs text-muted-foreground">
                {cacheStats.hitRate > 80 ? '命中率良好' : cacheStats.hitRate > 60 ? '命中率一般' : '命中率较低'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">内存使用率</CardTitle>
              <span className="text-2xl">💾</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{cacheStats.memoryUsage}%</div>
              <p className="text-xs text-muted-foreground">
                {cacheStats.memoryUsage > 80 ? '内存使用过高' : cacheStats.memoryUsage > 60 ? '内存使用适中' : '内存充足'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">缓存项目</CardTitle>
              <span className="text-2xl">📦</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {cacheStats.activeItems + cacheStats.expiredItems}
              </div>
              <p className="text-xs text-muted-foreground">
                活跃: {cacheStats.activeItems} | 过期: {cacheStats.expiredItems}
              </p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>缓存搜索和筛选</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="flex-1">
                <div className="relative">
                  <Search className="absolute left-3 top-3 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder="搜索缓存名称..."
                    value={searchTerm}
                    onChange={(e) => handleSearch(e.target.value)}
                    className="pl-10 w-full"
                  />
                </div>
              </div>
              <Select value={typeFilter} onValueChange={handleTypeFilter}>
                <SelectTrigger className="w-[150px]">
                  <SelectValue placeholder="缓存类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有类型</SelectItem>
                  <SelectItem value="user">用户缓存</SelectItem>
                  <SelectItem value="search">搜索缓存</SelectItem>
                  <SelectItem value="hotel">酒店缓存</SelectItem>
                  <SelectItem value="index">索引缓存</SelectItem>
                  <SelectItem value="api">API缓存</SelectItem>
                </SelectContent>
              </Select>
              <Select value={statusFilter} onValueChange={handleStatusFilter}>
                <SelectTrigger className="w-[150px]">
                  <SelectValue placeholder="缓存状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有状态</SelectItem>
                  <SelectItem value="active">活跃</SelectItem>
                  <SelectItem value="expired">已过期</SelectItem>
                  <SelectItem value="cleared">已清理</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 缓存列表 */}
        <Card>
          <CardHeader>
            <CardTitle>缓存列表</CardTitle>
            <p className="text-sm text-muted-foreground">
              共 {filteredCacheItems.length} 个缓存项目
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
                    <TableHead>缓存名称</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead>大小</TableHead>
                    <TableHead>访问次数</TableHead>
                    <TableHead>最后访问</TableHead>
                    <TableHead>TTL</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredCacheItems.map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-medium">{item.name}</TableCell>
                      <TableCell>
                        <Badge className={getTypeColor(item.type)}>
                          {getTypeName(item.type)}
                        </Badge>
                      </TableCell>
                      <TableCell>{item.size}</TableCell>
                      <TableCell>{item.hits.toLocaleString()}</TableCell>
                      <TableCell>{item.lastAccess}</TableCell>
                      <TableCell>{item.ttl}秒</TableCell>
                      <TableCell>
                        <Badge className={getStatusColor(item.status)}>
                          {getStatusName(item.status)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {item.status === 'active' && (
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button variant="ghost" size="sm">
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>确认清理缓存</AlertDialogTitle>
                                <AlertDialogDescription>
                                  确定要清理缓存 "{item.name}" 吗？此操作不可恢复。
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>取消</AlertDialogCancel>
                                <AlertDialogAction onClick={() => handleClearCache(item.id)}>
                                  确认清理
                                </AlertDialogAction>
                              </AlertDialogFooter>
                            </AlertDialogContent>
                          </AlertDialog>
                        )}
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

export default function CachePage() {
  return (
    <ProtectedRoute>
      <Header />
      <CacheContent />
    </ProtectedRoute>
  );
}