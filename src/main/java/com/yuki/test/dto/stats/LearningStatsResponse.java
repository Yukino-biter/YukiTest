package com.yuki.test.dto.stats;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class LearningStatsResponse {
    private int totalExams;
    private int totalQuestions;
    private int totalCorrect;
    private double overallCorrectRate;
    private int wrongBookTotal;
    private int wrongBookUnresolved;
    private List<RecentExam> recentExams;
    private Map<String, SectionStats> sectionStats;

    @Data
    public static class RecentExam {
        private Long attemptId;
        private Long paperId;
        private String paperName;
        private String level;
        private int score;
        private int total;
        private int correct;
        private String submittedAt;
    }

    @Data
    public static class SectionStats {
        private int total;
        private int correct;
        private double correctRate;
    }
}
