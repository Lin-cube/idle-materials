package com.campus.idle.config;

import com.campus.idle.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 安全配置
 * 采用"无状态JWT"方案: 服务端不存Session, 每次请求靠Token认证
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 密码加密器: BCrypt算法(数据库种子数据用的同一算法) */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 安全过滤链: 定义哪些接口放行、哪些需要登录 */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 关闭CSRF: 因为我们是无状态JWT接口, 不用Cookie会话
                .csrf(csrf -> csrf.disable())
                // 开启CORS跨域(配合下方corsConfigurationSource)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 无状态会话: 完全不使用Session
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 授权规则
                .authorizeHttpRequests(auth -> auth
                        // 放行: 登录/注册接口 + 文件访问
                        .requestMatchers("/api/auth/**", "/files/**").permitAll()
                        // 其余所有 /api/** 接口都需要登录(带有效Token)
                        .anyRequest().authenticated()
                )
                // 认证失败(未登录/Token无效)时返回统一JSON而不是跳转登录页
                .exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(200);
                    res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    res.setCharacterEncoding("UTF-8");
                    res.getWriter().write(new ObjectMapper().writeValueAsString(Result.error(401, "未登录或登录已过期")));
                }))
                // 把JWT过滤器插入到用户名密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 跨域配置: 允许前端开发服务器(5173端口)访问后端 */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));   // 允许所有来源(开发期)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
