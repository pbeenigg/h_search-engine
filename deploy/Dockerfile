# HeyTrip Supplier Integration JAR包部署专用 Dockerfile
# 用于直接部署已构建的JAR包，无需源码编译

FROM amazoncorretto:17-alpine

# 设置时区为上海时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 安装必要工具
RUN apk add --no-cache \
    curl \
    wget \
    && addgroup -g 1000 appuser \
    && adduser -D -s /bin/sh -u 1000 -G appuser appuser

# 设置工作目录
WORKDIR /app

# 复制JAR包（通过构建参数指定路径）
ARG JAR_FILE_PATH
COPY ${JAR_FILE_PATH} app.jar

# 创建日志目录并设置权限
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# 切换到非root用户
USER appuser

# 暴露端口
EXPOSE 18080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:18080/actuator/health || exit 1

# 设置JVM参数
ENV JAVA_OPTS="-Xmx4g -Xms2g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=80 -XX:G1HeapRegionSize=16m -XX:MaxGCPauseMillis=200 -Duser.timezone=Asia/Shanghai -Djava.security.egd=file:/dev/./urandom"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
