package com.campus.idle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.idle.entity.User;

/**
 * 用户 Mapper
 * 继承 BaseMapper 后自动拥有单表CRUD: insert/selectById/updateById/delete 等
 */
public interface UserMapper extends BaseMapper<User> {
}
