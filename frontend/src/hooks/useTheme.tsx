'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';

type Theme = 'blue' | 'green' | 'purple';

interface ThemeContextType {
  theme: Theme;
  setTheme: (theme: Theme) => void;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setTheme] = useState<Theme>(() => {
    // 从localStorage读取保存的主题，如果没有则默认为'blue'
    if (typeof window !== 'undefined') {
      const savedTheme = localStorage.getItem('hotel-search-theme') as Theme;
      return savedTheme || 'blue';
    }
    return 'blue';
  });

  useEffect(() => {
    // 移除所有主题类
    document.documentElement.classList.remove('theme-blue', 'theme-green', 'theme-purple');
    // 添加当前主题类
    document.documentElement.classList.add(`theme-${theme}`);
    // 保存到localStorage
    localStorage.setItem('hotel-search-theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    const themes: Theme[] = ['blue', 'green', 'purple'];
    const currentIndex = themes.indexOf(theme);
    const nextIndex = (currentIndex + 1) % themes.length;
    setTheme(themes[nextIndex]);
  };

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
}