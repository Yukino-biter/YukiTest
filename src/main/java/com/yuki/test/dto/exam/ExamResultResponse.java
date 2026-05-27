package com.yuki.test.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ExamResultResponse {
    private Long attemptId;
    private Integer score;
    private Integer total;
    private Integer correct;
    private Integer wrong;
    private Map<String, SectionStat> sections;
    private List<QuestionDetail> questions;

    @Data
    @AllArgsConstructor
    public static class SectionStat {
        private String label;
        private Integer total;
        private Integer correct;
        private Integer wrong;
    }

    @Data
    @AllArgsConstructor
    public static class QuestionDetail {
        private Long questionItemId;
        private Long mainId;
        private String section;
        private String userAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
    }
}
