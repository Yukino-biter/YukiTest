package com.yuki.test.controller;

import com.yuki.test.dto.ai.AiAnalyzeRequest;
import com.yuki.test.interceptor.AuthInterceptor;
import com.yuki.test.service.YukiAiAnalyzeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/yuki/ai")
@RequiredArgsConstructor
public class YukiAiController {

    private final YukiAiAnalyzeService aiAnalyzeService;

    @PostMapping(value = "/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyze(HttpServletRequest httpRequest,
                              @Valid @RequestBody AiAnalyzeRequest request) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        request.setUserId(userId);
        return aiAnalyzeService.analyze(request);
    }
}
