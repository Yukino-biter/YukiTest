package com.yuki.test.service;

import com.yuki.test.dto.user.LoginRequest;
import com.yuki.test.dto.user.LoginResponse;
import com.yuki.test.dto.user.RegisterRequest;
import com.yuki.test.dto.user.UpdateAiConfigRequest;
import com.yuki.test.entity.YukiUser;
import com.yuki.test.mapper.YukiUserMapper;
import com.yuki.test.util.JwtUtil;
import com.yuki.test.util.YukiCryptoUtil;
import com.yuki.test.util.YukiPasswordUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class YukiUserServiceTest {

    @Test
    void updatesUserAiConfigFieldsWithEncryption() {
        YukiUserMapper userMapper = mock(YukiUserMapper.class);
        YukiPasswordUtil passwordUtil = mock(YukiPasswordUtil.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        YukiCryptoUtil cryptoUtil = mock(YukiCryptoUtil.class);

        YukiUser user = new YukiUser();
        user.setId(8L);
        when(userMapper.selectById(8L)).thenReturn(user);
        when(cryptoUtil.encrypt("sk-user")).thenReturn("encrypted-sk-user");

        YukiUserService service = new YukiUserService(userMapper, passwordUtil, jwtUtil, cryptoUtil);
        UpdateAiConfigRequest request = new UpdateAiConfigRequest();
        request.setApiProvider("deepseek");
        request.setApiKey("sk-user");
        request.setBaseUrl("https://api.deepseek.com/v1");
        request.setApiModel("deepseek-chat");

        YukiUser updated = service.updateAiConfig(8L, request);

        assertThat(updated.getApiProvider()).isEqualTo("deepseek");
        assertThat(updated.getApiKey()).isEqualTo("encrypted-sk-user");
        assertThat(updated.getBaseUrl()).isEqualTo("https://api.deepseek.com/v1");
        assertThat(updated.getApiModel()).isEqualTo("deepseek-chat");

        verify(cryptoUtil).encrypt("sk-user");
        verify(userMapper).updateById(any(YukiUser.class));
    }

    @Test
    void registerHashesPasswordAndLoginReturnsToken() {
        YukiUserMapper userMapper = mock(YukiUserMapper.class);
        YukiPasswordUtil passwordUtil = mock(YukiPasswordUtil.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        YukiCryptoUtil cryptoUtil = mock(YukiCryptoUtil.class);

        when(userMapper.selectCount(any())).thenReturn(0L);
        when(passwordUtil.hash("pass123")).thenReturn("$2a$hashed");
        when(userMapper.insert(any(YukiUser.class))).thenReturn(1);

        YukiUserService service = new YukiUserService(userMapper, passwordUtil, jwtUtil, cryptoUtil);

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("pass123");
        registerRequest.setNickname("Test");
        service.register(registerRequest);

        ArgumentCaptor<YukiUser> captor = ArgumentCaptor.forClass(YukiUser.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$hashed");

        YukiUser loginUser = new YukiUser();
        loginUser.setId(1L);
        loginUser.setUsername("testuser");
        loginUser.setPassword("$2a$hashed");
        loginUser.setStatus(1);
        loginUser.setNickname("Test");
        when(userMapper.selectOne(any())).thenReturn(loginUser);
        when(passwordUtil.matches("pass123", "$2a$hashed")).thenReturn(true);
        when(jwtUtil.generateToken(1L)).thenReturn("jwt-token");

        LoginResponse loginResp = service.login(new LoginRequest("testuser", "pass123"));
        assertThat(loginResp.getToken()).isEqualTo("jwt-token");
        assertThat(loginResp.getUserId()).isEqualTo(1L);
    }
}
