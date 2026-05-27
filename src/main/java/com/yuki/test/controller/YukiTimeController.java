package com.yuki.test.controller;

import com.yuki.test.common.ApiResponse;
import com.yuki.test.util.YukiTimeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/yuki/time")
public class YukiTimeController {

    @GetMapping("/{level}")
    public ApiResponse<Map<String, Object>> examLimit(@PathVariable String level) {
        return ApiResponse.ok(Map.of(
                "level", level.trim().toUpperCase(),
                "minutes", YukiTimeUtils.getExamLimitMinutes(level)
        ));
    }
}
