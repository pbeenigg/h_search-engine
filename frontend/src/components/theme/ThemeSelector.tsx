'use client';

import { useTheme } from '@/hooks/useTheme';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Palette, Check } from 'lucide-react';

const themes = [
  { id: 'blue' as const, name: '蓝色主题', color: '#3b82f6' },
  { id: 'green' as const, name: '绿色主题', color: '#10b981' },
  { id: 'purple' as const, name: '紫色主题', color: '#8b5cf6' },
];

export function ThemeSelector() {
  const { theme, setTheme } = useTheme();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="sm" className="btn">
          <Palette className="mr-2 h-4 w-4" />
          主题
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="dropdown-menu">
        <div className="p-3">
          <p className="text-sm font-medium text-muted-foreground mb-3">选择主题色彩</p>
          {themes.map((themeOption) => (
            <DropdownMenuItem
              key={themeOption.id}
              onClick={() => setTheme(themeOption.id)}
              className="flex items-center justify-between p-3 rounded-lg cursor-pointer transition-all duration-200 hover:bg-muted/50"
            >
              <div className="flex items-center space-x-3">
                <div
                  className="w-6 h-6 rounded-full border-2 border-border/50 shadow-sm"
                  style={{ backgroundColor: themeOption.color }}
                />
                <span className="font-medium">{themeOption.name}</span>
              </div>
              {theme === themeOption.id && (
                <div className="w-6 h-6 bg-primary rounded-full flex items-center justify-center">
                  <Check className="h-3 w-3 text-primary-foreground" />
                </div>
              )}
            </DropdownMenuItem>
          ))}
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}