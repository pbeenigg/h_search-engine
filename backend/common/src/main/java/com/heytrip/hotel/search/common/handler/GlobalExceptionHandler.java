package com.heytrip.hotel.search.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.heytrip.hotel.search.common.api.R;
import com.heytrip.hotel.search.common.exception.AuthException;
import com.heytrip.hotel.search.common.exception.BusinessException;
import com.heytrip.hotel.search.common.exception.RateLimitException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(AuthException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleAuthException(AuthException e) {
        log.warn("认证异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public R<Void> handleRateLimitException(RateLimitException e) {
        log.warn("限流异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleNotLoginException(NotLoginException e) {
        log.warn("Sa-Token 未登录异常: {}", e.getMessage());
        return R.fail(401, "用户未登录，请先登录");
    }
    
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("Sa-Token 权限不足: {}", e.getMessage());
        return R.fail(403, "权限不足，无法访问该资源");
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", errors);
        return R.fail(400, errors);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String errors = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数约束违反: {}", errors);
        return R.fail(400, errors);
    }
    
    @ExceptionHandler(DataIntegrityViolationException.class)
    public R<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error("数据完整性违反", e);
        return R.fail("数据约束违反，可能是重复数据或外键约束");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public R<Void> handNoResourceFoundException(NoResourceFoundException e) {
        log.error("资源未找到", e);
        return R.fail("请求的资源不存在");
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail("系统内部错误，请联系管理员");
    }
}
