// 认证授权模块类型定义
// 基于后端API文档 /docs/认证授权功能使用指南.md

// ========== 通用响应类型 ==========

// 后端标准响应格式
export interface ApiResponse<T = unknown> {
  code: number;
  msg: string;
  data: T;
  timestamp: number;
}

// 分页信息
export interface PageInfo {
  size: number;
  number: number; // 当前页码 (0-based)
  totalElements: number;
  totalPages: number;
}

// 分页响应格式（匹配后端实际返回结构）
export interface PageResponse<T> {
  content: T[];
  page: PageInfo;
}

// ========== 用户相关类型 ==========

// 用户性别
export type UserSex = 'M' | 'F' | 'U' | 'O';

// 用户信息（从API返回）
export interface User {
  userId: number;
  userName: string;
  userNick: string;
  sex: UserSex;
  timeout?: number;
  appId?: string;
  createAt?: string;
  updateAt?: string;
  createBy?: string;
  updateBy?: string;
}

// 登录请求
export interface LoginRequest {
  username: string;
  password: string;
}

// 登录响应数据
export interface LoginResponseData {
  tokenName: string;
  tokenValue: string;
  userId: number;
  userName: string;
  userNick: string;
}

// 当前用户信息响应
export interface CurrentUserInfo {
  userId: number;
  userName: string;
  userNick: string;
  sex: UserSex;
}

// 创建用户请求
export interface CreateUserRequest {
  userName: string;
  password: string;
  userNick: string;
  sex?: UserSex;
}

// 更新用户请求
export interface UpdateUserRequest {
  userNick?: string;
  sex?: UserSex;
}

// 修改密码请求
export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

// ========== 应用相关类型 ==========

// 应用信息
export interface App {
  appId: string;
  secretKey: string;
  encryptionKey?: string;
  rateLimit: number;
  timeout: number;
  createAt?: string;
  updateAt?: string;
  // 关联用户ID（用于显示）
  userId?: number;
}

// 更新应用请求
export interface UpdateAppRequest {
  rateLimit?: number;
  timeout?: number;
}

// 刷新密钥响应
export interface RefreshSecretResponse {
  appId: string;
  secretKey: string;
}

// ========== 前端扩展类型 ==========

// 前端用户信息（包含额外字段用于UI显示）
export interface FrontendUser {
  id: string;
  username: string;
  userNick: string;
  sex: UserSex;
  role: 'admin' | 'user' | 'viewer'; // 前端角色映射
  createdAt: string;
  email: string; // 必需字段，与 UserInfo 兼容
}

// 前端应用信息（包含额外字段用于UI显示）
export interface FrontendApp {
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

// ========== 类型映射工具 ==========

// 将后端用户映射为前端用户
export function mapUserToFrontend(user: User): FrontendUser {
  return {
    id: user.userId.toString(),
    username: user.userName,
    userNick: user.userNick,
    sex: user.sex,
    role: 'user', // 默认角色，可根据业务需要调整
    createdAt: user.createAt || new Date().toISOString(),
    email: `${user.userName}@example.com`, // 使用用户名生成默认邮箱
  };
}

// 将后端应用映射为前端应用
export function mapAppToFrontend(app: App, userName?: string): FrontendApp {
  return {
    id: app.appId,
    name: userName ? `${userName}的应用` : app.appId,
    appId: app.appId,
    secretKey: app.secretKey,
    status: app.timeout === -1 ? 'active' : app.timeout === 0 ? 'inactive' : 'active',
    rateLimit: app.rateLimit,
    timeout: app.timeout,
    creator: userName || 'unknown',
    createdAt: app.createAt || new Date().toISOString(),
  };
}
