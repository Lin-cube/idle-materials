package com.campus.idle.common;

/**
 * 业务异常
 * 在Service里遇到不满足业务规则的情况时抛出,
 * 由 GlobalExceptionHandler 统一捕获返回给前端
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
