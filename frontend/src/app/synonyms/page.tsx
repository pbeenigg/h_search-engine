// 同义词管理页面
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
  Hash, 
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
  FileText,
  Tag
} from 'lucide-react';

interface SynonymGroup {
  id: string;
  name: string;
  type: 'explicit' | 'synonym';
  category: string;
  terms: string[];
  description: string;
  version: string;
  status: 'active' | 'inactive' | 'draft' | 'published';
  createdAt: string;
  updatedAt: string;
  lastSyncAt?: string;
  syncStatus: 'synced' | 'pending' | 'failed' | 'syncing';
  usage: {
    total: number;
    successful: number;
    failed: number;
  };
}

interface SynonymStats {
  totalGroups: number;
  activeGroups: number;
  draftGroups: number;
  totalTerms: number;
  categoriesCount: number;
  avgTermsPerGroup: number;
}

// 模拟同义词数据
const mockSynonymGroups: SynonymGroup[] = [
  {
    id: '1',
    name: '酒店品牌同义词',
    type: 'explicit',
    category: 'brand',
    terms: ['希尔顿', 'Hilton', 'hilton hotels', '希尔顿酒店'],
    description: '希尔顿酒店集团品牌相关同义词',
    version: 'v1.2.0',
    status: 'active',
    createdAt: '2025-11-15 10:30:00',
    updatedAt: '2025-11-18 02:15:00',
    lastSyncAt: '2025-11-18 03:20:00',
    syncStatus: 'synced',
    usage: {
      total: 15678,
      successful: 15420,
      failed: 258,
    },
  },
  {
    id: '2',
    name: '房间类型同义词',
    type: 'synonym',
    category: 'room-type',
    terms: ['标准间', '大床房', '双人房', 'double room', 'double bed'],
    description: '各种房间类型的同义词',
    version: 'v1.1.5',
    status: 'active',
    createdAt: '2025-11-14 14:20:00',
    updatedAt: '2025-11-17 16:45:00',
    lastSyncAt: '2025-11-18 03:15:00',
    syncStatus: 'synced',
    usage: {
      total: 8934,
      successful: 8876,
      failed: 58,
    },
  },
  {
    id: '3',
    name: '城市别名',
    type: 'explicit',
    category: 'location',
    terms: ['北京', '北京市', 'Beijing', 'Peking', '京'],
    description: '北京相关城市名称和别名',
    version: 'v2.0.1',
    status: 'draft',
    createdAt: '2025-11-16 09:15:00',
    updatedAt: '2025-11-18 01:30:00',
    syncStatus: 'pending',
    usage: {
      total: 0,
      successful: 0,
      failed: 0,
    },
  },
  {
    id: '4',
    name: '设施设施同义词',
    type: 'synonym',
    category: 'facility',
    terms: ['健身房', '健身中心', 'gym', 'fitness center', 'workout'],
    description: '健身相关设施的同义词',
    version: 'v1.0.8',
    status: 'active',
    createdAt: '2025-11-13 11:45:00',
    updatedAt: '2025-11-16 13:20:00',
    lastSyncAt: '2025-11-17 22:30:00',
    syncStatus: 'failed',
    usage: {
      total: 3456,
      successful: 3201,
      failed: 255,
    },
  },
  {
    id: '5',
    name: '商务设施',
    type: 'explicit',
    category: 'facility',
    terms: ['会议室', '商务中心', 'meeting room', 'conference room', 'business center'],
    description: '商务会议相关设施',
    version: 'v1.1.2',
    status: 'published',
    createdAt: '2025-11-12 08:20:00',
    updatedAt: '2025-11-15 17:30:00',
    lastSyncAt: '2025-11-18 02:45:00',
    syncStatus: 'syncing',
    usage: {
      total: 2178,
      successful: 2156,
      failed: 22,
    },
  },
];

const mockSynonymStats: SynonymStats = {
  totalGroups: 127,
  activeGroups: 98,
  draftGroups: 12,
  totalTerms: 1847,
  categoriesCount: 8,
  avgTermsPerGroup: 3.4,
};

