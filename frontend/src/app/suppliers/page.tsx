// 供应商管理页面
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
  Building, 
  Plus, 
  Edit, 
  Trash2, 
  Activity, 
  CheckCircle,
  XCircle,
  AlertCircle,
  Settings,
  ExternalLink,
  Clock,
  TrendingUp,
  Database
} from 'lucide-react';

interface Supplier {
  id: string;
  name: string;
  type: 'booking' | 'hotel-chain' | 'aggregator' | 'direct';
  status: 'active' | 'inactive' | 'maintenance' | 'error';
  apiEndpoint: string;
  description: string;
  lastSync: string;
  syncStatus: 'success' | 'failed' | 'in-progress' | 'pending';
  responseTime: number;
  successRate: number;
  totalRecords: number;
  errorCount: number;
  config: {
    apiKey?: string;
    timeout: number;
    retryCount: number;
    rateLimit: number;
  };
}

interface SupplierStats {
  totalSuppliers: number;
  activeSuppliers: number;
  failedSuppliers: number;
  totalRecords: number;
  avgResponseTime: number;
  avgSuccessRate: number;
}

// 模拟供应商数据
const mockSuppliers: Supplier[] = [
  {
    id: '1',
    name: 'Booking.com API',
    type: 'aggregator',
    status: 'active',
    apiEndpoint: 'https://api.booking.com/v1',
    description: '全球最大的在线旅行预订平台API',
    lastSync: '2025-11-18 03:15:30',
    syncStatus: 'success',
    responseTime: 245,
    successRate: 98.7,
    totalRecords: 1250000,
    errorCount: 12,
    config: {
      apiKey: 'booking_api_key_****',
      timeout: 30000,
      retryCount: 3,
      rateLimit: 1000,
    },
  },
  {
    id: '2',
    name: '万豪国际酒店集团',
    type: 'hotel-chain',
    status: 'active',
    apiEndpoint: 'https://api.marriott.com/v2',
    description: '万豪国际官方API，提供直连酒店数据',
    lastSync: '2025-11-18 03:10:15',
    syncStatus: 'success',
    responseTime: 189,
    successRate: 99.2,
    totalRecords: 850000,
    errorCount: 5,
    config: {
      apiKey: 'marriott_api_key_****',
      timeout: 25000,
      retryCount: 2,
      rateLimit: 500,
    },
  },
  {
    id: '3',
    name: '携程旅行网',
    type: 'booking',
    status: 'maintenance',
    apiEndpoint: 'https://api.ctrip.com/hotel/search',
    description: '中国领先的在线旅行服务平台API',
    lastSync: '2025-11-18 02:45:20',
    syncStatus: 'failed',
    responseTime: 0,
    successRate: 0,
    totalRecords: 750000,
    errorCount: 156,
    config: {
      apiKey: 'ctrip_api_key_****',
      timeout: 30000,
      retryCount: 3,
      rateLimit: 800,
    },
  },
  {
    id: '4',
    name: '希尔顿酒店集团',
    type: 'hotel-chain',
    status: 'active',
    apiEndpoint: 'https://api.hilton.com/hotels',
    description: '希尔顿全球酒店集团官方API',
    lastSync: '2025-11-18 03:12:45',
    syncStatus: 'in-progress',
    responseTime: 567,
    successRate: 94.3,
    totalRecords: 620000,
    errorCount: 28,
    config: {
      apiKey: 'hilton_api_key_****',
      timeout: 35000,
      retryCount: 2,
      rateLimit: 300,
    },
  },
  {
    id: '5',
    name: 'Airbnb合作伙伴API',
    type: 'aggregator',
    status: 'inactive',
    apiEndpoint: 'https://partner-api.airbnb.com/v1',
    description: 'Airbnb全球房源合作伙伴API',
    lastSync: '2025-11-17 20:30:00',
    syncStatus: 'pending',
    responseTime: 0,
    successRate: 0,
    totalRecords: 0,
    errorCount: 0,
    config: {
      apiKey: 'airbnb_api_key_****',
      timeout: 30000,
      retryCount: 3,
      rateLimit: 200,
    },
  },
];

