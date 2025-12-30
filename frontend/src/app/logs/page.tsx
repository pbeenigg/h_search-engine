// 日志管理页面
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
  Download, 
  FileText, 
  Search, 
  Filter, 
  Calendar,
  User,
  AlertCircle,
  Info,
  CheckCircle,
  XCircle,
  RefreshCw
} from 'lucide-react';

interface LogEntry {
  id: string;
  timestamp: string;
  level: 'INFO' | 'WARN' | 'ERROR' | 'DEBUG';
  module: string;
  user?: string;
  message: string;
  details?: string;
  ip?: string;
  userAgent?: string;
  duration?: number;
  status?: number;
}

interface LogStats {
  totalLogs: number;
  infoCount: number;
  warnCount: number;
  errorCount: number;
  debugCount: number;
  todayCount: number;
}

// 模拟日志数据
const mockLogEntries: LogEntry[] = [
  {
    id: '1',
    timestamp: '2025-11-18 03:15:23',
    level: 'INFO',
    module: 'hotel-search',
    user: 'admin',
    message: '用户登录成功',
    details: '用户 admin 从 192.168.1.100 成功登录系统',
    ip: '192.168.1.100',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    duration: 234,
    status: 200,
  },
  {
    id: '2',
    timestamp: '2025-11-18 03:14:45',
    level: 'WARN',
    module: 'cache-manager',
    message: '缓存命中率低于阈值',
    details: '当前缓存命中率为 67.3%，低于期望的 80%',
    duration: 1200,
    status: 206,
  },
  {
    id: '3',
    timestamp: '2025-11-18 03:14:12',
    level: 'ERROR',
    module: 'api-gateway',
    user: 'api-client-001',
    message: '第三方API调用失败',
    details: '调用酒店供应商API超时，请检查网络连接',
    ip: '10.0.1.50',
    duration: 30000,
    status: 504,
  },
  {
    id: '4',
    timestamp: '2025-11-18 03:13:56',
    level: 'INFO',
    module: 'data-sync',
    message: '数据同步任务开始执行',
    details: '开始同步 360万条酒店数据，预计耗时 45 分钟',
    duration: 2700000,
  },
  {
    id: '5',
    timestamp: '2025-11-18 03:13:30',
    level: 'DEBUG',
    module: 'search-engine',
    user: 'user',
    message: '执行酒店搜索查询',
    details: '查询参数: location=北京, checkin=2025-12-01, checkout=2025-12-03, guests=2',
    ip: '192.168.1.105',
    userAgent: 'HeyTrip/1.2.3 (iOS 15.0)',
    duration: 156,
    status: 200,
  },
  {
    id: '6',
    timestamp: '2025-11-18 03:12:45',
    level: 'ERROR',
    module: 'user-service',
    message: '用户注册失败',
    details: '邮箱格式无效: invalid-email',
    ip: '203.45.67.89',
    duration: 89,
    status: 400,
  },
  {
    id: '7',
    timestamp: '2025-11-18 03:12:18',
    level: 'INFO',
    module: 'monitoring',
    message: '系统健康检查完成',
    details: '所有服务运行正常，响应时间: API(245ms), DB(23ms), Cache(12ms)',
    duration: 1234,
    status: 200,
  },
  {
    id: '8',
    timestamp: '2025-11-18 03:11:52',
    level: 'WARN',
    module: 'rate-limiter',
    user: 'api-client-002',
    message: 'API调用频率超过限制',
    details: 'AppID web987654321 在 1 分钟内调用了 500 次，超出限制 500',
    ip: '172.16.1.200',
    duration: 45,
    status: 429,
  },
];

const mockLogStats: LogStats = {
  totalLogs: 125847,
  infoCount: 89234,
  warnCount: 28456,
  errorCount: 8157,
  debugCount: 0,
  todayCount: 1247,
};

