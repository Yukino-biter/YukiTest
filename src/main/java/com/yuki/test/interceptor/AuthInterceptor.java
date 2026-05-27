package com.yuki.test.interceptor;

import com.yuki.test.exception.UnauthorizedException;
import com.yuki.test.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER_ID = "currentUserId";
    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("缺少认证令牌");
        }
        String token = authHeader.substring(7);
        try {
            Long userId = jwtUtil.parseToken(token);
            request.setAttribute(CURRENT_USER_ID, userId);
            return true;
        } catch (JwtException e) {
            throw new UnauthorizedException("认证令牌无效或已过期");
        }
    }
}
