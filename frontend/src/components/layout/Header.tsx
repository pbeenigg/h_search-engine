'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { APP_NAME, ROUTES } from '@/lib/constants';
import { ChevronDown, Settings, BarChart3, Users, Smartphone } from 'lucide-react';
import { ThemeSelector } from '@/components/theme/ThemeSelector';
import { UserMenu } from './UserMenu';

export function Header() {
  return (
    <header className="header-nav sticky top-0 z-50">
      <div className="flex h-20 items-center px-6">
        <div className="mr-12 flex items-center space-x-3">
          <div className="w-10 h-10 bg-gradient-to-br from-primary/20 to-primary/10 rounded-xl flex items-center justify-center">
            <BarChart3 className="w-6 h-6 text-primary" />
          </div>
          <h1 className="text-xl font-bold text-foreground bg-gradient-to-r from-foreground to-muted-foreground bg-clip-text">
            {APP_NAME}
          </h1>
        </div>
        
        <nav className="flex items-center space-x-2">
          <Link href={ROUTES.DASHBOARD} className="nav-link flex items-center">
            <BarChart3 className="mr-2 h-4 w-4" />
            仪表盘
          </Link>
          <Link href={ROUTES.USERS} className="nav-link flex items-center">
            <Users className="mr-2 h-4 w-4" />
            用户管理
          </Link>
          <Link href={ROUTES.APPS} className="nav-link flex items-center">
            <Smartphone className="mr-2 h-4 w-4" />
            应用管理
          </Link>
          
          {/* 系统管理下拉菜单 */}
          <DropdownMenu>
            <DropdownMenuTrigger className="nav-link flex items-center">
              系统管理
              <ChevronDown className="ml-1 h-3 w-3" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-52">
              <DropdownMenuItem asChild className="cursor-pointer">
                <Link href={ROUTES.MONITOR} className="w-full flex items-center">
                  📊 系统监控
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild className="cursor-pointer">
                <Link href={ROUTES.CACHE} className="w-full flex items-center">
                  💾 缓存管理
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild className="cursor-pointer">
                <Link href={ROUTES.LOGS} className="w-full flex items-center">
                  📋 日志管理
                </Link>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          {/* 搜索管理下拉菜单 */}
          <DropdownMenu>
            <DropdownMenuTrigger className="nav-link flex items-center">
              搜索管理
              <ChevronDown className="ml-1 h-3 w-3" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-52">
              <DropdownMenuItem asChild className="cursor-pointer">
                <Link href={ROUTES.SYNONYMS} className="w-full flex items-center">
                  🔤 同义词管理
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild className="cursor-pointer">
                <Link href={ROUTES.INDEX} className="w-full flex items-center">
                  📚 索引管理
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild className="cursor-pointer">
                <Link href={ROUTES.SUPPLIERS} className="w-full flex items-center">
                  🏢 供应商管理
                </Link>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>

          {/* 任务管理下拉菜单 */}
          <DropdownMenu>
            <DropdownMenuTrigger className="nav-link flex items-center">
              任务管理
              <ChevronDown className="ml-1 h-3 w-3" />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="w-52">
              <DropdownMenuItem asChild className="cursor-pointer">
                <Link href={ROUTES.SCHEDULER} className="w-full flex items-center">
                  ⏰ 定时任务
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem asChild className="cursor-pointer">
                <Link href={ROUTES.CAMEL} className="w-full flex items-center">
                  🔀 Camel路由
                </Link>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </nav>
        
        <div className="ml-auto flex items-center space-x-3">
          <ThemeSelector />
          <Button variant="outline" size="sm" className="btn">
            <Settings className="mr-2 h-4 w-4" />
            设置
          </Button>
          <UserMenu />
        </div>
      </div>
    </header>
  );
}
