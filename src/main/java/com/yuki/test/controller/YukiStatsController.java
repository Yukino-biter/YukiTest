package com.yuki.test.controller;

import com.yuki.test.common.ApiResponse;
import com.yuki.test.dto.stats.LearningStatsResponse;
import com.yuki.test.interceptor.AuthInterceptor;
import com.yuki.test.service.YukiStatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yuki/stats")
@RequiredArgsConstructor
public class YukiStatsController {

    private final YukiStatsService statsService;

    @GetMapping
    public ApiResponse<LearningStatsResponse> getStats(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return ApiResponse.ok(statsService.getStats(userId));
    }
}
