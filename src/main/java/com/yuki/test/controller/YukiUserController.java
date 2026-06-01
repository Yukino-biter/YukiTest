package com.yuki.test.controller;

import com.yuki.test.common.ApiResponse;
import com.yuki.test.dto.user.LoginRequest;
import com.yuki.test.dto.user.LoginResponse;
import com.yuki.test.dto.user.RegisterRequest;
import com.yuki.test.dto.user.UpdateAiConfigRequest;
import com.yuki.test.dto.user.UserAiConfigResponse;
import com.yuki.test.interceptor.AuthInterceptor;
import com.yuki.test.service.YukiUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yuki/user")
@RequiredArgsConstructor
public class YukiUserController {

    private final YukiUserService userService;

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ApiResponse.ok(userService.login(new LoginRequest(request.getUsername(), request.getPassword())));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<java.util.Map<String, Object>> me(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        var user = userService.getById(userId);
        return ApiResponse.ok(java.util.Map.of("userId", user.getId(), "nickname", user.getNickname() != null ? user.getNickname() : ""));
    }

    @PostMapping("/update-ai-config")
    public ApiResponse<UserAiConfigResponse> updateAiConfig(HttpServletRequest httpRequest,
                                                            @Valid @RequestBody UpdateAiConfigRequest request) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return ApiResponse.ok(UserAiConfigResponse.from(userService.updateAiConfig(userId, request)));
    }
}
