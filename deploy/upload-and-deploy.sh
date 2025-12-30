#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKEND_DIR="${PROJECT_ROOT}/backend"
FRONTEND_DIR="${PROJECT_ROOT}/frontend"  # 前端目录（如果使用 web 目录，请改为 web）
TARGET_JAR_NAME="hotel-search-app-1.0.0-SNAPSHOT.jar"
LOCAL_JAR_PATH="${BACKEND_DIR}/app/target/${TARGET_JAR_NAME}"
REMOTE_WORKDIR="/root/pax"  # 服务器上的默认部署目录，可通过参数覆盖
CONFIG_FILE="${SCRIPT_DIR}/upload-and-deploy.env"

usage() {
  cat <<'EOF'
用法：
  upload-and-deploy.sh --host <服务器IP> --user <用户名> [--key <私钥路径> | --password <密码>] \
      [--port <SSH端口>] [--remote-dir <远程目录>] [--deploy-type <部署类型>] [--extra-ssh "参数"]

提示：可在 deploy/upload-and-deploy.env 中设置默认的 HOST/USER/PORT 等参数。

示例：
  # 部署后端JAR包（默认）
  ./upload-and-deploy.sh --key ~/.ssh/id_rsa

  # 部署前端Web
  ./upload-and-deploy.sh --key ~/.ssh/id_rsa --deploy-type web

  # 部署前后端（完整部署）
  ./upload-and-deploy.sh --key ~/.ssh/id_rsa --deploy-type full

  # 指定服务器和目录
  ./upload-and-deploy.sh --host 203.0.113.10 --user deployer --key ~/.ssh/id_rsa \
      --remote-dir /data/hhotel-search --deploy-type full

入参说明：
  --host         目标服务器 IP 或域名（必填，可在 env 文件中预配置）
  --user         SSH 登录用户名（必填，可在 env 文件中预配置）
  --key          SSH 私钥路径，用于认证（与 --password 二选一）
  --password     SSH 登录密码（与 --key 二选一，脚本会使用 sshpass）
  --port         SSH 端口，默认 22（可在 env 文件中预配置）
  --remote-dir   部署的远程目录，默认 /root/pax
  --deploy-type  部署类型：backend|web|full，默认 backend
                 backend: 仅部署后端JAR包
                 web: 仅部署前端Web
                 full: 部署前后端完整应用
  --extra-ssh    追加到 ssh/scp/sftp 命令的参数，例如 "-o ProxyJump=bastion"
  --help         查看帮助
EOF
}

if [[ -f "$CONFIG_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$CONFIG_FILE"
fi

HOST="${UPLOAD_HOST:-}"
SSH_USER="${UPLOAD_USER:-}"
SSH_KEY=""
SSH_PASSWORD="${UPLOAD_PASSWORD:-}"
SSH_PORT="${UPLOAD_PORT:-22}"
REMOTE_DIR="${UPLOAD_REMOTE_DIR:-$REMOTE_WORKDIR}"
EXTRA_SSH_OPTIONS="${UPLOAD_SSH_OPTIONS:-}"
DEPLOY_TYPE="${DEPLOY_TYPE:-backend}"  # backend, web, full

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host)
      HOST="$2"; shift 2;;
    --user)
      SSH_USER="$2"; shift 2;;
    --key)
      SSH_KEY="$2"; shift 2;;
    --password)
      SSH_PASSWORD="$2"; shift 2;;
    --port)
      SSH_PORT="$2"; shift 2;;
    --remote-dir)
      REMOTE_DIR="$2"; shift 2;;
    --deploy-type)
      DEPLOY_TYPE="$2"; shift 2;;
    --extra-ssh)
      EXTRA_SSH_OPTIONS="$2"; shift 2;;
    --help|-h)
      usage; exit 0;;
    *)
      echo "[ERROR] 未识别的参数: $1" >&2
      usage
      exit 1;;
  esac
done

if [[ -z "$HOST" || -z "$SSH_USER" ]]; then
  echo "[ERROR] --host/--user 为必填参数，可在 ${CONFIG_FILE} 中预配置 host/user/port" >&2
  usage
  exit 1
fi

# 验证部署类型
if [[ ! "$DEPLOY_TYPE" =~ ^(backend|web|full)$ ]]; then
  echo "[ERROR] --deploy-type 必须为 backend, web 或 full" >&2
  usage
  exit 1
