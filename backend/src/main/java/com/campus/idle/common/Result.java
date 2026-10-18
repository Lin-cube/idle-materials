package com.campus.idle.common;

import lombok.Data;

/**
 * 统一响应结果类
 * 所有接口都返回这个结构, 前端按 code 判断是否成功
 *
 * @param <T> 响应数据的类型
 */
@Data
public class Result<T> {

    /** 状态码: 200成功, 其他为失败 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    /** 私有构造, 通过静态方法创建 */
    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功(带数据) */
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /** 成功(不带数据) */
    public static <T> Result<T> ok() {
        return new Result<>(200, "操作成功", null);
    }

    /** 成功(自定义提示) */
    public static <T> Result<T> ok(String msg, T data) {
        return new Result<>(200, msg, data);
    }

    /** 失败(带提示) */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg, null);
    }

    /** 失败(自定义状态码) */
    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }
}
