package com.yuki.test.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuki.test.dto.import_.ImportPaperRequest;
import com.yuki.test.dto.import_.ImportPaperResponse;
import com.yuki.test.entity.YukiPaper;
import com.yuki.test.entity.YukiQuestionItem;
import com.yuki.test.entity.YukiQuestionMain;
import com.yuki.test.mapper.YukiPaperMapper;
import com.yuki.test.mapper.YukiQuestionItemMapper;
import com.yuki.test.mapper.YukiQuestionMainMapper;
import com.yuki.test.util.YukiTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YukiImportService {

    private final YukiPaperMapper paperMapper;
    private final YukiQuestionMainMapper mainMapper;
    private final YukiQuestionItemMapper itemMapper;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public ImportPaperResponse importPaper(ImportPaperRequest request) {
        ImportPaperRequest.PaperMeta meta = request.getPaper();
        String level = meta.getLevel().toUpperCase();
        YukiTimeUtils.validateLevel(level);

        YukiPaper paper = new YukiPaper();
        paper.setLevel(level);
        paper.setPaperName(meta.getPaperName());
        paper.setExamMinutes(meta.getExamMinutes() != null ? meta.getExamMinutes() : YukiTimeUtils.getMinutes(level));
        paper.setIsPublished(0);
        paperMapper.insert(paper);

        int mainCount = 0;
        int itemCount = 0;

        List<ImportPaperRequest.QuestionMainBlock> mains = request.getQuestionMains();
        for (int i = 0; i < mains.size(); i++) {
            ImportPaperRequest.QuestionMainBlock mainBlock = mains.get(i);

            YukiQuestionMain main = new YukiQuestionMain();
            main.setPaperId(paper.getId());
            main.setLevel(level);
            main.setSection(mainBlock.getSection().toLowerCase());
            main.setTitle(mainBlock.getTitle());
            main.setMaterial(mainBlock.getMaterial());
            main.setSortOrder(mainBlock.getSortOrder() != null ? mainBlock.getSortOrder() : i + 1);
            mainMapper.insert(main);
            mainCount++;

            List<ImportPaperRequest.QuestionItemBlock> items = mainBlock.getQuestionItems();
            for (int j = 0; j < items.size(); j++) {
                ImportPaperRequest.QuestionItemBlock itemBlock = items.get(j);

                YukiQuestionItem item = new YukiQuestionItem();
                item.setMainId(main.getId());
                item.setContent(itemBlock.getContent());
                item.setOptions(toJsonString(itemBlock.getOptions()));
                item.setCorrectAnswer(itemBlock.getCorrectAnswer().toUpperCase());
                item.setSortOrder(itemBlock.getSortOrder() != null ? itemBlock.getSortOrder() : j + 1);
                itemMapper.insert(item);
                itemCount++;
            }
        }

        return new ImportPaperResponse(paper.getId(), mainCount, itemCount);
    }

    private String toJsonString(JsonNode node) {
        if (node == null) return null;
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("options JSON 格式错误", e);
        }
    }
}
