package com.campus.idle.common;

import com.campus.idle.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户上下文工具
 * 在任何Service/Controller里调用 UserContext.get() 即可拿到当前登录用户
 */
public class UserContext {

    /** 获取当前登录用户对象 */
    public static User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    /** 获取当前登录用户ID */
    public static Long getUserId() {
        User user = get();
        return user == null ? null : user.getId();
    }
}