const mockSupplierStats: SupplierStats = {
  totalSuppliers: 12,
  activeSuppliers: 8,
  failedSuppliers: 2,
  totalRecords: 3600000,
  avgResponseTime: 312,
  avgSuccessRate: 96.4,
};

const getTypeColor = (type: string) => {
  switch (type) {
    case 'booking':
      return 'bg-blue-100 text-blue-800';
    case 'hotel-chain':
      return 'bg-green-100 text-green-800';
    case 'aggregator':
      return 'bg-purple-100 text-purple-800';
    case 'direct':
      return 'bg-orange-100 text-orange-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getTypeName = (type: string) => {
  switch (type) {
    case 'booking':
      return '预订平台';
    case 'hotel-chain':
      return '酒店集团';
    case 'aggregator':
      return '聚合平台';
    case 'direct':
      return '直连酒店';
    default:
      return type;
  }
};

const getStatusColor = (status: string) => {
  switch (status) {
    case 'active':
      return 'bg-green-100 text-green-800';
    case 'inactive':
      return 'bg-gray-100 text-gray-800';
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
    case 'maintenance':
      return '维护中';
    case 'error':
      return '错误';
    default:
      return status;
  }
};

const getSyncStatusColor = (status: string) => {
  switch (status) {
    case 'success':
      return 'text-green-600';
    case 'failed':
      return 'text-red-600';
    case 'in-progress':
      return 'text-blue-600';
    case 'pending':
      return 'text-muted-foreground';
    default:
      return 'text-muted-foreground';
  }
};

const getSyncStatusIcon = (status: string) => {
  switch (status) {
    case 'success':
      return <CheckCircle className="h-4 w-4" />;
    case 'failed':
      return <XCircle className="h-4 w-4" />;
    case 'in-progress':
      return <Activity className="h-4 w-4 animate-pulse" />;
    case 'pending':
      return <Clock className="h-4 w-4" />;
    default:
      return <AlertCircle className="h-4 w-4" />;
  }
};

function SuppliersContent() {
  const [suppliers] = useState<Supplier[]>(mockSuppliers);
  const [supplierStats] = useState<SupplierStats>(mockSupplierStats);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [syncFilter, setSyncFilter] = useState('all');
  const [selectedSuppliers, setSelectedSuppliers] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // 搜索和筛选逻辑
  const filteredSuppliers = suppliers.filter(supplier => {
    const matchesSearch = searchTerm === '' || 
      supplier.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      supplier.description.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesType = typeFilter === 'all' || supplier.type === typeFilter;
    const matchesStatus = statusFilter === 'all' || supplier.status === statusFilter;
    const matchesSync = syncFilter === 'all' || supplier.syncStatus === syncFilter;
    return matchesSearch && matchesType && matchesStatus && matchesSync;
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

  const handleSyncFilter = (value: string) => {
    setSyncFilter(value);
  };

  // 供应商选择处理
  const handleSupplierSelect = (supplierId: string, checked: boolean) => {
    if (checked) {
      setSelectedSuppliers(prev => [...prev, supplierId]);
    } else {
      setSelectedSuppliers(prev => prev.filter(id => id !== supplierId));
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedSuppliers(filteredSuppliers.map(supplier => supplier.id));
    } else {
      setSelectedSuppliers([]);
    }
  };

  // 健康检查
  const handleHealthCheck = async (supplierId: string) => {
    setIsLoading(true);
    console.log('执行供应商健康检查:', supplierId);
    // 模拟健康检查延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    setIsLoading(false);
  };

  // 批量健康检查
  const handleBatchHealthCheck = async () => {
    setIsLoading(true);
    console.log('批量执行健康检查:', selectedSuppliers);
    // 模拟批量检查延迟
    await new Promise(resolve => setTimeout(resolve, 3000));
    setIsLoading(false);
    setSelectedSuppliers([]);
  };

  // 同步供应商数据
  const handleSyncSupplier = async (supplierId: string) => {
    setIsLoading(true);
    console.log('同步供应商数据:', supplierId);
    // 模拟同步延迟
    await new Promise(resolve => setTimeout(resolve, 5000));
    setIsLoading(false);
  };

  // 禁用供应商
  const handleDisableSupplier = async (supplierId: string) => {
    setIsLoading(true);
    console.log('禁用供应商:', supplierId);
    // 模拟禁用操作
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 启用供应商
  const handleEnableSupplier = async (supplierId: string) => {
    setIsLoading(true);
    console.log('启用供应商:', supplierId);
    // 模拟启用操作
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 页面头部操作区域 */}
      <div className="bg-card border-b">
        <div className="container px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-card-foreground">供应商管理</h1>
              <p className="text-sm text-muted-foreground">
                管理第三方供应商连接和同步状态
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <Button
                variant="outline"
                onClick={handleBatchHealthCheck}
                disabled={isLoading || selectedSuppliers.length === 0}
                className="flex items-center space-x-2"
              >
                <Activity className="h-4 w-4" />
                <span>批量检查 ({selectedSuppliers.length})</span>
              </Button>
              <Button className="flex items-center space-x-2">
                <Plus className="h-4 w-4" />
                <span>添加供应商</span>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6 sm:px-6 lg:px-8">
        {/* 供应商统计概览 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总供应商数</CardTitle>
              <Building className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{supplierStats.totalSuppliers}</div>
              <p className="text-xs text-muted-foreground">
                活跃: {supplierStats.activeSuppliers} | 异常: {supplierStats.failedSuppliers}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总记录数</CardTitle>
              <Database className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{(supplierStats.totalRecords / 1000000).toFixed(1)}M</div>
              <p className="text-xs text-muted-foreground">
                累计酒店记录数量
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">平均响应时间</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{supplierStats.avgResponseTime}ms</div>
              <p className="text-xs text-muted-foreground">
                {supplierStats.avgResponseTime > 500 ? '较慢' : supplierStats.avgResponseTime > 300 ? '正常' : '良好'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">平均成功率</CardTitle>
              <CheckCircle className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{supplierStats.avgSuccessRate}%</div>
              <p className="text-xs text-muted-foreground">
                API调用成功率
              </p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>供应商搜索和筛选</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              <Input
                placeholder="搜索供应商名称或描述..."
                value={searchTerm}
                onChange={(e) => handleSearch(e.target.value)}
              />
              <Select value={typeFilter} onValueChange={handleTypeFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="供应商类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有类型</SelectItem>
                  <SelectItem value="booking">预订平台</SelectItem>
                  <SelectItem value="hotel-chain">酒店集团</SelectItem>
                  <SelectItem value="aggregator">聚合平台</SelectItem>
                  <SelectItem value="direct">直连酒店</SelectItem>
                </SelectContent>
              </Select>
              <Select value={statusFilter} onValueChange={handleStatusFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="供应商状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有状态</SelectItem>
                  <SelectItem value="active">活跃</SelectItem>
                  <SelectItem value="inactive">停用</SelectItem>
                  <SelectItem value="maintenance">维护中</SelectItem>
                  <SelectItem value="error">错误</SelectItem>
                </SelectContent>
              </Select>
              <Select value={syncFilter} onValueChange={handleSyncFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="同步状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有同步状态</SelectItem>
                  <SelectItem value="success">同步成功</SelectItem>
                  <SelectItem value="failed">同步失败</SelectItem>
                  <SelectItem value="in-progress">同步中</SelectItem>
                  <SelectItem value="pending">等待同步</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 供应商列表 */}
        <Card>
          <CardHeader>
            <CardTitle>供应商列表</CardTitle>
            <p className="text-sm text-muted-foreground">
              共 {filteredSuppliers.length} 个供应商
              {selectedSuppliers.length > 0 && ` (已选择 ${selectedSuppliers.length} 个)`}
              {searchTerm && ` (搜索: "${searchTerm}")`}
              {typeFilter !== 'all' && ` (类型: ${getTypeName(typeFilter)})`}
              {statusFilter !== 'all' && ` (状态: ${getStatusName(statusFilter)})`}
              {syncFilter !== 'all' && ` (同步: ${syncFilter})`}
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
                        checked={filteredSuppliers.length > 0 && filteredSuppliers.every(supplier => selectedSuppliers.includes(supplier.id))}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        className="rounded"
                      />
                    </TableHead>
                    <TableHead>供应商名称</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>同步状态</TableHead>
                    <TableHead>响应时间</TableHead>
                    <TableHead>成功率</TableHead>
                    <TableHead>记录数</TableHead>
                    <TableHead>最后同步</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredSuppliers.map((supplier) => (
                    <TableRow key={supplier.id} className={selectedSuppliers.includes(supplier.id) ? 'bg-blue-50' : ''}>
                      <TableCell>
                        <input
                          type="checkbox"
                          checked={selectedSuppliers.includes(supplier.id)}
                          onChange={(e) => handleSupplierSelect(supplier.id, e.target.checked)}
                          className="rounded"
                        />
                      </TableCell>
                      <TableCell>
                        <div>
                          <div className="font-medium">{supplier.name}</div>
                          <div className="text-sm text-gray-500 truncate max-w-xs" title={supplier.description}>
                            {supplier.description}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge className={getTypeColor(supplier.type)}>
                          {getTypeName(supplier.type)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge className={getStatusColor(supplier.status)}>
                          {getStatusName(supplier.status)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className={`flex items-center space-x-1 ${getSyncStatusColor(supplier.syncStatus)}`}>
                          {getSyncStatusIcon(supplier.syncStatus)}
                          <span className="text-sm capitalize">{supplier.syncStatus}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        {supplier.responseTime > 0 ? `${supplier.responseTime}ms` : '-'}
                      </TableCell>
                      <TableCell>
                        {supplier.successRate > 0 ? `${supplier.successRate}%` : '-'}
                      </TableCell>
                      <TableCell>
                        {supplier.totalRecords > 0 ? supplier.totalRecords.toLocaleString() : '-'}
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {supplier.lastSync}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleHealthCheck(supplier.id)}
                            disabled={isLoading}
                            title="健康检查"
                          >
                            <Activity className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleSyncSupplier(supplier.id)}
                            disabled={isLoading || supplier.status !== 'active'}
                            title="同步数据"
                          >
                            <TrendingUp className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            title="查看详情"
                          >
                            <Settings className="h-4 w-4" />
                          </Button>
                          {supplier.status === 'active' ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDisableSupplier(supplier.id)}
                              disabled={isLoading}
                              title="禁用供应商"
                            >
                              <XCircle className="h-4 w-4 text-red-500" />
                            </Button>
                          ) : (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleEnableSupplier(supplier.id)}
                              disabled={isLoading}
                              title="启用供应商"
                            >
                              <CheckCircle className="h-4 w-4 text-green-500" />
                            </Button>
                          )}
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button
                                variant="ghost"
                                size="sm"
                                disabled={isLoading}
                                title="删除供应商"
                              >
                                <Trash2 className="h-4 w-4 text-red-500" />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>删除供应商</AlertDialogTitle>
                                <AlertDialogDescription>
                                  确定要删除供应商 "{supplier.name}" 吗？此操作不可撤销，将永久删除所有相关配置和数据。
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>取消</AlertDialogCancel>
                                <AlertDialogAction className="bg-red-600 hover:bg-red-700">
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

export default function SuppliersPage() {
  return (
    <ProtectedRoute>
      <Header />
      <SuppliersContent />
    </ProtectedRoute>
  );
}