fi

if [[ -z "$SSH_KEY" && -z "$SSH_PASSWORD" ]]; then
  read -rsp "请输入 SSH 密码: " SSH_PASSWORD
  echo
fi

if [[ -n "$SSH_KEY" && -n "$SSH_PASSWORD" ]]; then
  log "检测到同时提供私钥和密码，将优先使用私钥"
  SSH_PASSWORD=""
fi

if [[ -n "$SSH_KEY" && ! -f "$SSH_KEY" ]]; then
  echo "[ERROR] 指定的私钥文件不存在: $SSH_KEY" >&2
  exit 1
fi

log() {
  echo -e "[INFO] $1"
}

log_connection_context() {
  local masked_password="${SSH_PASSWORD:+******}"
  echo "----------------------------------------"
  echo -e "[INFO] date: $(date '+%Y-%m-%d %H:%M:%S')"
  echo -e "[INFO] deploy-type: $DEPLOY_TYPE"
  echo -e "[INFO] host: $HOST"
  echo -e "[INFO] user: $SSH_USER"
  echo -e "[INFO] port: $SSH_PORT"
  echo -e "[INFO] remote-dir: $REMOTE_DIR"
  echo -e "[INFO] key: $SSH_KEY"
  echo -e "[INFO] password: $masked_password"
  if [[ -n "$EXTRA_SSH_OPTIONS" ]]; then
    echo -e "[INFO] extra-ssh-options: $EXTRA_SSH_OPTIONS"
  fi
}

die() {
  echo -e "[ERROR] $1" >&2
  exit 1
}

ensure_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    die "缺少依赖命令: $1"
  fi
}

# 在后端目录执行命令（用于 Maven 构建）
run_in_backend() {
  (cd "$BACKEND_DIR" && "$@")
}

# 在项目根目录执行命令（通用）
run_in_project() {
  (cd "$PROJECT_ROOT" && "$@")
}

get_file_size() {
  local file_path="$1"
  if stat -f%z "$file_path" >/dev/null 2>&1; then
    stat -f%z "$file_path"
  else
    stat -c%s "$file_path"
  fi
}

format_bytes() {
  local bytes="$1"
  awk -v b="$bytes" 'BEGIN {
    split("B KB MB GB TB PB", units);
    value = b;
    i = 1;
    while (value >= 1024 && i < 6) {
      value /= 1024;
      i++;
    }
    printf("%.2f %s", value, units[i]);
  }'
}

format_speed() {
  local bytes="$1"
  local seconds="$2"
  awk -v b="$bytes" -v s="$seconds" 'BEGIN {
    if (s <= 0) { s = 1 }
    speed = b / s;
    split("B/s KB/s MB/s GB/s TB/s", units);
    value = speed;
    i = 1;
    while (value >= 1024 && i < 5) {
      value /= 1024;
      i++;
    }
    printf("%.2f %s", value, units[i]);
  }'
}

transfer_with_scp() {
  local source_path="$1"
  local destination="$2"
  local label="$3"
  local start_ts end_ts duration bytes human_size speed
  local max_retries=3
  local retry_count=0

  start_ts=$(date +%s)
  
  while (( retry_count < max_retries )); do
    if default_scp "$source_path" "$destination"; then
      break
    else
      retry_count=$((retry_count + 1))
      if (( retry_count < max_retries )); then
        log "上传失败，等待5秒后重试 ($retry_count/$max_retries)..."
        sleep 5
      else
        log "上传失败，已达到最大重试次数 ($max_retries)"
        return 1
      fi
    fi
  done
  
  end_ts=$(date +%s)
  duration=$(( end_ts - start_ts ))
  if (( duration < 1 )); then
    duration=1
  fi

  bytes=$(get_file_size "$source_path")
  human_size=$(format_bytes "$bytes")
  speed=$(format_speed "$bytes" "$duration")

  echo -e "[INFO] 上传完成: ${label} (${human_size}) -> ${destination}，耗时 ${duration}s，平均速率 ${speed}"
  return 0
}

