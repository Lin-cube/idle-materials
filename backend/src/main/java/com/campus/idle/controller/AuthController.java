package com.campus.idle.controller;

import com.campus.idle.common.Result;
import com.campus.idle.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口: 登录 / 注册
 * 路径 /api/auth/** 在SecurityConfig中放行, 无需Token
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录请求体 */
    @Data
    public static class LoginBody {
        private String username;   // 登录账号
        private String password;   // 密码
    }

    /** 注册请求体 */
    @Data
    public static class RegisterBody {
        private String username;   // 学号/工号
        private String password;   // 密码
        private String realName;   // 真实姓名
        private String phone;      // 手机号
        private String dept;       // 部门/学院/社团
    }

    /** 登录 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Validated LoginBody body) {
        return Result.ok(authService.login(body.getUsername(), body.getPassword()));
    }

    /** 注册(默认学生角色) */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Validated RegisterBody body) {
        authService.register(body.getUsername(), body.getPassword(),
                body.getRealName(), body.getPhone(), body.getDept());
        return Result.ok("注册成功", null);
    }
}
