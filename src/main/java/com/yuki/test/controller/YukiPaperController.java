package com.yuki.test.controller;

import com.yuki.test.common.ApiResponse;
import com.yuki.test.dto.paper.PaperDetailResponse;
import com.yuki.test.entity.YukiPaper;
import com.yuki.test.service.YukiPaperService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/yuki/papers")
@RequiredArgsConstructor
public class YukiPaperController {

    private final YukiPaperService paperService;

    @GetMapping
    public ApiResponse<List<YukiPaper>> list() {
        return ApiResponse.ok(paperService.listPublishedPapers());
    }

    @GetMapping("/{paperId}")
    public ApiResponse<PaperDetailResponse> detail(@PathVariable Long paperId) {
        return ApiResponse.ok(paperService.getPaperDetail(paperId));
    }
}
