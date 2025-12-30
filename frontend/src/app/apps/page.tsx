// 应用管理页面
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
import { EditAppDialog } from '@/components/apps/EditAppDialog';
import { RotateCcw, Eye, EyeOff } from 'lucide-react';
import { useApps } from '@/hooks/api/useApps';
import type { UpdateAppRequest } from '@/types/auth';

// 应用类型定义（前端展示用）
interface AppInfo {
  id: string;
  name: string;
  appId: string;
  secretKey: string;
  status: 'active' | 'inactive' | 'suspended';
  rateLimit: number;
  timeout: number;
  creator: string;
  createdAt: string;
  lastUsedAt?: string;
  description?: string;
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'active':
      return 'bg-green-100 text-green-800';
    case 'inactive':
      return 'bg-gray-100 text-gray-800';
    case 'suspended':
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
      return '未激活';
    case 'suspended':
      return '已暂停';
    default:
      return status;
  }
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
};

function AppContent() {
  // 使用 API hook 获取应用数据
  const {
    apps,
    loading: isLoading,
    error: fetchError,
    pagination,
    updateApp: apiUpdateApp,
    refreshSecret: apiRefreshSecret,
    refresh,
  } = useApps();

  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [sortField, setSortField] = useState<keyof AppInfo>('createdAt');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [showSecrets, setShowSecrets] = useState<{ [key: string]: boolean }>({});

  // 编辑应用处理函数
  const handleEditApp = async (updatedApp: { appId: string; rateLimit: number; timeout?: number }) => {
    // 调用后端 API 更新应用
    const updateRequest: UpdateAppRequest = {
      rateLimit: updatedApp.rateLimit,
      timeout: updatedApp.timeout,
    };
    
    await apiUpdateApp(updatedApp.appId, updateRequest);
    console.log('App edited successfully via API:', updatedApp);
  };

  // 生成新密钥
  const handleGenerateNewSecret = async (appId: string) => {
    try {
      const newSecret = await apiRefreshSecret(appId);
      console.log('Secret key regenerated for app:', appId, 'new secret:', newSecret);
    } catch (error) {
      console.error('Failed to refresh secret:', error);
    }
  };

  // 切换密钥显示
  const toggleSecretVisibility = (appId: string) => {
    setShowSecrets(prev => ({
      ...prev,
      [appId]: !prev[appId]
    }));
  };

  // 复制到剪贴板
  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    console.log('Copied to clipboard:', text);
  };

  // 筛选和排序逻辑
  const filteredApps = apps
    .filter(app => {
      const matchesSearch = app.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                           app.appId.toLowerCase().includes(searchTerm.toLowerCase());
      const matchesStatus = statusFilter === 'all' || app.status === statusFilter;
      return matchesSearch && matchesStatus;
    })
    .sort((a, b) => {
      const aValue = a[sortField];
      const bValue = b[sortField];
      
      // 处理可能为undefined的值
      if (aValue === undefined && bValue === undefined) return 0;
      if (aValue === undefined) return 1;
      if (bValue === undefined) return -1;
      
      const comparison = aValue < bValue ? -1 : aValue > bValue ? 1 : 0;
      return sortDirection === 'asc' ? comparison : -comparison;
    });

  // 分页逻辑
  const totalPages = Math.ceil(filteredApps.length / pageSize);
  const startIndex = (currentPage - 1) * pageSize;
  const endIndex = startIndex + pageSize;
  const currentApps = filteredApps.slice(startIndex, endIndex);

  const handleSort = (field: keyof AppInfo) => {
    if (field === sortField) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  };

  const handleSearch = (value: string) => {
    setSearchTerm(value);
    setCurrentPage(1); // 重置到第一页
  };

  const handleStatusFilter = (value: string) => {
    setStatusFilter(value);
    setCurrentPage(1); // 重置到第一页
  };

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 页面头部操作区域 */}
      <div className="bg-card border-b">
        <div className="container">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-card-foreground">应用管理</h1>
              <p className="text-sm text-muted-foreground">管理应用配置和API密钥</p>
            </div>
            <div className="flex items-center space-x-4">
              <Button variant="outline" onClick={refresh} disabled={isLoading}>
                {isLoading ? '加载中...' : '刷新'}
              </Button>
              <p className="text-sm text-muted-foreground">
                (应用由用户创建时自动生成)
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6">
        {/* 错误提示 */}
        {fetchError && (
          <Card className="mb-6 bg-red-50 border-red-200">
            <CardContent className="py-4">
              <p className="text-red-600">{fetchError}</p>
            </CardContent>
          </Card>
        )}

        {/* 统计卡片 */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-gray-600">总应用数</CardTitle>
              <span className="text-2xl">📱</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{isLoading ? '-' : pagination.totalElements}</div>
              <p className="text-xs text-gray-500">注册应用总数</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-gray-600">活跃应用</CardTitle>
              <span className="text-2xl">✅</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {isLoading ? '-' : apps.filter(app => app.status === 'active').length}
              </div>
              <p className="text-xs text-gray-500">可正常使用</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-gray-600">未激活</CardTitle>
              <span className="text-2xl">⏳</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {isLoading ? '-' : apps.filter(app => app.status === 'inactive').length}
              </div>
              <p className="text-xs text-gray-500">待激活使用</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-gray-600">已暂停</CardTitle>
              <span className="text-2xl">⏸️</span>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {isLoading ? '-' : apps.filter(app => app.status === 'suspended').length}
              </div>
              <p className="text-xs text-gray-500">暂停使用</p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>搜索和筛选</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="flex-1">
                <Input
                  placeholder="搜索应用名称或AppId..."
                  value={searchTerm}
                  onChange={(e) => handleSearch(e.target.value)}
                  className="w-full"
                />
              </div>
              <Select value={statusFilter} onValueChange={handleStatusFilter}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="选择状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有状态</SelectItem>
                  <SelectItem value="active">活跃</SelectItem>
                  <SelectItem value="inactive">未激活</SelectItem>
                  <SelectItem value="suspended">已暂停</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 应用列表 */}
        <Card>
          <CardHeader>
            <CardTitle>应用列表</CardTitle>
            <p className="text-sm text-gray-600">
              共 {filteredApps.length} 个应用
              {searchTerm && ` (搜索: "${searchTerm}")`}
              {statusFilter !== 'all' && ` (状态: ${getStatusName(statusFilter)})`}
            </p>
          </CardHeader>
          <CardContent>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead 
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => handleSort('name')}
                    >
                      应用名称 {sortField === 'name' && (sortDirection === 'asc' ? '↑' : '↓')}
                    </TableHead>
                    <TableHead 
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => handleSort('appId')}
                    >
                      AppId {sortField === 'appId' && (sortDirection === 'asc' ? '↑' : '↓')}
                    </TableHead>
                    <TableHead>密钥</TableHead>
                    <TableHead 
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => handleSort('status')}
                    >
                      状态 {sortField === 'status' && (sortDirection === 'asc' ? '↑' : '↓')}
                    </TableHead>
                    <TableHead 
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => handleSort('rateLimit')}
                    >
                      限流 {sortField === 'rateLimit' && (sortDirection === 'asc' ? '↑' : '↓')}
                    </TableHead>
                    <TableHead 
                      className="cursor-pointer hover:bg-gray-50"
                      onClick={() => handleSort('createdAt')}
                    >
                      创建时间 {sortField === 'createdAt' && (sortDirection === 'asc' ? '↑' : '↓')}
                    </TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {currentApps.map((app) => (
                    <TableRow key={app.id}>
                      <TableCell className="font-medium">
                        <div>
                          <div className="font-semibold">{app.name}</div>
                          {app.description && (
                            <div className="text-sm text-gray-500">{app.description}</div>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="font-mono text-sm">{app.appId}</TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <code className="text-xs bg-gray-100 px-2 py-1 rounded">
                            {showSecrets[app.id] ? app.secretKey : '•'.repeat(24)}
                          </code>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => toggleSecretVisibility(app.id)}
                          >
                            {showSecrets[app.id] ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => copyToClipboard(app.secretKey)}
                          >
                            📋
                          </Button>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge className={getStatusColor(app.status)}>
                          {getStatusName(app.status)}
                        </Badge>
                      </TableCell>
                      <TableCell>{app.rateLimit}/秒</TableCell>
                      <TableCell>{formatDate(app.createdAt)}</TableCell>
                      <TableCell>
                        <div className="flex space-x-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleGenerateNewSecret(app.appId)}
                            title="刷新密钥"
                          >
                            <RotateCcw className="h-4 w-4" />
                          </Button>
                          <EditAppDialog 
                            app={app} 
                            onEditApp={handleEditApp} 
                          />
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>

            {/* 分页 */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-4">
                <p className="text-sm text-gray-700">
                  显示第 {startIndex + 1} - {Math.min(endIndex, filteredApps.length)} 条，共 {filteredApps.length} 条
                </p>
                <div className="flex space-x-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 1}
                  >
                    上一页
                  </Button>
                  <span className="px-4 py-2 text-sm">
                    第 {currentPage} 页，共 {totalPages} 页
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage === totalPages}
                  >
                    下一页
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  );
}

export default function AppPage() {
  return (
    <ProtectedRoute>
      <Header />
      <AppContent />
    </ProtectedRoute>
  );
}