const getTypeColor = (type: string) => {
  switch (type) {
    case 'explicit':
      return 'bg-blue-100 text-blue-800';
    case 'synonym':
      return 'bg-green-100 text-green-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getTypeName = (type: string) => {
  switch (type) {
    case 'explicit':
      return '显式同义词';
    case 'synonym':
      return '对称同义词';
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
    case 'draft':
      return 'bg-yellow-100 text-yellow-800';
    case 'published':
      return 'bg-purple-100 text-purple-800';
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
    case 'draft':
      return '草稿';
    case 'published':
      return '已发布';
    default:
      return status;
  }
};

const getSyncStatusColor = (status: string) => {
  switch (status) {
    case 'synced':
      return 'text-green-600';
    case 'pending':
      return 'text-yellow-600';
    case 'failed':
      return 'text-red-600';
    case 'syncing':
      return 'text-blue-600';
    default:
      return 'text-muted-foreground';
  }
};

const getSyncStatusIcon = (status: string) => {
  switch (status) {
    case 'synced':
      return <CheckCircle className="h-4 w-4" />;
    case 'pending':
      return <Clock className="h-4 w-4" />;
    case 'failed':
      return <XCircle className="h-4 w-4" />;
    case 'syncing':
      return <RefreshCw className="h-4 w-4 animate-spin" />;
    default:
      return <RefreshCw className="h-4 w-4" />;
  }
};

function SynonymsContent() {
  const [synonymGroups] = useState<SynonymGroup[]>(mockSynonymGroups);
  const [synonymStats] = useState<SynonymStats>(mockSynonymStats);
  const [searchTerm, setSearchTerm] = useState('');
  const [typeFilter, setTypeFilter] = useState('all');
  const [categoryFilter, setCategoryFilter] = useState('all');
  const [statusFilter, setStatusFilter] = useState('all');
  const [syncFilter, setSyncFilter] = useState('all');
  const [selectedGroups, setSelectedGroups] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // 搜索和筛选逻辑
  const filteredGroups = synonymGroups.filter(group => {
    const matchesSearch = searchTerm === '' || 
      group.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      group.terms.some(term => term.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesType = typeFilter === 'all' || group.type === typeFilter;
    const matchesCategory = categoryFilter === 'all' || group.category === categoryFilter;
    const matchesStatus = statusFilter === 'all' || group.status === statusFilter;
    const matchesSync = syncFilter === 'all' || group.syncStatus === syncFilter;
    return matchesSearch && matchesType && matchesCategory && matchesStatus && matchesSync;
  });

  // 获取唯一值用于筛选
  const uniqueCategories = [...new Set(synonymGroups.map(group => group.category))];

  // 搜索处理
  const handleSearch = (value: string) => {
    setSearchTerm(value);
  };

  // 筛选处理
  const handleTypeFilter = (value: string) => {
    setTypeFilter(value);
  };

  const handleCategoryFilter = (value: string) => {
    setCategoryFilter(value);
  };

  const handleStatusFilter = (value: string) => {
    setStatusFilter(value);
  };

  const handleSyncFilter = (value: string) => {
    setSyncFilter(value);
  };

  // 组选择处理
  const handleGroupSelect = (groupId: string, checked: boolean) => {
    if (checked) {
      setSelectedGroups(prev => [...prev, groupId]);
    } else {
      setSelectedGroups(prev => prev.filter(id => id !== groupId));
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedGroups(filteredGroups.map(group => group.id));
    } else {
      setSelectedGroups([]);
    }
  };

  // 创建同义词组
  const handleCreateGroup = async () => {
    setIsLoading(true);
    console.log('创建同义词组');
    // 模拟创建延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 同步到Elasticsearch
  const handleSyncToES = async (groupId: string) => {
    setIsLoading(true);
    console.log('同步到Elasticsearch:', groupId);
    // 模拟同步延迟
    await new Promise(resolve => setTimeout(resolve, 3000));
    setIsLoading(false);
  };

  // 批量同步到ES
  const handleBatchSyncToES = async () => {
    setIsLoading(true);
    console.log('批量同步到Elasticsearch:', selectedGroups);
    // 模拟批量同步延迟
    await new Promise(resolve => setTimeout(resolve, 5000));
    setIsLoading(false);
    setSelectedGroups([]);
  };

  // 发布同义词组
  const handlePublishGroup = async (groupId: string) => {
    setIsLoading(true);
    console.log('发布同义词组:', groupId);
    // 模拟发布延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    setIsLoading(false);
  };

  // 删除同义词组
  const handleDeleteGroup = async (groupId: string) => {
    setIsLoading(true);
    console.log('删除同义词组:', groupId);
    // 模拟删除延迟
    await new Promise(resolve => setTimeout(resolve, 1000));
    setIsLoading(false);
  };

  // 导入同义词
  const handleImportSynonyms = async () => {
    setIsLoading(true);
    console.log('导入同义词');
    // 模拟导入延迟
    await new Promise(resolve => setTimeout(resolve, 3000));
    setIsLoading(false);
  };

  // 导出同义词
  const handleExportSynonyms = async () => {
    setIsLoading(true);
    console.log('导出同义词');
    // 模拟导出延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    setIsLoading(false);
  };

  return (
    <div className="min-h-screen bg-background">
      {/* 页面头部操作区域 */}
      <div className="bg-card border-b">
        <div className="container px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-card-foreground">同义词管理</h1>
              <p className="text-sm text-muted-foreground">
                管理Elasticsearch同义词配置，支持热更新和版本控制
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <Button
                variant="outline"
                onClick={handleImportSynonyms}
                disabled={isLoading}
                className="flex items-center space-x-2"
              >
                <Upload className="h-4 w-4" />
                <span>导入</span>
              </Button>
              <Button
                variant="outline"
                onClick={handleExportSynonyms}
                disabled={isLoading}
                className="flex items-center space-x-2"
              >
                <Download className="h-4 w-4" />
                <span>导出</span>
              </Button>
              <Button
                variant="outline"
                onClick={handleBatchSyncToES}
                disabled={isLoading || selectedGroups.length === 0}
                className="flex items-center space-x-2"
              >
                <GitBranch className="h-4 w-4" />
                <span>批量同步 ({selectedGroups.length})</span>
              </Button>
              <Button
                onClick={handleCreateGroup}
                disabled={isLoading}
                className="flex items-center space-x-2"
              >
                <Plus className="h-4 w-4" />
                <span>创建同义词组</span>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6 sm:px-6 lg:px-8">
        {/* 同义词统计概览 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总组数</CardTitle>
              <Hash className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{synonymStats.totalGroups}</div>
              <p className="text-xs text-muted-foreground">
                活跃: {synonymStats.activeGroups} | 草稿: {synonymStats.draftGroups}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总术语数</CardTitle>
              <FileText className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{synonymStats.totalTerms}</div>
              <p className="text-xs text-muted-foreground">
                平均每组: {synonymStats.avgTermsPerGroup} 个
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">分类数</CardTitle>
              <Tag className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{synonymStats.categoriesCount}</div>
              <p className="text-xs text-muted-foreground">
                同义词分类数量
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">平均使用率</CardTitle>
              <CheckCircle className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {synonymGroups.length > 0 
                  ? Math.round(synonymGroups.reduce((acc, group) => acc + (group.usage.successful / group.usage.total) * 100, 0) / synonymGroups.length)
                  : 0}%
              </div>
              <p className="text-xs text-muted-foreground">
                同义词匹配成功率
              </p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Search className="h-5 w-5" />
              <span>同义词搜索和筛选</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
              <Input
                placeholder="搜索组名或术语..."
                value={searchTerm}
                onChange={(e) => handleSearch(e.target.value)}
              />
              <Select value={typeFilter} onValueChange={handleTypeFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="类型" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有类型</SelectItem>
                  <SelectItem value="explicit">显式同义词</SelectItem>
                  <SelectItem value="synonym">对称同义词</SelectItem>
                </SelectContent>
              </Select>
              <Select value={categoryFilter} onValueChange={handleCategoryFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="分类" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有分类</SelectItem>
                  {uniqueCategories.map(category => (
                    <SelectItem key={category} value={category}>{category}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={statusFilter} onValueChange={handleStatusFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有状态</SelectItem>
                  <SelectItem value="active">活跃</SelectItem>
                  <SelectItem value="inactive">停用</SelectItem>
                  <SelectItem value="draft">草稿</SelectItem>
                  <SelectItem value="published">已发布</SelectItem>
                </SelectContent>
              </Select>
              <Select value={syncFilter} onValueChange={handleSyncFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="同步状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有同步状态</SelectItem>
                  <SelectItem value="synced">已同步</SelectItem>
                  <SelectItem value="pending">待同步</SelectItem>
                  <SelectItem value="failed">同步失败</SelectItem>
                  <SelectItem value="syncing">同步中</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 同义词组列表 */}
        <Card>
          <CardHeader>
            <CardTitle>同义词组列表</CardTitle>
            <p className="text-sm text-muted-foreground">
              共 {filteredGroups.length} 个同义词组
              {selectedGroups.length > 0 && ` (已选择 ${selectedGroups.length} 个)`}
              {searchTerm && ` (搜索: "${searchTerm}")`}
              {typeFilter !== 'all' && ` (类型: ${getTypeName(typeFilter)})`}
              {categoryFilter !== 'all' && ` (分类: ${categoryFilter})`}
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
                        checked={filteredGroups.length > 0 && filteredGroups.every(group => selectedGroups.includes(group.id))}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        className="rounded"
                      />
                    </TableHead>
                    <TableHead>组名</TableHead>
                    <TableHead>类型</TableHead>
                    <TableHead>分类</TableHead>
                    <TableHead>术语</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>版本</TableHead>
                    <TableHead>同步状态</TableHead>
                    <TableHead>使用率</TableHead>
                    <TableHead>更新时间</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredGroups.map((group) => (
                    <TableRow key={group.id} className={selectedGroups.includes(group.id) ? 'bg-blue-50' : ''}>
                      <TableCell>
                        <input
                          type="checkbox"
                          checked={selectedGroups.includes(group.id)}
                          onChange={(e) => handleGroupSelect(group.id, e.target.checked)}
                          className="rounded"
                        />
                      </TableCell>
                      <TableCell>
                        <div>
                          <div className="font-medium">{group.name}</div>
                          <div className="text-sm text-gray-500 truncate max-w-xs" title={group.description}>
                            {group.description}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge className={getTypeColor(group.type)}>
                          {getTypeName(group.type)}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">{group.category}</Badge>
                      </TableCell>
                      <TableCell>
                        <div className="text-sm">
                          {group.terms.slice(0, 3).map((term, index) => (
                            <span key={index} className="inline-block bg-gray-100 px-2 py-1 rounded mr-1 mb-1">
                              {term}
                            </span>
                          ))}
                          {group.terms.length > 3 && (
                            <span className="text-gray-500">+{group.terms.length - 3}...</span>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge className={getStatusColor(group.status)}>
                          {getStatusName(group.status)}
                        </Badge>
                      </TableCell>
                      <TableCell className="font-mono text-sm">{group.version}</TableCell>
                      <TableCell>
                        <div className={`flex items-center space-x-1 ${getSyncStatusColor(group.syncStatus)}`}>
                          {getSyncStatusIcon(group.syncStatus)}
                          <span className="text-sm capitalize">{group.syncStatus}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        {group.usage.total > 0 ? (
                          <div className="text-sm">
                            <div>{Math.round((group.usage.successful / group.usage.total) * 100)}%</div>
                            <div className="text-xs text-gray-500">
                              {group.usage.successful}/{group.usage.total}
                            </div>
                          </div>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </TableCell>
                      <TableCell className="font-mono text-sm">{group.updatedAt}</TableCell>
                      <TableCell>
                        <div className="flex items-center space-x-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleSyncToES(group.id)}
                            disabled={isLoading}
                            title="同步到ES"
                          >
                            <GitBranch className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handlePublishGroup(group.id)}
                            disabled={isLoading || group.status === 'published'}
                            title="发布"
                          >
                            <Upload className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            title="编辑"
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          <AlertDialog>
                            <AlertDialogTrigger asChild>
                              <Button
                                variant="ghost"
                                size="sm"
                                disabled={isLoading}
                                title="删除"
                              >
                                <Trash2 className="h-4 w-4 text-red-500" />
                              </Button>
                            </AlertDialogTrigger>
                            <AlertDialogContent>
                              <AlertDialogHeader>
                                <AlertDialogTitle>删除同义词组</AlertDialogTitle>
                                <AlertDialogDescription>
                                  确定要删除同义词组 "{group.name}" 吗？此操作不可撤销，将永久删除所有相关配置。
                                </AlertDialogDescription>
                              </AlertDialogHeader>
                              <AlertDialogFooter>
                                <AlertDialogCancel>取消</AlertDialogCancel>
                                <AlertDialogAction 
                                  onClick={() => handleDeleteGroup(group.id)}
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

export default function SynonymsPage() {
  return (
    <ProtectedRoute>
      <Header />
      <SynonymsContent />
    </ProtectedRoute>
  );
}