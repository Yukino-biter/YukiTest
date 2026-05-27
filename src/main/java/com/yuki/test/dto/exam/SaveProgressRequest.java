package com.yuki.test.dto.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class SaveProgressRequest {
    private Long userId;

    @NotNull
    private Long paperId;

    private Map<Long, String> answers;

    private Integer timeLeft;
}
