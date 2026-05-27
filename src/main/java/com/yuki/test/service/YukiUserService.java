package com.yuki.test.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuki.test.dto.user.LoginRequest;
import com.yuki.test.dto.user.LoginResponse;
import com.yuki.test.dto.user.RegisterRequest;
import com.yuki.test.dto.user.UpdateAiConfigRequest;
import com.yuki.test.entity.YukiUser;
import com.yuki.test.exception.ResourceNotFoundException;
import com.yuki.test.mapper.YukiUserMapper;
import com.yuki.test.util.JwtUtil;
import com.yuki.test.util.YukiCryptoUtil;
import com.yuki.test.util.YukiPasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class YukiUserService {

    private final YukiUserMapper userMapper;
    private final YukiPasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final YukiCryptoUtil cryptoUtil;

    @Transactional(rollbackFor = Exception.class)
    public YukiUser register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<YukiUser>().eq(YukiUser::getUsername, request.getUsername()));
        if (count > 0) {
            throw new IllegalArgumentException("用户名已存在：" + request.getUsername());
        }
        YukiUser user = new YukiUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.hash(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    public LoginResponse login(LoginRequest request) {
        YukiUser user = userMapper.selectOne(
                new LambdaQueryWrapper<YukiUser>().eq(YukiUser::getUsername, request.getUsername()));
        if (user == null || !passwordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new IllegalArgumentException("账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId());
        return new LoginResponse(token, user.getId(), user.getNickname());
    }

    @Transactional(rollbackFor = Exception.class)
    public YukiUser updateAiConfig(Long userId, UpdateAiConfigRequest request) {
        YukiUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在：" + userId);
        }

        user.setApiProvider(clean(request.getApiProvider()));
        user.setApiKey(cryptoUtil.encrypt(clean(request.getApiKey())));
        user.setBaseUrl(clean(request.getBaseUrl()));
        user.setApiModel(clean(request.getApiModel()));
        userMapper.updateById(user);
        return user;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
