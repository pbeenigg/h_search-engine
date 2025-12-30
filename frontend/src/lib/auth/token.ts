// JWT Token 管理工具函数
import { DEBUG_MODE } from '../constants';

// Token 存储键名
const TOKEN_KEY = 'auth_token';
const USER_INFO_KEY = 'user_info';
const TOKEN_EXPIRY_KEY = 'token_expiry';

// Token 类型定义
export interface UserInfo {
  id: string;
  username: string;
  email: string;
  role: 'admin' | 'user' | 'viewer';
  createdAt: string;
}

// Token 管理类
export class TokenManager {
  // 设置 Token
  static setToken(token: string, userInfo: UserInfo, expiresIn: number = 3600) {
    try {
      const expiryTime = Date.now() + (expiresIn * 1000);
      
      localStorage.setItem(TOKEN_KEY, token);
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo));
      localStorage.setItem(TOKEN_EXPIRY_KEY, expiryTime.toString());
      
      if (DEBUG_MODE) {
        console.log('Token stored successfully', {
          token: token.substring(0, 10) + '...',
          userInfo,
          expiryTime: new Date(expiryTime).toISOString(),
        });
      }
    } catch (error) {
      console.error('Failed to store token:', error);
    }
  }

  // 获取 Token
  static getToken(): string | null {
    try {
      const token = localStorage.getItem(TOKEN_KEY);
      const expiryTime = localStorage.getItem(TOKEN_EXPIRY_KEY);
      
      if (!token || !expiryTime) {
        return null;
      }

      // 检查 Token 是否过期
      const now = Date.now();
      const expiry = parseInt(expiryTime);
      
      if (now >= expiry) {
        this.clearToken();
        return null;
      }

      return token;
    } catch (error) {
      console.error('Failed to get token:', error);
      return null;
    }
  }

  // 获取用户信息
  static getUserInfo(): UserInfo | null {
    try {
      const userInfoStr = localStorage.getItem(USER_INFO_KEY);
      return userInfoStr ? JSON.parse(userInfoStr) : null;
    } catch (error) {
      console.error('Failed to get user info:', error);
      return null;
    }
  }

  // 检查 Token 是否有效
  static isTokenValid(): boolean {
    return this.getToken() !== null;
  }

  // 获取剩余有效期（毫秒）
  static getRemainingTime(): number {
    const expiryTime = localStorage.getItem(TOKEN_EXPIRY_KEY);
    if (!expiryTime) return 0;
    
    const expiry = parseInt(expiryTime);
    const now = Date.now();
    
    return Math.max(0, expiry - now);
  }

  // 清除 Token
  static clearToken() {
    try {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_INFO_KEY);
      localStorage.removeItem(TOKEN_EXPIRY_KEY);
      
      if (DEBUG_MODE) {
        console.log('Token cleared successfully');
      }
    } catch (error) {
      console.error('Failed to clear token:', error);
    }
  }

  // 刷新 Token（预留接口）
  static async refreshToken(): Promise<boolean> {
    try {
      const currentToken = this.getToken();
      if (!currentToken) {
        return false;
      }

      // TODO: 实现实际的 Token 刷新逻辑
      // 这里应该调用后端 API 来刷新 Token
      
      if (DEBUG_MODE) {
        console.log('Token refresh requested');
      }
      
      return true;
    } catch (error) {
      console.error('Failed to refresh token:', error);
      return false;
    }
  }
}

// 便捷的导出函数
export const tokenManager = {
  set: TokenManager.setToken.bind(TokenManager),
  get: TokenManager.getToken.bind(TokenManager),
  getUser: TokenManager.getUserInfo.bind(TokenManager),
  isValid: TokenManager.isTokenValid.bind(TokenManager),
  getRemainingTime: TokenManager.getRemainingTime.bind(TokenManager),
  clear: TokenManager.clearToken.bind(TokenManager),
  refresh: TokenManager.refreshToken.bind(TokenManager),
};

export default TokenManager;