# 可靠文件传输
reliable_transfer() {
  local source_path="$1"
  local destination="$2"
  local label="$3"
  
  log "开始传输 $label：$source_path -> $destination"
  
  # 检查源文件是否存在
  if [[ ! -e "$source_path" ]]; then
    log "源文件不存在：$source_path"
    return 1
  fi
  
  # 如果是目录，优先使用压缩传输
  if [[ -d "$source_path" ]]; then
    log "检测到目录传输，使用压缩方式：$label"
    if compress_and_transfer "$source_path" "$destination" "$label"; then
      return 0
    fi
    log "压缩传输失败，尝试其他方式"
    
    # 回退到 rsync（如果支持）
    if check_remote_rsync; then
      log "尝试rsync传输：$label"
      if transfer_directory_with_rsync "$source_path" "$destination" "$label"; then
        return 0
      fi
      log "rsync传输失败"
    fi
    
    # 最后回退到递归scp（需要-r参数）
    log "使用递归scp传输：$label"
    local parent_dir="$(dirname "$destination")"
    if default_scp -r "$source_path" "${SSH_USER}@${HOST}:${parent_dir}/"; then
      return 0
    else
      log "递归scp传输失败"
      return 1
    fi
  fi
  
  # 文件传输使用scp
  log "使用scp传输文件：$label"
  transfer_with_scp "$source_path" "$destination" "$label"
}

# 检查远程服务器是否支持rsync
check_remote_rsync() {
  # 如果已经检查过，直接返回缓存结果
  if [[ "$REMOTE_RSYNC_AVAILABLE" == "yes" ]]; then
    return 0
  elif [[ "$REMOTE_RSYNC_AVAILABLE" == "no" ]]; then
    return 1
  fi
  
  # 首次检查远程rsync可用性
  if default_ssh "command -v rsync >/dev/null 2>&1"; then
    log "远程服务器支持rsync"
    REMOTE_RSYNC_AVAILABLE="yes"
    return 0
  else
    log "远程服务器不支持rsync，将使用scp进行所有传输"
    REMOTE_RSYNC_AVAILABLE="no"
    return 1
  fi
}

# 压缩并传输目录
compress_and_transfer() {
  local source_dir="$1"
  local destination="$2"
  local label="$3"
  
  if [[ ! -d "$source_dir" ]]; then
    log "源目录不存在：$source_dir"
    return 1
  fi
  
  # 从destination中提取远程路径（去除SSH用户信息）
  local remote_path="${destination#*:}"
  
  local temp_archive="/tmp/$(basename "$source_dir")-$(date +%s).tar.gz"
  local remote_temp="/tmp/$(basename "$temp_archive")"
  
  log "压缩目录 $label：$source_dir -> $temp_archive"
  
  # 压缩目录
  if ! tar -czf "$temp_archive" -C "$(dirname "$source_dir")" "$(basename "$source_dir")"; then
    log "压缩失败：$source_dir"
    return 1
  fi
  
  local archive_size
  archive_size=$(get_file_size "$temp_archive")
  log "压缩完成，压缩包大小：$(format_bytes "$archive_size")"
  
  # 上传压缩包
  log "上传压缩包：$label"
  if ! transfer_with_scp "$temp_archive" "${SSH_USER}@${HOST}:${remote_temp}" "$(basename "$temp_archive")"; then
    rm -f "$temp_archive"
    return 1
  fi
  
  # 在远程服务器解压
  log "远程解压：$label"
  # 如果远程路径以/结尾，说明是目标目录，需要解压到该目录内
  local extract_target_dir
  if [[ "$remote_path" == */ ]]; then
    extract_target_dir="$remote_path"
  else
    extract_target_dir="$(dirname "$remote_path")"
  fi
  if ! default_ssh "mkdir -p '$extract_target_dir' && cd '$extract_target_dir' && tar -xzf '$remote_temp' && rm -f '$remote_temp'"; then
    rm -f "$temp_archive"
    log "远程解压失败"
    return 1
  fi
  
  # 清理本地临时文件
  rm -f "$temp_archive"
  log "目录传输完成：$label"
  return 0
}

