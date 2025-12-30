// 系统监控页面
'use client';

import { useState, useEffect } from 'react';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { Header } from '@/components/layout/Header';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Progress } from '@/components/ui/progress';
import { Badge } from '@/components/ui/badge';
import { Activity, Server, Database, Cpu, HardDrive, Wifi } from 'lucide-react';

interface SystemMetrics {
  cpu: number;
  memory: number;
  disk: number;
  network: number;
  uptime: string;
  status: 'healthy' | 'warning' | 'error';
  responseTime: number;
  throughput: number;
  errorRate: number;
}

interface ServiceStatus {
  name: string;
  status: 'online' | 'offline' | 'warning';
  responseTime: number;
  lastCheck: string;
}

// 模拟系统指标数据
const mockSystemMetrics: SystemMetrics = {
  cpu: 45.6,
  memory: 68.2,
  disk: 34.1,
  network: 23.7,
  uptime: '15天 8小时 32分钟',
  status: 'healthy',
  responseTime: 245,
  throughput: 1250,
  errorRate: 0.02,
};

// 模拟服务状态数据
const mockServiceStatus: ServiceStatus[] = [
  {
    name: '酒店搜索API',
    status: 'online',
    responseTime: 156,
    lastCheck: '2025-11-18 02:20:00',
  },
  {
    name: 'Elasticsearch集群',
    status: 'online',
    responseTime: 89,
    lastCheck: '2025-11-18 02:20:00',
  },
  {
    name: 'Redis缓存',
    status: 'online',
    responseTime: 23,
    lastCheck: '2025-11-18 02:20:00',
  },
  {
    name: '数据库连接池',
    status: 'warning',
    responseTime: 567,
    lastCheck: '2025-11-18 02:20:00',
  },
  {
    name: '邮件服务',
    status: 'online',
    responseTime: 234,
    lastCheck: '2025-11-18 02:20:00',
  },
  {
    name: '日志收集',
    status: 'offline',
    responseTime: 0,
    lastCheck: '2025-11-18 02:19:45',
  },
];

const getStatusColor = (status: string) => {
  switch (status) {
    case 'healthy':
    case 'online':
      return 'bg-green-100 text-green-800';
    case 'warning':
      return 'bg-yellow-100 text-yellow-800';
    case 'error':
    case 'offline':
      return 'bg-red-100 text-red-800';
    default:
      return 'bg-gray-100 text-gray-800';
  }
};

const getStatusName = (status: string) => {
  switch (status) {
    case 'healthy':
      return '健康';
    case 'warning':
      return '警告';
    case 'error':
      return '错误';
    case 'online':
      return '在线';
    case 'offline':
      return '离线';
    default:
      return status;
  }
};

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'healthy':
    case 'online':
      return '🟢';
    case 'warning':
      return '🟡';
    case 'error':
    case 'offline':
      return '🔴';
    default:
      return '⚪';
  }
};

