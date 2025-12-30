// 仪表盘页面
'use client';

import { useAuthState, useAuthActions } from '@/hooks/auth/useAuth';
import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { Header } from '@/components/layout/Header';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { useRouter } from 'next/navigation';
import { Search, ExternalLink } from 'lucide-react';

function DashboardContent() {
  const { user, isAuthenticated } = useAuthState();
  const { logout } = useAuthActions();
  const router = useRouter();

  const handleLogout = () => {
    logout();
  };

  const quickActions = [
    {
      title: '用户管理',
      description: '管理系统用户和权限',
      href: '/users',
      icon: '👥',
    },
    {
      title: '应用管理',
      description: '管理应用配置和API密钥',
      href: '/apps',
      icon: '📱',
    },
    {
      title: '系统监控',
      description: '监控系统运行状态',
      href: '/monitor',
      icon: '📊',
    },
    {
      title: '日志管理',
      description: '查看系统日志记录',
      href: '/logs',
      icon: '📋',
    },
    {
      title: '缓存管理',
      description: '管理系统缓存',
      href: '/cache',
      icon: '💾',
    },
    {
      title: '供应商管理',
      description: '管理数据供应商',
      href: '/suppliers',
      icon: '🏢',
    },
  ];

  const stats = [
    {
      title: '总用户数',
      value: '1,234',
      change: '+12%',
      icon: '👥',
    },
    {
      title: '活跃应用',
      value: '56',
      change: '+3%',
      icon: '📱',
    },
    {
      title: '今日搜索',
      value: '8,901',
      change: '+24%',
      icon: '🔍',
    },
    {
      title: '系统状态',
      value: '正常',
      change: '100%',
      icon: '✅',
    },
  ];

  return (
    <>
      <Header />
      <div className="min-h-screen">
        {/* Main Content */}
        <main className="container mx-auto py-8">
          {/* 欢迎区域 */}
          <div className="mb-12">
            <h2 className="page-title text-4xl font-bold mb-4">
              欢迎回来，{user?.username}!
            </h2>
            <p className="text-muted-foreground text-lg">
              这里是您的系统管理仪表盘，您可以在这里快速访问各种管理功能。
            </p>
          </div>

          {/* 酒店搜索演示入口 */}
          <Card className="mb-12 overflow-hidden border-0 bg-gradient-to-r from-blue-500 to-purple-600">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-16 h-16 bg-white/20 rounded-2xl flex items-center justify-center">
                    <Search className="w-8 h-8 text-white" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-white mb-1">
                      酒店搜索功能演示
                    </h3>
                    <p className="text-white/80">
                      体验智能酒店搜索，支持关键词搜索、热门推荐等功能
                    </p>
                  </div>
                </div>
                <Button
                  onClick={() => router.push('/search-demo')}
                  variant="secondary"
                  size="lg"
                  className="bg-white text-blue-600 hover:bg-white/90"
                >
                  进入搜索演示
                  <ExternalLink className="ml-2 w-4 h-4" />
                </Button>
              </div>
            </CardContent>
          </Card>

        {/* 统计卡片 */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          {stats.map((stat, index) => (
            <Card key={index} className="stats-card">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-3">
                <CardTitle className="text-sm font-medium text-muted-foreground">
                  {stat.title}
                </CardTitle>
                <span className="text-3xl">{stat.icon}</span>
              </CardHeader>
              <CardContent>
                <div className="text-3xl font-bold text-foreground mb-2">{stat.value}</div>
                <p className="text-xs text-green-600 font-medium">{stat.change}</p>
              </CardContent>
            </Card>
          ))}
        </div>

        {/* 快捷操作 */}
        <div className="mb-12">
          <h3 className="text-2xl font-semibold mb-6 text-foreground">快捷操作</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {quickActions.map((action, index) => (
              <div
                key={index}
                className="quick-action-card"
                onClick={() => router.push(action.href)}
              >
                <Card className="h-full border-0">
                  <CardHeader className="pb-4">
                    <div className="flex items-center space-x-4">
                      <span className="text-4xl">{action.icon}</span>
                      <CardTitle className="text-xl text-foreground">{action.title}</CardTitle>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <p className="text-muted-foreground">{action.description}</p>
                  </CardContent>
                </Card>
              </div>
            ))}
          </div>
        </div>

        {/* 系统状态和活动 */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <Card className="card">
            <CardHeader>
              <CardTitle className="text-xl text-foreground">系统状态</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-6">
                <div className="flex items-center justify-between p-3 rounded-lg bg-muted/20">
                  <span className="text-muted-foreground font-medium">数据库</span>
                  <span className="flex items-center text-green-600">
                    <span className="w-3 h-3 bg-green-500 rounded-full mr-3 animate-pulse"></span>
                    正常
                  </span>
                </div>
                <div className="flex items-center justify-between p-3 rounded-lg bg-muted/20">
                  <span className="text-muted-foreground font-medium">搜索服务</span>
                  <span className="flex items-center text-green-600">
                    <span className="w-3 h-3 bg-green-500 rounded-full mr-3 animate-pulse"></span>
                    正常
                  </span>
                </div>
                <div className="flex items-center justify-between p-3 rounded-lg bg-muted/20">
                  <span className="text-muted-foreground font-medium">缓存服务</span>
                  <span className="flex items-center text-green-600">
                    <span className="w-3 h-3 bg-green-500 rounded-full mr-3 animate-pulse"></span>
                    正常
                  </span>
                </div>
                <div className="flex items-center justify-between p-3 rounded-lg bg-muted/20">
                  <span className="text-muted-foreground font-medium">API网关</span>
                  <span className="flex items-center text-green-600">
                    <span className="w-3 h-3 bg-green-500 rounded-full mr-3 animate-pulse"></span>
                    正常
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="card">
            <CardHeader>
              <CardTitle className="text-xl text-foreground">最近活动</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-6">
                <div className="flex items-center space-x-4 p-3 rounded-lg bg-muted/20 hover:bg-muted/30 transition-colors">
                  <span className="text-3xl">🔍</span>
                  <div>
                    <p className="text-sm font-medium text-foreground">搜索请求</p>
                    <p className="text-xs text-muted-foreground">2分钟前</p>
                  </div>
                </div>
                <div className="flex items-center space-x-4 p-3 rounded-lg bg-muted/20 hover:bg-muted/30 transition-colors">
                  <span className="text-3xl">👤</span>
                  <div>
                    <p className="text-sm font-medium text-foreground">用户登录</p>
                    <p className="text-xs text-muted-foreground">5分钟前</p>
                  </div>
                </div>
                <div className="flex items-center space-x-4 p-3 rounded-lg bg-muted/20 hover:bg-muted/30 transition-colors">
                  <span className="text-3xl">📊</span>
                  <div>
                    <p className="text-sm font-medium text-foreground">数据同步</p>
                    <p className="text-xs text-muted-foreground">1小时前</p>
                  </div>
                </div>
                <div className="flex items-center space-x-4 p-3 rounded-lg bg-muted/20 hover:bg-muted/30 transition-colors">
                  <span className="text-3xl">🔧</span>
                  <div>
                    <p className="text-sm font-medium text-foreground">系统维护</p>
                    <p className="text-xs text-muted-foreground">2小时前</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
    </>
  );
}

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <DashboardContent />
    </ProtectedRoute>
  );
}
