package com.yuki.test.controller;

import com.yuki.test.common.ApiResponse;
import com.yuki.test.dto.exam.ExamResultResponse;
import com.yuki.test.dto.exam.ExamSubmitRequest;
import com.yuki.test.dto.exam.ExamSubmitResponse;
import com.yuki.test.dto.exam.SaveProgressRequest;
import com.yuki.test.entity.YukiExamAnswer;
import com.yuki.test.entity.YukiExamAttempt;
import com.yuki.test.entity.YukiExamDraft;
import com.yuki.test.entity.YukiQuestionItem;
import com.yuki.test.entity.YukiQuestionMain;
import com.yuki.test.exception.ResourceNotFoundException;
import com.yuki.test.interceptor.AuthInterceptor;
import com.yuki.test.mapper.YukiExamAnswerMapper;
import com.yuki.test.mapper.YukiExamAttemptMapper;
import com.yuki.test.mapper.YukiExamDraftMapper;
import com.yuki.test.mapper.YukiQuestionItemMapper;
import com.yuki.test.mapper.YukiQuestionMainMapper;
import com.yuki.test.service.YukiExamScoringService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/yuki/exam")
@RequiredArgsConstructor
public class YukiExamController {

    private final YukiExamScoringService examScoringService;
    private final YukiExamDraftMapper draftMapper;
    private final YukiExamAttemptMapper attemptMapper;
    private final YukiExamAnswerMapper answerMapper;
    private final YukiQuestionItemMapper questionItemMapper;
    private final YukiQuestionMainMapper questionMainMapper;

