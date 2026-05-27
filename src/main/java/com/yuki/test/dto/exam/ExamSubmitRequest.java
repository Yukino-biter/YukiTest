package com.yuki.test.dto.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class ExamSubmitRequest {
    private Long userId;

    @NotNull
    private Long paperId;

    @Valid
    @NotEmpty
    private List<AnswerItem> answers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerItem {
        @NotNull
        private Long questionItemId;

        @NotNull
        private String userAnswer;
    }
}
