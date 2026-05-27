package com.yuki.test.dto.exam;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExamSubmitResponse {
    private Long attemptId;
    private Integer total;
    private Integer correct;
    private Integer wrong;
    private Integer score;
}
