package com.yuki.test.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuki.test.entity.YukiUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface YukiUserMapper extends BaseMapper<YukiUser> {
}
