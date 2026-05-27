package com.yuki.test.dto.import_;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ImportPaperRequest {

    @NotNull
    @Valid
    private PaperMeta paper;

    @NotEmpty
    @Valid
    private List<QuestionMainBlock> questionMains;

    @Data
    public static class PaperMeta {
        @NotBlank
        private String level;
        @NotBlank
        private String paperName;
        @NotNull
        private Integer examMinutes;
    }

    @Data
    public static class QuestionMainBlock {
        @NotBlank
        private String section;
        private String title;
        private String material;
        private Integer sortOrder;

        @NotEmpty
        @Valid
        private List<QuestionItemBlock> questionItems;
    }

    @Data
    public static class QuestionItemBlock {
        @NotBlank
        private String content;
        @NotNull
        private JsonNode options;
        @NotBlank
        private String correctAnswer;
        private Integer sortOrder;
    }
}
