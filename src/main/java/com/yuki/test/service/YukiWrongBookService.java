package com.yuki.test.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yuki.test.dto.wrongbook.WrongBookResponse;
import com.yuki.test.entity.YukiPaper;
import com.yuki.test.entity.YukiQuestionItem;
import com.yuki.test.entity.YukiQuestionMain;
import com.yuki.test.entity.YukiUserWrongBook;
import com.yuki.test.exception.ResourceNotFoundException;
import com.yuki.test.mapper.YukiPaperMapper;
import com.yuki.test.mapper.YukiQuestionItemMapper;
import com.yuki.test.mapper.YukiQuestionMainMapper;
import com.yuki.test.mapper.YukiUserWrongBookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class YukiWrongBookService {

    private final YukiUserWrongBookMapper wrongBookMapper;
    private final YukiQuestionItemMapper itemMapper;
    private final YukiQuestionMainMapper mainMapper;
    private final YukiPaperMapper paperMapper;

    public Page<WrongBookResponse> queryWrongBook(Long userId, Integer isResolved, int page, int size) {
        LambdaQueryWrapper<YukiUserWrongBook> wrapper = new LambdaQueryWrapper<YukiUserWrongBook>()
                .eq(YukiUserWrongBook::getUserId, userId)
                .eq(isResolved != null, YukiUserWrongBook::getIsResolved, isResolved)
                .orderByDesc(YukiUserWrongBook::getUpdatedAt);
        Page<YukiUserWrongBook> pageResult = wrongBookMapper.selectPage(new Page<>(page, size), wrapper);

        Page<WrongBookResponse> result = new Page<>(page, size, pageResult.getTotal());
        List<WrongBookResponse> records = pageResult.getRecords().stream().map(this::toResponse).toList();
        result.setRecords(records);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markResolved(Long id, Long userId) {
        YukiUserWrongBook record = wrongBookMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("错题记录不存在：" + id);
        }
        record.setIsResolved(1);
        wrongBookMapper.updateById(record);
    }

    public WrongBookResponse getRetryQuestion(Long questionItemId, Long userId) {
        YukiUserWrongBook record = wrongBookMapper.selectOne(
                new LambdaQueryWrapper<YukiUserWrongBook>()
                        .eq(YukiUserWrongBook::getUserId, userId)
                        .eq(YukiUserWrongBook::getQuestionItemId, questionItemId));
        if (record == null) {
            throw new ResourceNotFoundException("错题记录不存在");
        }
        return toResponse(record);
    }

    private WrongBookResponse toResponse(YukiUserWrongBook record) {
        WrongBookResponse resp = new WrongBookResponse();
        resp.setId(record.getId());
        resp.setUserId(record.getUserId());
        resp.setQuestionItemId(record.getQuestionItemId());
        resp.setUserAnswer(record.getUserAnswer());
        resp.setIsResolved(record.getIsResolved());
        resp.setWrongCount(record.getWrongCount());

        YukiQuestionItem item = itemMapper.selectById(record.getQuestionItemId());
        if (item != null) {
            resp.setContent(item.getContent());
            resp.setOptions(item.getOptions() != null ? item.getOptions().toString() : null);
            resp.setCorrectAnswer(item.getCorrectAnswer());
            YukiQuestionMain main = mainMapper.selectById(item.getMainId());
            if (main != null) {
                resp.setSection(main.getSection());
                resp.setLevel(main.getLevel());
                resp.setPaperId(main.getPaperId());
                YukiPaper paper = paperMapper.selectById(main.getPaperId());
                if (paper != null) {
                    resp.setPaperName(paper.getPaperName());
                }
            }
        }
        return resp;
    }
}
