package com.yuki.test.service;

import com.yuki.test.dto.exam.ExamSubmitRequest;
import com.yuki.test.dto.exam.ExamSubmitResponse;
import com.yuki.test.entity.YukiExamAnswer;
import com.yuki.test.entity.YukiExamAttempt;
import com.yuki.test.entity.YukiQuestionItem;
import com.yuki.test.entity.YukiPaper;
import com.yuki.test.entity.YukiQuestionMain;
import com.yuki.test.exception.ResourceNotFoundException;
import com.yuki.test.mapper.YukiExamAnswerMapper;
import com.yuki.test.mapper.YukiExamAttemptMapper;
import com.yuki.test.mapper.YukiPaperMapper;
import com.yuki.test.mapper.YukiQuestionItemMapper;
import com.yuki.test.mapper.YukiQuestionMainMapper;
import com.yuki.test.mapper.YukiUserWrongBookMapper;
import com.yuki.test.util.YukiAnswerUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YukiExamScoringService {

    private final YukiQuestionItemMapper questionItemMapper;
    private final YukiUserWrongBookMapper wrongBookMapper;
    private final YukiExamAttemptMapper attemptMapper;
    private final YukiExamAnswerMapper answerMapper;
    private final YukiPaperMapper paperMapper;
    private final YukiQuestionMainMapper questionMainMapper;

    @Transactional(rollbackFor = Exception.class)
    public ExamSubmitResponse submit(ExamSubmitRequest request) {
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new IllegalArgumentException("answers cannot be empty");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId cannot be null");
        }

        YukiPaper paper = paperMapper.selectById(request.getPaperId());
        if (paper == null) {
            throw new ResourceNotFoundException("试卷不存在：" + request.getPaperId());
        }
        if (paper.getIsPublished() != 1) {
            throw new IllegalArgumentException("试卷未发布");
        }

        Set<Long> questionIds = request.getAnswers().stream()
                .map(ExamSubmitRequest.AnswerItem::getQuestionItemId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (questionIds.size() != request.getAnswers().size()) {
            throw new IllegalArgumentException("同一张交卷请求中不能重复提交同一道题");
        }

        List<YukiQuestionItem> questionItems = questionItemMapper.selectBatchIds(questionIds);
        Map<Long, YukiQuestionItem> itemMap = questionItems.stream()
                .collect(Collectors.toMap(YukiQuestionItem::getId, Function.identity()));

        Set<Long> mainIds = questionItems.stream()
                .map(YukiQuestionItem::getMainId)
                .collect(Collectors.toSet());
        List<YukiQuestionMain> mains = questionMainMapper.selectBatchIds(mainIds);
        boolean hasForeign = mains.stream()
                .anyMatch(m -> !m.getPaperId().equals(request.getPaperId()));
        if (hasForeign) {
            throw new IllegalArgumentException("部分题目不属于当前试卷");
        }

        int total = request.getAnswers().size();
        int correct = 0;

        for (ExamSubmitRequest.AnswerItem answer : request.getAnswers()) {
            YukiAnswerUtils.requireOption(answer.getUserAnswer());
            YukiQuestionItem item = itemMap.get(answer.getQuestionItemId());
            if (item == null) {
                throw new ResourceNotFoundException("题目不存在：" + answer.getQuestionItemId());
            }

            String userAnswer = YukiAnswerUtils.normalize(answer.getUserAnswer());
            String correctAnswer = YukiAnswerUtils.normalize(item.getCorrectAnswer());
            if (correctAnswer.equals(userAnswer)) {
                correct++;
            }
        }

        int score = Math.round(correct * 100.0f / total);
        YukiExamAttempt attempt = new YukiExamAttempt();
        attempt.setUserId(request.getUserId());
        attempt.setPaperId(request.getPaperId());
        attempt.setTotalCount(total);
        attempt.setCorrectCount(correct);
        attempt.setScore(score);
        attempt.setSubmittedAt(LocalDateTime.now());
        attemptMapper.insert(attempt);

        for (ExamSubmitRequest.AnswerItem answer : request.getAnswers()) {
            YukiQuestionItem item = itemMap.get(answer.getQuestionItemId());
            String userAnswer = YukiAnswerUtils.normalize(answer.getUserAnswer());
            String correctAnswer = YukiAnswerUtils.normalize(item.getCorrectAnswer());
            boolean isCorrect = correctAnswer.equals(userAnswer);

            YukiExamAnswer examAnswer = new YukiExamAnswer();
            examAnswer.setAttemptId(attempt.getId());
            examAnswer.setQuestionItemId(item.getId());
            examAnswer.setUserAnswer(userAnswer);
            examAnswer.setCorrectAnswer(correctAnswer);
            examAnswer.setIsCorrect(isCorrect ? 1 : 0);
            answerMapper.insert(examAnswer);

            if (!isCorrect) {
                wrongBookMapper.upsertWrongQuestion(request.getUserId(), item.getId(), userAnswer);
            }
        }

        return new ExamSubmitResponse(attempt.getId(), total, correct, total - correct, score);
    }
}
