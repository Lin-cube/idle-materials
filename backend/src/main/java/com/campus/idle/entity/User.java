package com.campus.idle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体, 对应表 sys_user
 * 实现 UserDetails 供 Spring Security 认证使用
 */
@Data
@TableName("sys_user")
public class User implements org.springframework.security.core.userdetails.UserDetails, Serializable {

    /** 用户ID(主键自增) */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号(学号/工号) */
    private String username;

    /** 密码(BCrypt加密) */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 角色: STUDENT/TEACHER/WAREHOUSE/ASSET/LEADER */
    private String role;

    /** 联系电话 */
    private String phone;

    /** 所属部门/学院/社团 */
    private String dept;

    /** 状态: 0正常 1冻结 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    // ============ 以下是 UserDetails 接口要求的方法 ============

    /** 授予的权限: 用角色名作为权限标识 */
    @Override
    public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return java.util.List.of(() -> "ROLE_" + role);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;   // 账号未过期
    }

    @Override
    public boolean isAccountNonLocked() {
        return status == null || status == 0;   // 冻结状态视为锁定
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;   // 凭证未过期
    }

    @Override
    public boolean isEnabled() {
        return true;   // 账号可用
    }
}
