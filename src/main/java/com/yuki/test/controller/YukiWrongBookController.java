package com.yuki.test.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuki.test.common.ApiResponse;
import com.yuki.test.dto.wrongbook.WrongBookResponse;
import com.yuki.test.interceptor.AuthInterceptor;
import com.yuki.test.service.YukiWrongBookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yuki/wrong-book")
@RequiredArgsConstructor
public class YukiWrongBookController {

    private final YukiWrongBookService wrongBookService;

    @GetMapping
    public ApiResponse<Page<WrongBookResponse>> query(HttpServletRequest httpRequest,
                                                      @RequestParam(required = false) Integer resolved,
                                                      @RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return ApiResponse.ok(wrongBookService.queryWrongBook(userId, resolved, page, size));
    }

    @PutMapping("/{id}/resolve")
    public ApiResponse<Void> markResolved(HttpServletRequest httpRequest, @PathVariable Long id) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        wrongBookService.markResolved(id, userId);
        return ApiResponse.ok(null);
    }

    @PostMapping("/retry")
    public ApiResponse<WrongBookResponse> retry(HttpServletRequest httpRequest,
                                                @RequestParam Long questionItemId) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        return ApiResponse.ok(wrongBookService.getRetryQuestion(questionItemId, userId));
    }
}
