package com.heytrip.hotel.search.ingest.exception;

/**
 * 任务停止异常
 * 当检测到任务应该停止时抛出此异常，用于中断Camel路由循环
 */
public class TaskStoppedException extends RuntimeException {
    
    public TaskStoppedException(String message) {
        super(message);
    }
    
    public TaskStoppedException(String message, Throwable cause) {
        super(message, cause);
    }
}
