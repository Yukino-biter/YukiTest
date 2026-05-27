package com.yuki.test.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuki.test.dto.paper.PaperDetailResponse;
import com.yuki.test.entity.YukiPaper;
import com.yuki.test.entity.YukiQuestionItem;
import com.yuki.test.entity.YukiQuestionMain;
import com.yuki.test.exception.ResourceNotFoundException;
import com.yuki.test.mapper.YukiPaperMapper;
import com.yuki.test.mapper.YukiQuestionItemMapper;
import com.yuki.test.mapper.YukiQuestionMainMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YukiPaperService {

    private final YukiPaperMapper paperMapper;
    private final YukiQuestionMainMapper mainMapper;
    private final YukiQuestionItemMapper itemMapper;

    public List<YukiPaper> listPublishedPapers() {
        return paperMapper.selectList(
                new LambdaQueryWrapper<YukiPaper>()
                        .eq(YukiPaper::getIsPublished, 1)
                        .orderByAsc(YukiPaper::getLevel)
                        .orderByAsc(YukiPaper::getId));
    }

    public PaperDetailResponse getPaperDetail(Long paperId) {
        YukiPaper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new ResourceNotFoundException("试卷不存在：" + paperId);
        }
        if (paper.getIsPublished() != 1) {
            throw new IllegalArgumentException("试卷未发布");
        }

        List<YukiQuestionMain> mains = mainMapper.selectList(
                new LambdaQueryWrapper<YukiQuestionMain>()
                        .eq(YukiQuestionMain::getPaperId, paperId)
                        .orderByAsc(YukiQuestionMain::getSortOrder)
                        .orderByAsc(YukiQuestionMain::getId)
        );

        List<Long> mainIds = mains.stream().map(YukiQuestionMain::getId).toList();
        Map<Long, List<YukiQuestionItem>> itemMap = mainIds.isEmpty()
                ? Map.of()
                : itemMapper.selectList(new LambdaQueryWrapper<YukiQuestionItem>()
                        .in(YukiQuestionItem::getMainId, mainIds)
                        .orderByAsc(YukiQuestionItem::getSortOrder)
                        .orderByAsc(YukiQuestionItem::getId))
                .stream()
                .collect(Collectors.groupingBy(YukiQuestionItem::getMainId));

        List<PaperDetailResponse.QuestionMainBlock> blocks = mains.stream()
                .map(main -> toMainBlock(main, itemMap.getOrDefault(main.getId(), List.of())))
                .toList();

        return new PaperDetailResponse(paper, blocks);
    }

    private PaperDetailResponse.QuestionMainBlock toMainBlock(YukiQuestionMain main,
                                                              List<YukiQuestionItem> items) {
        List<PaperDetailResponse.QuestionItemBlock> itemBlocks = items.stream()
                .sorted(Comparator.comparing(YukiQuestionItem::getSortOrder)
                        .thenComparing(YukiQuestionItem::getId))
                .map(item -> new PaperDetailResponse.QuestionItemBlock(
                        item.getId(),
                        item.getMainId(),
                        item.getContent(),
                        item.getOptions(),
                        item.getSortOrder()))
                .toList();

        return new PaperDetailResponse.QuestionMainBlock(
                main.getId(),
                main.getLevel(),
                main.getSection(),
                main.getTitle(),
                main.getMaterial(),
                main.getSortOrder(),
                itemBlocks);
    }
}
