package com.yuki.test.dto.wrongbook;

import lombok.Data;

@Data
public class WrongBookResponse {
    private Long id;
    private Long userId;
    private Long questionItemId;
    private String userAnswer;
    private String correctAnswer;
    private Integer isResolved;
    private Integer wrongCount;
    private String content;
    private String options;
    private String section;
    private String level;
    private Long paperId;
    private String paperName;
}
