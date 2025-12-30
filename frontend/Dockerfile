# 多阶段构建 Dockerfile for Next.js (静态导出模式)

# 可配置基础镜像（支持通过 --build-arg 覆盖以使用镜像加速）
ARG NODE_IMAGE=node:22-alpine
ARG NGINX_IMAGE=nginx:alpine

# 阶段 1: 构建依赖
FROM ${NODE_IMAGE} AS deps
WORKDIR /app

# 安装 pnpm
RUN npm install -g pnpm

# 复制依赖配置文件
COPY package.json pnpm-lock.yaml ./

# 安装依赖
RUN pnpm install --frozen-lockfile

# 阶段 2: 构建应用
FROM ${NODE_IMAGE} AS builder
WORKDIR /app

# 安装 pnpm
RUN npm install -g pnpm

# 复制依赖
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# 复制前端构建所需的公开环境变量（仅包含 NEXT_PUBLIC_*）
# 优先使用 .env.production；存在时复制为 .env 以供构建读取
RUN if [ -f .env.production ]; then cp .env.production .env; fi

# 构建 Next.js 应用（静态导出到 out 目录）
RUN pnpm build

# 阶段 3: 使用 Nginx 服务静态文件
FROM ${NGINX_IMAGE} AS runner

# 复制 Nginx 配置
COPY --from=builder /app/nginx.conf /etc/nginx/nginx.conf

# 复制构建产物
COPY --from=builder /app/out /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 健康检查：容器内 Nginx 监听 80，检查 /health 返回 200
# 说明：nginx:alpine 内通常是 BusyBox wget，使用 -t/-T/-O 参数更兼容
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -q -t 1 -T 3 -O /dev/null http://127.0.0.1:80/health || exit 1

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]

