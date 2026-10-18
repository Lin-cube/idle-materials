package com.campus.idle.config;

import com.campus.idle.common.JwtUtil;
import com.campus.idle.entity.User;
import com.campus.idle.mapper.UserMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 * 每个请求进来先经过这里: 从请求头取出Token并解析,
 * 解析成功就把用户信息放进 SecurityContext, 后续接口就能拿到当前用户
 */
@Component
@RequiredArgsConstructor   // 自动生成带final字段的构造器(依赖注入)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求头 Authorization 中取出Token(格式: Bearer xxxxx)
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                // 2. 解析Token拿到用户ID
                Long userId = jwtUtil.parseUserId(token);
                // 3. 查询用户并放入安全上下文
                User user = userMapper.selectById(userId);
                if (user != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // Token无效或过期: 不设置认证信息, 由后面的授权规则处理
            }
        }
        // 4. 继续执行后续过滤器链
        filterChain.doFilter(request, response);
    }
}
