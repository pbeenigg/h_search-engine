import axios from 'axios';
import { API_BASE_URL, DEBUG_MODE } from '../constants';

// 创建 axios 实例
export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器
apiClient.interceptors.request.use(
  (config) => {
    // 添加认证 token（如果有）
    // 后端使用 Sa-Token，Token 名称为 Authorization，直接传递 token 值即可
    const token = localStorage.getItem('auth_token');
    if (token) {
      // Sa-Token 支持直接传递 token 值，不需要 Bearer 前缀
      config.headers.Authorization = token;
    }

    if (DEBUG_MODE) {
      console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, config);
    }

    return config;
  },
  (error) => {
    if (DEBUG_MODE) {
      console.error('[API Request Error]', error);
    }
    return Promise.reject(error);
  }
);

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    if (DEBUG_MODE) {
      console.log(`[API Response] ${response.config.method?.toUpperCase()} ${response.config.url}`, response.data);
    }
    return response;
  },
  (error) => {
    if (DEBUG_MODE) {
      console.error('[API Response Error]', error);
    }

    // 处理认证错误
    if (error.response?.status === 401) {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user_info');
      window.location.href = '/login';
    }

    return Promise.reject(error);
  }
);

export default apiClient;