    @PostMapping("/submit")
    public ApiResponse<ExamSubmitResponse> submit(HttpServletRequest httpRequest,
                                                  @Valid @RequestBody ExamSubmitRequest request) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        request.setUserId(userId);
        return ApiResponse.ok(examScoringService.submit(request));
    }

    @GetMapping("/result/{attemptId}")
    public ApiResponse<ExamResultResponse> result(@PathVariable Long attemptId) {
        YukiExamAttempt attempt = attemptMapper.selectById(attemptId);
        if (attempt == null) throw new ResourceNotFoundException("考试记录不存在");

        // Get all submitted answers for this attempt
        List<YukiExamAnswer> answers = answerMapper.selectList(
                Wrappers.<YukiExamAnswer>lambdaQuery()
                        .eq(YukiExamAnswer::getAttemptId, attemptId));
        Map<Long, YukiExamAnswer> answerMap = new LinkedHashMap<>();
        for (YukiExamAnswer a : answers) answerMap.put(a.getQuestionItemId(), a);

        // Get ALL questions from the paper (not just answered ones)
        List<YukiQuestionMain> mains = questionMainMapper.selectList(
                Wrappers.<YukiQuestionMain>lambdaQuery()
                        .eq(YukiQuestionMain::getPaperId, attempt.getPaperId())
                        .orderByAsc(YukiQuestionMain::getSortOrder));
        Map<Long, YukiQuestionMain> mainMap = new LinkedHashMap<>();
        for (YukiQuestionMain m : mains) mainMap.put(m.getId(), m);

        List<YukiQuestionItem> allItems = new ArrayList<>();
        for (YukiQuestionMain main : mains) {
            List<YukiQuestionItem> items = questionItemMapper.selectList(
                    Wrappers.<YukiQuestionItem>lambdaQuery()
                            .eq(YukiQuestionItem::getMainId, main.getId())
                            .orderByAsc(YukiQuestionItem::getSortOrder));
            allItems.addAll(items);
        }

        // Build section stats and question details for ALL questions
        Map<String, ExamResultResponse.SectionStat> sections = new LinkedHashMap<>();
        sections.put("vocabulary", new ExamResultResponse.SectionStat("文字·词汇", 0, 0, 0));
        sections.put("grammar", new ExamResultResponse.SectionStat("语法", 0, 0, 0));
        sections.put("reading", new ExamResultResponse.SectionStat("阅读", 0, 0, 0));
        sections.put("listening", new ExamResultResponse.SectionStat("听力", 0, 0, 0));

        List<ExamResultResponse.QuestionDetail> questionDetails = new ArrayList<>();
        for (YukiQuestionItem item : allItems) {
            YukiQuestionMain main = mainMap.get(item.getMainId());
            String section = main != null ? main.getSection() : "vocabulary";

            YukiExamAnswer ans = answerMap.get(item.getId());
            String userAnswer = ans != null ? ans.getUserAnswer() : null;
            boolean correct = ans != null && ans.getIsCorrect() == 1;

            ExamResultResponse.SectionStat stat = sections.getOrDefault(section, sections.get("vocabulary"));
            stat.setTotal(stat.getTotal() + 1);
            if (correct) stat.setCorrect(stat.getCorrect() + 1);
            else stat.setWrong(stat.getWrong() + 1);

            questionDetails.add(new ExamResultResponse.QuestionDetail(
                    item.getId(),
                    item.getMainId(),
                    section,
                    userAnswer,
                    item.getCorrectAnswer(),
                    correct
            ));
        }

        // Compute actual totals from all paper questions
        int totalAll = 0, correctAll = 0, wrongAll = 0;
        for (ExamResultResponse.SectionStat s : sections.values()) {
            totalAll += s.getTotal();
            correctAll += s.getCorrect();
            wrongAll += s.getWrong();
        }

        return ApiResponse.ok(new ExamResultResponse(
                attempt.getId(), attempt.getScore(),
                totalAll, correctAll, wrongAll,
                sections, questionDetails));
    }

    @PostMapping("/save-progress")
    public ApiResponse<Void> saveProgress(HttpServletRequest httpRequest,
                                          @Valid @RequestBody SaveProgressRequest request) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        request.setUserId(userId);

        String answersJson = null;
        if (request.getAnswers() != null) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                answersJson = mapper.writeValueAsString(request.getAnswers());
            } catch (Exception e) {
                return ApiResponse.fail(500, "保存失败");
            }
        }

        YukiExamDraft existing = draftMapper.selectOne(
                Wrappers.<YukiExamDraft>lambdaQuery()
                        .eq(YukiExamDraft::getUserId, userId)
                        .eq(YukiExamDraft::getPaperId, request.getPaperId()));

        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setAnswers(answersJson);
            existing.setTimeLeft(request.getTimeLeft());
            existing.setSavedAt(now);
            draftMapper.updateById(existing);
        } else {
            YukiExamDraft draft = new YukiExamDraft();
            draft.setUserId(userId);
            draft.setPaperId(request.getPaperId());
            draft.setAnswers(answersJson);
            draft.setTimeLeft(request.getTimeLeft());
            draft.setSavedAt(now);
            draftMapper.insert(draft);
        }

        return ApiResponse.ok(null);
    }

    @GetMapping("/draft/{paperId}")
    public ApiResponse<YukiExamDraft> getDraft(HttpServletRequest httpRequest,
                                               @PathVariable Long paperId) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        YukiExamDraft draft = draftMapper.selectOne(
                Wrappers.<YukiExamDraft>lambdaQuery()
                        .eq(YukiExamDraft::getUserId, userId)
                        .eq(YukiExamDraft::getPaperId, paperId));
        return ApiResponse.ok(draft);
    }

    @PostMapping("/clear-draft/{paperId}")
    public ApiResponse<Void> clearDraft(HttpServletRequest httpRequest,
                                        @PathVariable Long paperId) {
        Long userId = (Long) httpRequest.getAttribute(AuthInterceptor.CURRENT_USER_ID);
        draftMapper.delete(
                Wrappers.<YukiExamDraft>lambdaQuery()
                        .eq(YukiExamDraft::getUserId, userId)
                        .eq(YukiExamDraft::getPaperId, paperId));
        return ApiResponse.ok(null);
    }
}
