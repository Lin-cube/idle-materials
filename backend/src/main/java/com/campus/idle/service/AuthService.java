package com.campus.idle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.idle.common.BusinessException;
import com.campus.idle.common.JwtUtil;
import com.campus.idle.entity.User;
import com.campus.idle.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务: 登录与注册
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 登录: 校验账号密码, 签发JWT
     * 冻结用户仍可登录(能看自己的逾期记录), 但不能提交新的借用申请
     */
    public Map<String, Object> login(String username, String password) {
        // 1. 按用户名查用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        // 2. 校验密码(BCrypt比对)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        // 3. 签发Token
        String token = jwtUtil.generate(user.getId());
        // 4. 返回Token和用户信息(前端据此显示菜单和角色)
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        user.setPassword(null);          // 密码不能返回给前端
        data.put("user", user);
        return data;
    }

    /**
     * 注册: 默认注册为学生角色
     */
    public void register(String username, String password, String realName, String phone, String dept) {
        // 1. 用户名不能重复
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (count > 0) {
            throw new BusinessException("该账号已被注册");
        }
        // 2. 创建用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));   // BCrypt加密后入库
        user.setRealName(realName);
        user.setRole("STUDENT");       // 默认学生角色, 教师等角色由管理员在库中调整
        user.setPhone(phone);
        user.setDept(dept);
        user.setStatus(0);
        userMapper.insert(user);
    }
}
