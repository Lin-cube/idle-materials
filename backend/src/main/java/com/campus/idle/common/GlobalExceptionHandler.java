package com.campus.idle.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 任何Controller抛出的异常都会被这里拦截, 统一转成 Result JSON 返回,
 * 避免前端收到一堆堆栈信息
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常: 主动抛出的带提示信息的异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.error(e.getMessage());
    }

    /** 登录失败: 用户名或密码错误 */
    @ExceptionHandler(BadCredentialsException.class)
    public Result<Void> handleBadCredentials(BadCredentialsException e) {
        return Result.error("用户名或密码错误");
    }

    /** 参数校验失败: 取第一条校验提示返回 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return Result.error(msg);
    }

    /** 兜底: 其他未知异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统繁忙, 请稍后重试");
    }
}
