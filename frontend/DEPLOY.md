# 前端 Docker 部署说明

## 一键部署脚本

本项目提供了基于 Docker 的一键部署脚本 `deploy.sh`，用于将前端项目自动构建为 Docker 镜像、推送到阿里云镜像仓库并在远程服务器上运行。

## 部署架构

```
本地开发环境
    ↓ (docker build)
Docker 镜像
    ↓ (docker push)
阿里云镜像仓库 (hk-store-registry.cn-hongkong.cr.aliyuncs.com)
    ↓ (docker pull)
远程生产服务器
    ↓ (docker run)
运行中的容器 (Nginx + 静态文件)
```

## 前置要求

1. **本地环境**：
   - Docker 已安装并运行
   - SSH 客户端已安装

2. **可选工具**（推荐安装以实现自动登录）：
   - macOS: `brew install hudochenkov/sshpass/sshpass`
   - Linux: `apt-get install sshpass` 或 `yum install sshpass`

3. **远程服务器**：
   - 已安装 Docker
   - 有访问阿里云镜像仓库的权限
   - 开放了容器运行端口（默认 80 或 3000）

## 配置

部署配置存储在 `.env.deploy` 文件中：

```bash
# 部署服务器配置
UPLOAD_HOST=dsjnvydxri-public.bastionhost.aliyuncs.com
UPLOAD_USER=root
UPLOAD_PORT=60022
UPLOAD_REMOTE_DIR=/root/pax/hotel-search/web
UPLOAD_PASSWORD=MDNiOWFjMTZiMjhjNDgxYWExMjc5OGNkMzJmYWZjMTAyYjA3NjAyNA

# Docker 镜像配置
DOCKER_REGISTRY=hk-store-registry.cn-hongkong.cr.aliyuncs.com
DOCKER_USERNAME=xw_german2023@1167092269249029
DOCKER_PASSWORD=!t1D@P3RP5Fj%V#N6X&Uby
DOCKER_IMAGE_NAME=web/pax
DOCKER_CONTAINER_NAME=hotel-search-web
DOCKER_PORT=3000
```

**安全提示**:

- 不要将 `.env.deploy` 文件提交到 Git 仓库
- 建议使用 SSH 密钥认证替代密码认证
- 已添加到 `.gitignore` 文件中

## 使用方法

### 基础用法

在 `frontend` 目录下运行：

```bash
./deploy.sh
```

### 部署流程

脚本会自动执行以下步骤：

1. **构建 Docker 镜像** - 基于 Dockerfile 构建前端镜像
2. **登录阿里云镜像仓库** - 使用配置的凭据登录
3. **推送镜像** - 推送带版本号和 latest 标签的镜像
4. **SSH 远程部署** - 在服务器上执行：
   - 登录阿里云镜像仓库
   - 拉取最新镜像
   - 停止并删除旧容器
   - 启动新容器
   - 清理未使用的镜像
5. **清理确认** - 可选择是否清理本地旧镜像

### 镜像版本命名规范

镜像标签格式：`hotel-search-we_1.0.0_YYYYMMDDHHmmss`

例如：`hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:hotel-search-web_1.0.0_20231224153045`

每次部署会自动生成时间戳，同时更新 `latest` 标签。

## Docker 镜像说明

### 镜像构建

镜像采用多阶段构建优化：

1. **deps 阶段** - 安装 pnpm 和项目依赖
2. **builder 阶段** - 构建 Next.js 应用（静态导出）
3. **runner 阶段** - 使用 Nginx Alpine 镜像服务静态文件

### 镜像特性

- **轻量级**：基于 Alpine Linux，镜像体积小
- **安全**：只包含运行时必需的文件
- **优化**：Gzip 压缩、静态资源缓存
- **健康检查**：内置健康检查端点

## 部署的文件和结构

Docker 镜像包含：

- `out/` - Next.js 静态导出文件
- `nginx.conf` - Nginx 配置文件

远程服务器只需运行 Docker 容器，无需额外配置。

## 故障排查

### 1. Docker 构建失败

```bash
# 手动测试 Docker 构建
cd frontend
docker build -t test-build .

# 查看构建日志
docker build --progress=plain -t test-build .
```

#### Docker Hub 连接超时（拉取基础镜像失败）

报错示例：`failed to fetch anonymous token ... i/o timeout` 或 `load metadata for docker.io/library/node:22-alpine`。

解决方案（使用镜像加速源）：

- 将基础镜像切换到阿里云镜像加速源（示例：`registry.cn-hangzhou.aliyuncs.com/library`）。
- 本项目的 Dockerfile 支持通过构建参数覆盖基础镜像。

方法一：直接使用部署脚本的内置支持（推荐）

在 `frontend/.env.deploy` 中设置：

```bash
# 使用阿里云镜像加速源
DOCKER_BASE_MIRROR=registry.cn-hangzhou.aliyuncs.com/library
```

然后执行：

```bash
cd frontend
./deploy.sh
```

方法二：手动构建时指定构建参数

```bash
docker buildx build \
   --platform linux/amd64 \
   --build-arg NODE_IMAGE=registry.cn-hangzhou.aliyuncs.com/library/node:22-alpine \
   --build-arg NGINX_IMAGE=registry.cn-hangzhou.aliyuncs.com/library/nginx:alpine \
   -t hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:manual \
   --load .
```