transfer_directory_with_rsync() {
  local source_path="$1"
  local destination="$2"
  local label="$3"
  local max_retries=3
  local retry_count=0

  # 检查是否有rsync命令
  if ! command -v rsync >/dev/null 2>&1; then
    log "rsync不可用，回退到scp进行目录传输"
    return 1
  fi

  log "使用rsync同步目录: $label"
  
  local start_ts end_ts duration
  start_ts=$(date +%s)
  
  while (( retry_count < max_retries )); do
    if default_rsync "$source_path" "$destination"; then
      break
    else
      retry_count=$((retry_count + 1))
      if (( retry_count < max_retries )); then
        log "rsync失败，等待5秒后重试 ($retry_count/$max_retries)..."
        sleep 5
      else
        log "rsync失败，已达到最大重试次数 ($max_retries)"
        return 1
      fi
    fi
  done
  
  end_ts=$(date +%s)
  duration=$(( end_ts - start_ts ))
  
  echo -e "[INFO] rsync同步完成: ${label}，耗时 ${duration}s"
  return 0
}

ensure_command mvnd
ensure_command ssh
ensure_command scp
if [[ -n "$SSH_PASSWORD" ]]; then
  if ! command -v sshpass >/dev/null 2>&1; then
    cat <<'EOF' >&2
[ERROR] 缺少依赖命令: sshpass
Mac 用户可通过 Homebrew 安装：
  brew install hudochenkov/sshpass/sshpass
或改用 SSH 私钥方式：
  ./upload-and-deploy.sh --key ~/.ssh/id_rsa
EOF
    exit 1
  fi
fi

SSH_COMMON_OPTS=(-p "$SSH_PORT" -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o ConnectTimeout=10)
SCP_COMMON_OPTS=(-P "$SSH_PORT" -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o ConnectTimeout=10 -C)

# 缓存远程rsync检查结果，避免重复检查
REMOTE_RSYNC_AVAILABLE="unknown"

if [[ -n "$SSH_KEY" ]]; then
  SSH_COMMON_OPTS+=(-i "$SSH_KEY")
  SCP_COMMON_OPTS+=(-i "$SSH_KEY")
fi