function MonitorContent() {
  const [systemMetrics, setSystemMetrics] = useState<SystemMetrics>(mockSystemMetrics);
  const [serviceStatus, setServiceStatus] = useState<ServiceStatus[]>(mockServiceStatus);
  const [lastUpdate, setLastUpdate] = useState(new Date());

  // 模拟实时数据更新
  useEffect(() => {
    const interval = setInterval(() => {
      // 随机波动模拟实时数据
      setSystemMetrics(prev => ({
        ...prev,
        cpu: Math.max(0, Math.min(100, prev.cpu + (Math.random() - 0.5) * 10)),
        memory: Math.max(0, Math.min(100, prev.memory + (Math.random() - 0.5) * 8)),
        disk: Math.max(0, Math.min(100, prev.disk + (Math.random() - 0.5) * 5)),
        network: Math.max(0, Math.min(100, prev.network + (Math.random() - 0.5) * 15)),
        responseTime: Math.max(50, Math.min(1000, prev.responseTime + (Math.random() - 0.5) * 50)),
        throughput: Math.max(500, Math.min(2000, prev.throughput + (Math.random() - 0.5) * 200)),
        errorRate: Math.max(0, Math.min(5, prev.errorRate + (Math.random() - 0.5) * 0.02)),
      }));
      setLastUpdate(new Date());
    }, 5000); // 每5秒更新一次

    return () => clearInterval(interval);
  }, []);

  // 执行健康检查
  const handleHealthCheck = () => {
    console.log('执行系统健康检查...');
    // 这里可以添加实际的健康检查逻辑
    setLastUpdate(new Date());
  };

  // 获取系统状态颜色
  const getSystemStatusColor = (metrics: SystemMetrics) => {
    const avgUsage = (metrics.cpu + metrics.memory + metrics.disk + metrics.network) / 4;
    if (avgUsage < 70) return 'healthy';
    if (avgUsage < 85) return 'warning';
    return 'error';
  };

  const systemStatus = getSystemStatusColor(systemMetrics);

  return (
    <div className="min-h-screen bg-background">
      {/* 页面头部操作区域 */}
      <div className="bg-card border-b">
        <div className="container px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div>
              <h1 className="text-2xl font-bold text-card-foreground">系统监控</h1>
              <p className="text-sm text-muted-foreground">
                实时监控系统状态和性能指标
                <span className="ml-2 text-xs text-gray-400">
                  最后更新: {lastUpdate.toLocaleTimeString('zh-CN')}
                </span>
              </p>
            </div>
            <div className="flex items-center space-x-4">
              <Button onClick={handleHealthCheck} className="flex items-center space-x-2">
                <Activity className="h-4 w-4" />
                <span>健康检查</span>
              </Button>
              <Badge className={getStatusColor(systemStatus)}>
                系统状态: {getStatusName(systemStatus)} {getStatusIcon(systemStatus)}
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <main className="container py-6 sm:px-6 lg:px-8">
        {/* 系统指标概览 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">CPU 使用率</CardTitle>
              <Cpu className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{systemMetrics.cpu.toFixed(1)}%</div>
              <Progress value={systemMetrics.cpu} className="mt-2" />
              <p className="text-xs text-muted-foreground mt-2">
                {systemMetrics.cpu > 80 ? '高负载' : systemMetrics.cpu > 60 ? '中等负载' : '正常'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">内存使用率</CardTitle>
              <Database className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{systemMetrics.memory.toFixed(1)}%</div>
              <Progress value={systemMetrics.memory} className="mt-2" />
              <p className="text-xs text-muted-foreground mt-2">
                {systemMetrics.memory > 85 ? '内存不足' : systemMetrics.memory > 70 ? '内存偏高' : '内存充足'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">磁盘使用率</CardTitle>
              <HardDrive className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{systemMetrics.disk.toFixed(1)}%</div>
              <Progress value={systemMetrics.disk} className="mt-2" />
              <p className="text-xs text-muted-foreground mt-2">
                {systemMetrics.disk > 90 ? '磁盘空间不足' : systemMetrics.disk > 75 ? '磁盘空间偏少' : '磁盘空间充足'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">网络使用率</CardTitle>
              <Wifi className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{systemMetrics.network.toFixed(1)}%</div>
              <Progress value={systemMetrics.network} className="mt-2" />
              <p className="text-xs text-muted-foreground mt-2">
                {systemMetrics.network > 80 ? '网络拥塞' : systemMetrics.network > 60 ? '网络繁忙' : '网络畅通'}
              </p>
            </CardContent>
          </Card>
        </div>

        {/* 性能指标 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">响应时间</CardTitle>
              <Activity className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{systemMetrics.responseTime}ms</div>
              <p className="text-xs text-muted-foreground">
                {systemMetrics.responseTime > 500 ? '响应较慢' : systemMetrics.responseTime > 200 ? '响应正常' : '响应快速'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">吞吐量</CardTitle>
              <Server className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{systemMetrics.throughput}</div>
              <p className="text-xs text-muted-foreground">请求/分钟</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">错误率</CardTitle>
              <Badge variant="destructive" className="text-xs">%</Badge>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{(systemMetrics.errorRate * 100).toFixed(2)}%</div>
              <p className="text-xs text-muted-foreground">
                {systemMetrics.errorRate > 0.05 ? '错误率偏高' : systemMetrics.errorRate > 0.01 ? '错误率正常' : '错误率很低'}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">系统运行时间</CardTitle>
              <span className="text-2xl">⏱️</span>
            </CardHeader>
            <CardContent>
              <div className="text-lg font-bold">{systemMetrics.uptime}</div>
              <p className="text-xs text-muted-foreground">连续运行时间</p>
            </CardContent>
          </Card>
        </div>

        {/* 服务状态列表 */}
        <Card>
          <CardHeader>
            <CardTitle>服务状态</CardTitle>
            <p className="text-sm text-muted-foreground">
              核心服务运行状态和响应时间监控
            </p>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {serviceStatus.map((service, index) => (
                <div key={index} className="flex items-center justify-between p-4 border rounded-lg">
                  <div className="flex items-center space-x-4">
                    <span className="text-2xl">{getStatusIcon(service.status)}</span>
                    <div>
                      <h3 className="font-semibold">{service.name}</h3>
                      <p className="text-sm text-gray-500">
                        最后检查: {service.lastCheck}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center space-x-4">
                    <div className="text-right">
                      <div className="text-sm font-medium">
                        {service.status === 'offline' ? '0ms' : `${service.responseTime}ms`}
                      </div>
                      <div className="text-xs text-gray-500">
                        {service.status === 'offline' ? '离线' : '响应时间'}
                      </div>
                    </div>
                    <Badge className={getStatusColor(service.status)}>
                      {getStatusName(service.status)}
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </main>
    </div>
  );
}

export default function MonitorPage() {
  return (
    <ProtectedRoute>
      <Header />
      <MonitorContent />
    </ProtectedRoute>
  );
}