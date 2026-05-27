package com.yuki.test.dto.ai;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiAnalyzeRequest {
    private Long userId;

    @NotNull
    private Long questionItemId;
}