const getLevelColor = (level: string) => {
  switch (level) {
    case 'INFO':
      return 'bg-blue-100 text-blue-800';
    case 'WARN':
      return 'bg-yellow-100 text-yellow-800';
    case 'ERROR':
      return 'bg-red-100 text-red-800';
    case 'DEBUG':
      return 'bg-gray-100 text-gray-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getLevelIcon = (level: string) => {
  switch (level) {
    case 'INFO':
      return <Info className="h-3 w-3" />;
    case 'WARN':
      return <AlertCircle className="h-3 w-3" />;
    case 'ERROR':
      return <XCircle className="h-3 w-3" />;
    case 'DEBUG':
      return <CheckCircle className="h-3 w-3" />;
    default:
      return <FileText className="h-3 w-3" />;
  }
};

const getLevelName = (level: string) => {
  switch (level) {
    case 'INFO':
      return '信息';
    case 'WARN':
      return '警告';
    case 'ERROR':
      return '错误';
    case 'DEBUG':
      return '调试';
    default:
      return level;
  }
};

function LogsContent() {
  const [logEntries] = useState<LogEntry[]>(mockLogEntries);
  const [logStats] = useState<LogStats>(mockLogStats);
  const [searchTerm, setSearchTerm] = useState('');
  const [levelFilter, setLevelFilter] = useState('all');
  const [moduleFilter, setModuleFilter] = useState('all');
  const [userFilter, setUserFilter] = useState('all');
  const [timeRange, setTimeRange] = useState('1h');
  const [selectedLogs, setSelectedLogs] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // 搜索日志
  const handleSearch = (value: string) => {
    setSearchTerm(value);
  };

  // 筛选日志
  const handleLevelFilter = (value: string) => {
    setLevelFilter(value);
  };

  const handleModuleFilter = (value: string) => {
    setModuleFilter(value);
  };

  const handleUserFilter = (value: string) => {
    setUserFilter(value);
  };

  const handleTimeRange = (value: string) => {
    setTimeRange(value);
  };

  // 选择日志
  const handleLogSelect = (logId: string, checked: boolean) => {
    if (checked) {
      setSelectedLogs(prev => [...prev, logId]);
    } else {
      setSelectedLogs(prev => prev.filter(id => id !== logId));
    }
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelectedLogs(filteredLogs.map(log => log.id));
    } else {
      setSelectedLogs([]);
    }
  };

  // 筛选逻辑
  const filteredLogs = logEntries.filter(log => {
    const matchesSearch = searchTerm === '' || 
      log.message.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (log.details && log.details.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesLevel = levelFilter === 'all' || log.level === levelFilter;
    const matchesModule = moduleFilter === 'all' || log.module === moduleFilter;
    const matchesUser = userFilter === 'all' || (log.user && log.user === userFilter);
    return matchesSearch && matchesLevel && matchesModule && matchesUser;
  });

  // 获取唯一值用于筛选
  const uniqueModules = [...new Set(logEntries.map(log => log.module))];
  const uniqueUsers = [...new Set(logEntries.filter(log => log.user).map(log => log.user!))];

  // 导出日志
  const handleExportLogs = async (format: 'csv' | 'json') => {
    setIsLoading(true);
    
    // 模拟导出延迟
    await new Promise(resolve => setTimeout(resolve, 2000));
    
    const logsToExport = selectedLogs.length > 0 
      ? logEntries.filter(log => selectedLogs.includes(log.id))
      : filteredLogs;
    
    let content = '';
    let filename = '';
    
    if (format === 'json') {
      content = JSON.stringify(logsToExport, null, 2);
      filename = `logs_${new Date().toISOString().slice(0, 10)}.json`;
    } else {
      // CSV格式
      const headers = ['时间', '级别', '模块', '用户', '消息', 'IP地址', '响应时间', '状态码'];
      const rows = logsToExport.map(log => [
        log.timestamp,
        getLevelName(log.level),
        log.module,
        log.user || '',
        log.message,
        log.ip || '',
        log.duration ? `${log.duration}ms` : '',
        log.status || ''
      ]);
      
      content = [headers, ...rows].map(row => row.join(',')).join('\n');
      filename = `logs_${new Date().toISOString().slice(0, 10)}.csv`;
    }
    
    // 下载文件
    const blob = new Blob([content], { type: format === 'json' ? 'application/json' : 'text/csv' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    
    setIsLoading(false);
    setSelectedLogs([]);
  };

  // 刷新日志
  const handleRefreshLogs = async () => {
    setIsLoading(true);
    // 模拟刷新延迟
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
              <h1 className="text-2xl font-bold text-card-foreground">日志管理</h1>
              <p className="text-sm text-muted-foreground">
                查看和分析系统运行日志，支持搜索、筛选和导出
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <Button
                variant="outline"
                onClick={handleRefreshLogs}
                disabled={isLoading}
                className="flex items-center space-x-2"
              >
                <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
                <span>刷新</span>
              </Button>
              <Button
                variant="outline"
                onClick={() => handleExportLogs('csv')}
                disabled={isLoading || selectedLogs.length === 0}
                className="flex items-center space-x-2"
              >
                <Download className="h-4 w-4" />
                <span>导出 CSV</span>
              </Button>
              <Button
                variant="outline"
                onClick={() => handleExportLogs('json')}
                disabled={isLoading || selectedLogs.length === 0}
                className="flex items-center space-x-2"
              >
                <Download className="h-4 w-4" />
                <span>导出 JSON</span>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6 sm:px-6 lg:px-8">
        {/* 日志统计概览 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">总日志数</CardTitle>
              <FileText className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{logStats.totalLogs.toLocaleString()}</div>
              <p className="text-xs text-muted-foreground">
                今日新增: {logStats.todayCount}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">信息日志</CardTitle>
              <Info className="h-4 w-4 text-blue-500" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-blue-600">{logStats.infoCount.toLocaleString()}</div>
              <p className="text-xs text-muted-foreground">INFO 级别</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">警告日志</CardTitle>
              <AlertCircle className="h-4 w-4 text-yellow-500" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-yellow-600">{logStats.warnCount.toLocaleString()}</div>
              <p className="text-xs text-muted-foreground">WARN 级别</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">错误日志</CardTitle>
              <XCircle className="h-4 w-4 text-red-500" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-red-600">{logStats.errorCount.toLocaleString()}</div>
              <p className="text-xs text-muted-foreground">ERROR 级别</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">调试日志</CardTitle>
              <CheckCircle className="h-4 w-4 text-gray-500" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-muted-foreground">{logStats.debugCount.toLocaleString()}</div>
              <p className="text-xs text-muted-foreground">DEBUG 级别</p>
            </CardContent>
          </Card>
        </div>

        {/* 搜索和筛选 */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle className="flex items-center space-x-2">
              <Search className="h-5 w-5" />
              <span>日志搜索和筛选</span>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
              <div>
                <Input
                  placeholder="搜索日志内容..."
                  value={searchTerm}
                  onChange={(e) => handleSearch(e.target.value)}
                  className="w-full"
                />
              </div>
              <Select value={levelFilter} onValueChange={handleLevelFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="日志级别" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有级别</SelectItem>
                  <SelectItem value="INFO">信息</SelectItem>
                  <SelectItem value="WARN">警告</SelectItem>
                  <SelectItem value="ERROR">错误</SelectItem>
                  <SelectItem value="DEBUG">调试</SelectItem>
                </SelectContent>
              </Select>
              <Select value={moduleFilter} onValueChange={handleModuleFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="模块" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有模块</SelectItem>
                  {uniqueModules.map(module => (
                    <SelectItem key={module} value={module}>{module}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={userFilter} onValueChange={handleUserFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="用户" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">所有用户</SelectItem>
                  {uniqueUsers.map(user => (
                    <SelectItem key={user} value={user}>{user}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select value={timeRange} onValueChange={handleTimeRange}>
                <SelectTrigger>
                  <SelectValue placeholder="时间范围" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1h">最近1小时</SelectItem>
                  <SelectItem value="6h">最近6小时</SelectItem>
                  <SelectItem value="24h">最近24小时</SelectItem>
                  <SelectItem value="7d">最近7天</SelectItem>
                  <SelectItem value="30d">最近30天</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* 日志列表 */}
        <Card>
          <CardHeader>
            <CardTitle>日志列表</CardTitle>
            <p className="text-sm text-muted-foreground">
              共 {filteredLogs.length} 条日志
              {selectedLogs.length > 0 && ` (已选择 ${selectedLogs.length} 条)`}
              {searchTerm && ` (搜索: "${searchTerm}")`}
              {levelFilter !== 'all' && ` (级别: ${getLevelName(levelFilter)})`}
              {moduleFilter !== 'all' && ` (模块: ${moduleFilter})`}
              {userFilter !== 'all' && ` (用户: ${userFilter})`}
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
                        checked={filteredLogs.length > 0 && filteredLogs.every(log => selectedLogs.includes(log.id))}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        className="rounded"
                      />
                    </TableHead>
                    <TableHead>时间</TableHead>
                    <TableHead>级别</TableHead>
                    <TableHead>模块</TableHead>
                    <TableHead>用户</TableHead>
                    <TableHead>消息</TableHead>
                    <TableHead>IP地址</TableHead>
                    <TableHead>响应时间</TableHead>
                    <TableHead>状态码</TableHead>
                    <TableHead>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredLogs.map((log) => (
                    <TableRow key={log.id} className={selectedLogs.includes(log.id) ? 'bg-blue-50' : ''}>
                      <TableCell>
                        <input
                          type="checkbox"
                          checked={selectedLogs.includes(log.id)}
                          onChange={(e) => handleLogSelect(log.id, e.target.checked)}
                          className="rounded"
                        />
                      </TableCell>
                      <TableCell className="font-mono text-sm">{log.timestamp}</TableCell>
                      <TableCell>
                        <Badge className={getLevelColor(log.level)}>
                          <span className="flex items-center space-x-1">
                            {getLevelIcon(log.level)}
                            <span>{getLevelName(log.level)}</span>
                          </span>
                        </Badge>
                      </TableCell>
                      <TableCell className="font-medium">{log.module}</TableCell>
                      <TableCell>{log.user || '-'}</TableCell>
                      <TableCell className="max-w-md truncate" title={log.message}>
                        {log.message}
                      </TableCell>
                      <TableCell className="font-mono text-sm">{log.ip || '-'}</TableCell>
                      <TableCell>{log.duration ? `${log.duration}ms` : '-'}</TableCell>
                      <TableCell>
                        {log.status && (
                          <Badge variant={log.status >= 400 ? 'destructive' : 'secondary'}>
                            {log.status}
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell>
                        <AlertDialog>
                          <AlertDialogTrigger asChild>
                            <Button variant="ghost" size="sm">
                              查看详情
                            </Button>
                          </AlertDialogTrigger>
                          <AlertDialogContent className="max-w-2xl">
                            <AlertDialogHeader>
                              <AlertDialogTitle>日志详情</AlertDialogTitle>
                            </AlertDialogHeader>
                            <div className="space-y-4">
                              <div className="grid grid-cols-2 gap-4 text-sm">
                                <div>
                                  <strong>时间:</strong> {log.timestamp}
                                </div>
                                <div>
                                  <strong>级别:</strong> {getLevelName(log.level)}
                                </div>
                                <div>
                                  <strong>模块:</strong> {log.module}
                                </div>
                                <div>
                                  <strong>用户:</strong> {log.user || '-'}
                                </div>
                                <div>
                                  <strong>IP地址:</strong> {log.ip || '-'}
                                </div>
                                <div>
                                  <strong>响应时间:</strong> {log.duration ? `${log.duration}ms` : '-'}
                                </div>
                                <div>
                                  <strong>状态码:</strong> {log.status || '-'}
                                </div>
                              </div>
                              <div>
                                <strong>消息:</strong>
                                <p className="mt-1 p-2 bg-background rounded text-sm">{log.message}</p>
                              </div>
                              {log.details && (
                                <div>
                                  <strong>详细信息:</strong>
                                  <Textarea
                                    value={log.details}
                                    readOnly
                                    className="mt-1"
                                    rows={4}
                                  />
                                </div>
                              )}
                              {log.userAgent && (
                                <div>
                                  <strong>用户代理:</strong>
                                  <p className="mt-1 p-2 bg-background rounded text-sm break-all">
                                    {log.userAgent}
                                  </p>
                                </div>
                              )}
                            </div>
                            <AlertDialogFooter>
                              <AlertDialogCancel>关闭</AlertDialogCancel>
                            </AlertDialogFooter>
                          </AlertDialogContent>
                        </AlertDialog>
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

export default function LogsPage() {
  return (
    <ProtectedRoute>
      <Header />
      <LogsContent />
    </ProtectedRoute>
  );
}