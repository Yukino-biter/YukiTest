package com.yuki.test.controller;

import com.yuki.test.common.ApiResponse;
import com.yuki.test.dto.import_.ImportPaperRequest;
import com.yuki.test.dto.import_.ImportPaperResponse;
import com.yuki.test.service.YukiImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/yuki/admin")
@RequiredArgsConstructor
public class YukiAdminController {

    private final YukiImportService importService;

    @PostMapping("/import")
    public ApiResponse<ImportPaperResponse> importPaper(@Valid @RequestBody ImportPaperRequest request) {
        return ApiResponse.ok(importService.importPaper(request));
    }
}
