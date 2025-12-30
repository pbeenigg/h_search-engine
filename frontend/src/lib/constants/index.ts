// 应用常量定义
export const APP_NAME = process.env.NEXT_PUBLIC_APP_NAME || 'HeyTrip Hotel Search Engine';
export const APP_VERSION = process.env.NEXT_PUBLIC_APP_VERSION || '1.0.0';
export const APP_ENV = process.env.NEXT_PUBLIC_APP_ENV || 'development';

// API 配置 - 后端服务端口为 18080
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://192.168.0.105:18080';

// 认证配置
export const AUTH_ENABLED = process.env.NEXT_PUBLIC_AUTH_ENABLED === 'true';
export const SESSION_TIMEOUT = parseInt(process.env.NEXT_PUBLIC_SESSION_TIMEOUT || '3600');

// 功能开关
export const ENABLE_MONITORING = process.env.NEXT_PUBLIC_ENABLE_MONITORING === 'true';
export const ENABLE_LOGS = process.env.NEXT_PUBLIC_ENABLE_LOGS === 'true';
export const ENABLE_CACHE = process.env.NEXT_PUBLIC_ENABLE_CACHE === 'true';

// 开发配置
export const DEBUG_MODE = process.env.NEXT_PUBLIC_DEBUG_MODE === 'true';
export const LOG_LEVEL = process.env.NEXT_PUBLIC_LOG_LEVEL || 'debug';

// 路由常量
export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  DASHBOARD: '/dashboard',
  USERS: '/users',
  APPS: '/apps', 
  CACHE: '/cache',
  MONITOR: '/monitor',
  LOGS: '/logs',
  SUPPLIERS: '/suppliers',
  SYNONYMS: '/synonyms',
  INDEX: '/index',
  SCHEDULER: '/scheduler',
  CAMEL: '/camel',
} as const;

// 用户角色
export const USER_ROLES = {
  ADMIN: 'admin',
  USER: 'user',
  VIEWER: 'viewer',
} as const;

// 响应状态码
export const HTTP_STATUS = {
  OK: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_SERVER_ERROR: 500,
} as const;
