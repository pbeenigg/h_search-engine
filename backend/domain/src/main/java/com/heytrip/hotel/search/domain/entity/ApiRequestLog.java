package com.heytrip.hotel.search.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;


/**
 * 外部API请求日志实体
 * 用途：记录对外部供应商接口的请求与响应摘要，正文采用压缩字节存储，便于审计与问题排查。
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "api_request_log")
@DynamicUpdate
@Comment("外部API请求日志（请求/响应原文采用压缩字节存储）")
public class ApiRequestLog {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 追踪ID（串联同一调用链或同一任务的多次请求） */
    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    /** 任务编码（如：HOTEL_FULL_SYNC_ALL） */
    @Column(name = "job_code", nullable = false, length = 64)
    private String jobCode;

    /** 数据来源（ALL/CN/INTL 等） */
    @Column(name = "source", nullable = false, length = 32)
    private String source;

    /** HTTP 方法（GET/POST/PUT/DELETE） */
    @Column(name = "http_method", nullable = false, length = 8)
    private String httpMethod;

    /** 请求URL */
    @Column(name = "url", nullable = false)
    private String url;

    /** 请求头（JSON） */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_headers", nullable = false, columnDefinition = "JSON")
    private String requestHeaders;

    /** 响应状态码 */
    @Column(name = "response_status")
    private Integer responseStatus;

    /** 耗时（毫秒） */
    @Column(name = "duration_ms")
    private Integer durationMs;

    /** 请求正文（压缩后字节，MySQL 使用 LONGBLOB 存储） */
    @Lob
    @Column(name = "request_body_compressed", columnDefinition = "LONGBLOB")
    private byte[] requestBodyCompressed;

    /** 响应正文（压缩后字节，MySQL 使用 LONGBLOB 存储） */
    @Lob
    @Column(name = "response_body_compressed", columnDefinition = "LONGBLOB")
    private byte[] responseBodyCompressed;

    /** 请求体大小（字节） */
    @Column(name = "request_size_bytes")
    private Integer requestSizeBytes;

    /** 响应体大小（字节） */
    @Column(name = "response_size_bytes")
    private Integer responseSizeBytes;

    /** 压缩算法（固定 gzip），数据库类型为 ENUM */
    @Enumerated(EnumType.STRING)
    @Column(name = "compression", nullable = false, columnDefinition = "ENUM('none','gzip','zstd','lz4')")
    private CompressionCodec compression = CompressionCodec.gzip;

    /** 供应商 app 标识（可选） */
    @Column(name = "app", length = 64)
    private String app;

    /** 记录时间（UTC） */
    @Column(name = "timestamp_utc")
    private OffsetDateTime timestampUtc;

    /** 创建时间 */
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
