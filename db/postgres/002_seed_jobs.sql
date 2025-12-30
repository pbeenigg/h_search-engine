-- 初始化 job_schedule 任务
-- 说明：使用 ON CONFLICT(job_code) DO UPDATE 以便可重复执行

INSERT INTO job_schedule (
  job_code, cron_expr, enabled, max_concurrency, batch_size, http_timeout_sec,
  params, sync_mode, sync_limit, remark, updated_at
) VALUES (
  'HOTEL_FULL_SYNC_ALL',
  '0 0 2 1 * ?',                  -- 每月1日 02:00 触发（约每30天）
  true,
  10,
  1000,
  20,
  '{"syncHotelDetail":true,"syncHotelName":true}'::jsonb,
  'FULL',
  0,
  '酒店全量同步（按 maxHotelId 遍历）',
  NOW()
)
ON CONFLICT (job_code) DO UPDATE SET
  cron_expr = EXCLUDED.cron_expr,
  enabled = EXCLUDED.enabled,
  max_concurrency = EXCLUDED.max_concurrency,
  batch_size = EXCLUDED.batch_size,
  http_timeout_sec = EXCLUDED.http_timeout_sec,
  params = EXCLUDED.params,
  sync_mode = EXCLUDED.sync_mode,
  sync_limit = EXCLUDED.sync_limit,
  remark = EXCLUDED.remark,
  updated_at = NOW();

-- 失败补偿：按 sync_log_id 重跑（通常不设定时，由运维接口触发；这里默认禁用）
INSERT INTO job_schedule (
  job_code, cron_expr, enabled, max_concurrency, batch_size, http_timeout_sec,
  params, sync_mode, sync_limit, remark, updated_at
) VALUES (
  'HOTEL_RETRY_BY_SYNC_LOG',
  '0 0 0 1 1 ? 2099',            -- 一个极少触发的占位 Cron（2099年），避免误触
  false,
  10,
  1000,
  20,
  '{
     "syncLogId": 0,
     "detailBatchSize": 20,
     "submitBatchSize": 1000,
     "concurrency": 10,
     "httpTimeoutSec": 20,
     "retry": {"maxAttempts": 3, "initialBackoffMs": 1000, "multiplier": 2}
   }'::jsonb,
  'LIMIT',
  0,
  '失败明细补偿：按 sync_log_id 重放',
  NOW()
)
ON CONFLICT (job_code) DO UPDATE SET
  cron_expr = EXCLUDED.cron_expr,
  enabled = EXCLUDED.enabled,
  max_concurrency = EXCLUDED.max_concurrency,
  batch_size = EXCLUDED.batch_size,
  http_timeout_sec = EXCLUDED.http_timeout_sec,
  params = EXCLUDED.params,
  sync_mode = EXCLUDED.sync_mode,
  sync_limit = EXCLUDED.sync_limit,
  remark = EXCLUDED.remark,
  updated_at = NOW();