### 2. 镜像推送失败

```bash
# 测试阿里云镜像仓库登录
docker login hk-store-registry.cn-hongkong.cr.aliyuncs.com

# 手动推送镜像
docker tag test-build hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:test
docker push hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:test
```

### 2. SSH 连接失败

```bash
# 测试 SSH 连接
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com

# 或使用密码登录
sshpass -p 'YOUR_PASSWORD' ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com
```

### 3. 权限问题

确保远程目录有写权限：

### 3. SSH 连接失败

```bash
# 测试 SSH 连接
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com

# 或使用密码登录
sshpass -p 'YOUR_PASSWORD' ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com
```

### 4. 容器启动失败

````bash
# SSH 登录服务器查看容器日志
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com
docker logs hotel-search-web

# 查看容器状态
docker ps -a | grep hotel-search-web

# 检查端口占用
netstat -tlnp | grep 3000

### 5. 健康检查失败

现象：容器启动后状态为 `unhealthy`，或编排环境标记健康检查失败。

检查步骤：

```bash
# 容器内或宿主机（按映射端口）访问健康检查端点
docker exec -it hotel-search-web wget -qO- http://localhost:80/health

# 如果宿主机映射端口为 3000
curl -f http://localhost:3000/health
```

常见原因与处理：

- 路径错误：健康检查已改为 /health，确保探针指向该路径。
- 端口映射：宿主机访问应使用映射端口（默认 3000 -> 容器 80）。
- 网络策略/防火墙：确认未拦截容器内到自身 80 端口的访问。
- 构建产物缺失：健康检查与静态文件无直接依赖，但容器应已正常启动 Nginx。

### 5. exec format error（入口脚本格式错误）

报错示例：`exec /docker-entrypoint.sh: exec format error`

常见原因：本地构建镜像的架构与远程服务器不一致（例如在 macOS/ARM64 上构建 ARM 镜像，远程服务器是 x86_64）。

解决方案：

- 使用 Buildx 按指定平台构建（部署脚本已默认使用 `linux/amd64`）

```bash
# 可选：手动启用并使用 buildx
docker buildx version
docker buildx create --name hs-builder --use

# 构建 amd64 单平台并加载到本地以便 push
docker buildx build --platform linux/amd64 -t hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:manual --load .
docker push hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:manual
````

- 如需覆盖默认平台，可在 `.env.deploy` 中增加：

```bash
# 可选：覆盖目标平台（默认 linux/amd64）
TARGET_PLATFORM=linux/amd64
```

- 若仍报错，确认远程服务器架构（`uname -m`），并确保构建平台与之匹配。

````

## 手动部署步骤

如果自动部署脚本失败，可以手动执行：

```bash
# 1. 本地构建镜像
cd frontend
docker build -t hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:manual .

# 2. 登录阿里云镜像仓库
docker login hk-store-registry.cn-hongkong.cr.aliyuncs.com

# 3. 推送镜像
docker push hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:manual

# 4. SSH 登录服务器
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com

# 5. 在服务器上执行
docker pull hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:manual
docker stop hotel-search-web || true
docker rm hotel-search-web || true
docker run -d --name hotel-search-web --restart unless-stopped -p 3000:80 \
   hk-store-registry.cn-hongkong.cr.aliyuncs.com/web/pax:manual
````

## 容器管理命令

### 查看日志

```bash
# 实时查看容器日志
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com "docker logs -f hotel-search-web"

# 查看最近 100 行日志
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com "docker logs --tail 100 hotel-search-web"
```

### 重启容器

```bash
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com "docker restart hotel-search-web"
```

### 停止容器

```bash
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com "docker stop hotel-search-web"
```

### 进入容器

```bash
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com "docker exec -it hotel-search-web sh"
```

## 安全建议

1. **使用 SSH 密钥认证**：

   ```bash
   # 生成密钥对
   ssh-keygen -t rsa -b 4096

   # 复制公钥到服务器
   ssh-copy-id -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com

   # 然后在 .env.deploy 中删除 UPLOAD_PASSWORD 和 DOCKER_PASSWORD
   ```

2. **限制 .env.deploy 文件权限**：

   ```bash
   chmod 600 .env.deploy
   ```

3. **使用 CI/CD 流水线**：
   考虑使用 GitHub Actions 或 GitLab CI 进行自动化部署

4. **定期更新基础镜像**：
   ```bash
   # 更新 Dockerfile 中的基础镜像版本
   # 重新构建并部署
   ```

## 回滚操作

### 方式 1: 使用历史镜像版本

````bash
# 查看所有镜像版本
# 在阿里云控制台查看或使用 API

# SSH 登录服务器

```bash
# SSH 登录服务器
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com

# 查看备份
ls -la /root/pax/hotel-search/backup/

# 恢复备份
cd /root/pax/hotel-search/web
rm -rf .next
cp -r ../backup/web-backup-YYYYMMDD_HHMMSS/.next ./

# 重启服务
docker compose up -d --force-recreate
````

## 监控和日志

查看 Nginx 日志：

```bash
ssh -p 60022 root@dsjnvydxri-public.bastionhost.aliyuncs.com
docker logs hotel-search-web
docker logs -f hotel-search-web  # 实时查看
```

## 联系支持

如遇问题请联系运维团队或查看项目文档。