default_ssh() {
  local cmd=(ssh "${SSH_COMMON_OPTS[@]}")
  if [[ -n "$EXTRA_SSH_OPTIONS" ]]; then
    # shellcheck disable=SC2206
    local extra_opts=($EXTRA_SSH_OPTIONS)
    cmd+=("${extra_opts[@]}")
  fi
  cmd+=("${SSH_USER}@${HOST}")
  if [[ $# -gt 0 ]]; then
    cmd+=("$@")
  fi
  if [[ -n "$SSH_PASSWORD" ]]; then
    sshpass -p "$SSH_PASSWORD" "${cmd[@]}"
  else
    "${cmd[@]}"
  fi
}

default_scp() {
  local cmd=(scp "${SCP_COMMON_OPTS[@]}")
  if [[ -n "$EXTRA_SSH_OPTIONS" ]]; then
    # shellcheck disable=SC2206
    local extra_opts=($EXTRA_SSH_OPTIONS)
    cmd+=("${extra_opts[@]}")
  fi
  if [[ -n "$SSH_PASSWORD" ]]; then
    cmd+=(-oBatchMode=no)
  fi
  cmd+=("$@")
  if [[ -n "$SSH_PASSWORD" ]]; then
    sshpass -p "$SSH_PASSWORD" "${cmd[@]}"
  else
    "${cmd[@]}"
  fi
}

default_rsync() {
  local ssh_cmd="ssh"
  if [[ -n "$SSH_KEY" ]]; then
    ssh_cmd="ssh -i $SSH_KEY"
  fi
  
  local cmd=(rsync -avz --timeout=300 --partial)
  cmd+=(-e "$ssh_cmd -p $SSH_PORT -o ServerAliveInterval=30 -o ServerAliveCountMax=3")
  if [[ -n "$EXTRA_SSH_OPTIONS" ]]; then
    cmd+=(-e "$ssh_cmd -p $SSH_PORT $EXTRA_SSH_OPTIONS -o ServerAliveInterval=30 -o ServerAliveCountMax=3")
  fi
  cmd+=("$@")
  
  if [[ -n "$SSH_PASSWORD" ]]; then
    RSYNC_PASSWORD="$SSH_PASSWORD" sshpass -p "$SSH_PASSWORD" "${cmd[@]}"
  else
    "${cmd[@]}"
  fi
}

# 根据部署类型执行不同的构建步骤
if [[ "$DEPLOY_TYPE" == "backend" || "$DEPLOY_TYPE" == "full" ]]; then
  log "步骤 1/5: Maven 打包后端项目（在 backend 目录）"
  log "执行目录: $BACKEND_DIR"
  log "JAR包路径: $LOCAL_JAR_PATH"

  # 在 backend 目录下执行 Maven 构建
  run_in_backend mvnd clean package -Dmaven.test.skip=true

  if [[ ! -f "$LOCAL_JAR_PATH" ]]; then
    die "未找到 JAR 文件: $LOCAL_JAR_PATH"
  fi

  log "构建成功: $TARGET_JAR_NAME"
fi

if [[ "$DEPLOY_TYPE" == "web" || "$DEPLOY_TYPE" == "full" ]]; then
  log "步骤 1/5: 构建前端项目"

  # 检查前端目录是否存在
  if [[ ! -d "$FRONTEND_DIR" ]]; then
    log "警告: 未找到前端目录 $FRONTEND_DIR"
    log "尝试使用 web 目录..."
    FRONTEND_DIR="${PROJECT_ROOT}/web"
    if [[ ! -d "$FRONTEND_DIR" ]]; then
      die "前端目录不存在：既没有 frontend 也没有 web 目录"
    fi
  fi

  cd "$FRONTEND_DIR"
  if ! command -v pnpm >/dev/null 2>&1; then
    die "缺少依赖: pnpm，请先安装 pnpm"
  fi
  pnpm install --frozen-lockfile
  pnpm build
  
  # 检查构建产物
  if [[ ! -d "${FRONTEND_DIR}/out" ]]; then
    die "前端构建失败：未找到 out 目录：${FRONTEND_DIR}/out"
  fi
  log "前端构建完成，产物路径：${FRONTEND_DIR}/out"
  cd "${SCRIPT_DIR}"
fi

TMP_REMOTE_SCRIPT="/tmp/upload-deploy-$RANDOM.sh"
REMOTE_JAR_PATH="${REMOTE_DIR}/hotel-search/${TARGET_JAR_NAME}"
REMOTE_DEPLOY_SCRIPT="${REMOTE_DIR}/hotel-search/deploy-jar.sh"

log "步骤 2/5: 测试SSH连接并创建远程目录"
log_connection_context

# 测试SSH连接
log "测试SSH连接..."
if ! default_ssh "echo 'SSH连接测试成功'"; then
  die "SSH连接失败，请检查服务器地址、端口、用户名和认证信息"
fi

default_ssh "mkdir -p '${REMOTE_DIR}/hotel-search' '${REMOTE_DIR}/hotel-search/web'"

# 根据部署类型上传文件
if [[ "$DEPLOY_TYPE" == "backend" || "$DEPLOY_TYPE" == "full" ]]; then
  log "步骤 3/5: 上传 Jar 到服务器 -> ${REMOTE_JAR_PATH}"
  if ! transfer_with_scp "$LOCAL_JAR_PATH" "${SSH_USER}@${HOST}:${REMOTE_DIR}/hotel-search/" "$TARGET_JAR_NAME"; then
    die "上传 Jar 失败"
  fi
fi

if [[ "$DEPLOY_TYPE" == "web" || "$DEPLOY_TYPE" == "full" ]]; then
  log "步骤 3/5: 同步前端到服务器"

  # 创建远程web目录
  default_ssh "mkdir -p '${REMOTE_DIR}/hotel-search/web'"

  # 方式1: 传输构建产物 + 轻量Dockerfile（推荐）
 if [[ -d "${FRONTEND_DIR}/out" ]]; then
    log "传输前端构建产物（out目录）"
    if ! reliable_transfer "${FRONTEND_DIR}/out" "${SSH_USER}@${HOST}:${REMOTE_DIR}/hotel-search/web/" "web/out"; then
      die "上传前端构建产物失败"
    fi

    # 传输轻量级 Dockerfile 和 nginx 配置
    WEB_RUNTIME_FILES=("Dockerfile")
    for file in "${WEB_RUNTIME_FILES[@]}"; do
      LOCAL_WEB_FILE="${FRONTEND_DIR}/${file}"
      if [[ -f "$LOCAL_WEB_FILE" ]]; then
        log "同步运行时文件 web/${file}"
        if ! transfer_with_scp "$LOCAL_WEB_FILE" "${SSH_USER}@${HOST}:${REMOTE_DIR}/hotel-search/web/" "web/${file}"; then
          log "警告：web/${file} 上传失败，将使用默认配置"
        fi
      fi
    done

    log "前端部署采用构建产物模式，无需服务器端构建"

  else
    # 方式2: 传输完整源码进行Docker构建（备选）
    log "未找到构建产物，将传输源码进行服务器构建"

    WEB_FILES=("Dockerfile" "package.json" "next.config.js" "tailwind.config.js" "tsconfig.json" "postcss.config.js")
    LARGE_WEB_FILES=("pnpm-lock.yaml")

    # 传输配置文件
    for file in "${WEB_FILES[@]}"; do
      LOCAL_WEB_FILE="${FRONTEND_DIR}/${file}"
      if [[ -f "$LOCAL_WEB_FILE" ]]; then
        log "同步 web/${file}"
        if ! transfer_with_scp "$LOCAL_WEB_FILE" "${SSH_USER}@${HOST}:${REMOTE_DIR}/hotel-search/web/" "web/${file}"; then
          die "上传 web/${file} 失败"
        fi
      fi
    done

    # 传输大文件
    for file in "${LARGE_WEB_FILES[@]}"; do
      LOCAL_WEB_FILE="${FRONTEND_DIR}/${file}"
      if [[ -f "$LOCAL_WEB_FILE" ]]; then
        log "同步 web/${file}"
        if ! reliable_transfer "$LOCAL_WEB_FILE" "${SSH_USER}@${HOST}:${REMOTE_DIR}/hotel-search/web/${file}" "web/${file}"; then
          die "上传 web/${file} 失败"
        fi
      fi
    done

    # 传输源码目录（压缩方式）
    log "同步 web/src 目录"
    if ! reliable_transfer "${FRONTEND_DIR}/src" "${SSH_USER}@${HOST}:${REMOTE_DIR}/hotel-search/web/src" "web/src"; then
      die "上传 web/src 目录失败"
    fi
  fi
fi

log "步骤 4/5: 同步 deploy 核心文件"
DEFAULT_DEPLOY_DIR="${PROJECT_ROOT}/deploy"
DEPLOY_FILES=("deploy-jar.sh" "docker-compose.yml" "Dockerfile" "version-manager.sh")
for file in "${DEPLOY_FILES[@]}"; do
  LOCAL_DEPLOY_FILE="${DEFAULT_DEPLOY_DIR}/${file}"
  if [[ ! -f "$LOCAL_DEPLOY_FILE" ]]; then
    echo -e "[WARN] 本地缺少 deploy/${file}，已跳过"
    continue
  fi
  log "同步 deploy/${file}"
  if ! transfer_with_scp "$LOCAL_DEPLOY_FILE" "${SSH_USER}@${HOST}:${REMOTE_DIR}/hotel-search/" "deploy/${file}"; then
    die "上传 deploy/${file} 失败"
  fi
done

default_ssh "chmod +x '${REMOTE_DIR}/hotel-search/deploy-jar.sh'"

# 根据部署类型生成部署脚本
DEPLOY_COMMAND=""
case "$DEPLOY_TYPE" in
  "backend")
    DEPLOY_COMMAND="./deploy-jar.sh './${TARGET_JAR_NAME}' --clean --type backend"
    ;;
  "web")
    DEPLOY_COMMAND="./deploy-jar.sh dummy.jar --clean --type web"
    ;;
  "full")
    DEPLOY_COMMAND="./deploy-jar.sh './${TARGET_JAR_NAME}' --clean --type full"
    ;;
esac

# 创建远程部署脚本
default_ssh "cat > '${TMP_REMOTE_SCRIPT}' << 'EOS'
#!/bin/bash
set -e
cd '${REMOTE_DIR}/hotel-search'
${DEPLOY_COMMAND}
EOS"

# 设置执行权限
default_ssh "chmod +x '${TMP_REMOTE_SCRIPT}'"

log "步骤 5/5: 触发远程部署 (${DEPLOY_TYPE})"
default_ssh "${TMP_REMOTE_SCRIPT}"

default_ssh "rm -f '${TMP_REMOTE_SCRIPT}'"

log "部署完成！可登录 ${SSH_USER}@${HOST} 查看服务状态"

