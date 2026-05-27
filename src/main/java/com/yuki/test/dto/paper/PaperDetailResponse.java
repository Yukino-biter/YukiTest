package com.yuki.test.dto.paper;

import com.yuki.test.entity.YukiPaper;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaperDetailResponse {
    private YukiPaper paper;
    private List<QuestionMainBlock> mains;

    @Data
    @AllArgsConstructor
    public static class QuestionMainBlock {
        private Long id;
        private String level;
        private String section;
        private String title;
        private String material;
        private Integer sortOrder;
        private List<QuestionItemBlock> items;
    }

    @Data
    @AllArgsConstructor
    public static class QuestionItemBlock {
        private Long id;
        private Long mainId;
        private String content;
        private String options;
        private Integer sortOrder;
    }
}
