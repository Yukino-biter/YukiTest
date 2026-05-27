package com.yuki.test.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yuki.test.dto.stats.LearningStatsResponse;
import com.yuki.test.entity.YukiExamAnswer;
import com.yuki.test.entity.YukiExamAttempt;
import com.yuki.test.entity.YukiPaper;
import com.yuki.test.entity.YukiQuestionItem;
import com.yuki.test.entity.YukiQuestionMain;
import com.yuki.test.entity.YukiUserWrongBook;
import com.yuki.test.mapper.YukiExamAnswerMapper;
import com.yuki.test.mapper.YukiExamAttemptMapper;
import com.yuki.test.mapper.YukiPaperMapper;
import com.yuki.test.mapper.YukiQuestionItemMapper;
import com.yuki.test.mapper.YukiQuestionMainMapper;
import com.yuki.test.mapper.YukiUserWrongBookMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class YukiStatsService {

    private final YukiExamAttemptMapper attemptMapper;
    private final YukiExamAnswerMapper answerMapper;
    private final YukiPaperMapper paperMapper;
    private final YukiQuestionItemMapper itemMapper;
    private final YukiQuestionMainMapper mainMapper;
    private final YukiUserWrongBookMapper wrongBookMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public LearningStatsResponse getStats(Long userId) {
        LearningStatsResponse resp = new LearningStatsResponse();

        List<YukiExamAttempt> attempts = attemptMapper.selectList(
                new LambdaQueryWrapper<YukiExamAttempt>()
                        .eq(YukiExamAttempt::getUserId, userId)
                        .orderByDesc(YukiExamAttempt::getSubmittedAt));

        resp.setTotalExams(attempts.size());

        if (attempts.isEmpty()) {
            resp.setTotalQuestions(0);
            resp.setTotalCorrect(0);
            resp.setOverallCorrectRate(0);
            resp.setRecentExams(List.of());
            resp.setSectionStats(Map.of());
        } else {
            int totalQ = attempts.stream().mapToInt(YukiExamAttempt::getTotalCount).sum();
            int totalC = attempts.stream().mapToInt(YukiExamAttempt::getCorrectCount).sum();
            resp.setTotalQuestions(totalQ);
            resp.setTotalCorrect(totalC);
            resp.setOverallCorrectRate(totalQ > 0 ? Math.round(totalC * 10000.0 / totalQ) / 100.0 : 0);

            List<YukiExamAttempt> recent = attempts.size() > 10 ? attempts.subList(0, 10) : attempts;
            List<LearningStatsResponse.RecentExam> recentExams = recent.stream().map(a -> {
                LearningStatsResponse.RecentExam re = new LearningStatsResponse.RecentExam();
                re.setAttemptId(a.getId());
                re.setPaperId(a.getPaperId());
                re.setScore(a.getScore());
                re.setTotal(a.getTotalCount());
                re.setCorrect(a.getCorrectCount());
                re.setSubmittedAt(a.getSubmittedAt() != null ? a.getSubmittedAt().format(FMT) : "");
                YukiPaper paper = paperMapper.selectById(a.getPaperId());
                if (paper != null) {
                    re.setPaperName(paper.getPaperName());
                    re.setLevel(paper.getLevel());
                }
                return re;
            }).toList();
            resp.setRecentExams(recentExams);

            resp.setSectionStats(buildSectionStats(userId));
        }

        long wrongTotal = wrongBookMapper.selectCount(
                new LambdaQueryWrapper<YukiUserWrongBook>().eq(YukiUserWrongBook::getUserId, userId));
        long wrongUnresolved = wrongBookMapper.selectCount(
                new LambdaQueryWrapper<YukiUserWrongBook>()
                        .eq(YukiUserWrongBook::getUserId, userId)
                        .eq(YukiUserWrongBook::getIsResolved, 0));
        resp.setWrongBookTotal((int) wrongTotal);
        resp.setWrongBookUnresolved((int) wrongUnresolved);

        return resp;
    }

    private Map<String, LearningStatsResponse.SectionStats> buildSectionStats(Long userId) {
        List<YukiExamAttempt> attempts = attemptMapper.selectList(
                new LambdaQueryWrapper<YukiExamAttempt>()
                        .eq(YukiExamAttempt::getUserId, userId));
        if (attempts.isEmpty()) {
            return Map.of();
        }

        List<Long> attemptIds = attempts.stream().map(YukiExamAttempt::getId).toList();
        List<YukiExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<YukiExamAnswer>().in(YukiExamAnswer::getAttemptId, attemptIds));
        if (answers.isEmpty()) {
            return Map.of();
        }

        List<Long> itemIds = answers.stream().map(YukiExamAnswer::getQuestionItemId).distinct().toList();
        Map<Long, YukiQuestionItem> itemMap = new HashMap<>();
        for (List<Long> batch : partition(itemIds, 500)) {
            itemMapper.selectBatchIds(batch).forEach(i -> itemMap.put(i.getId(), i));
        }

        List<Long> mainIds = itemMap.values().stream()
                .map(YukiQuestionItem::getMainId).distinct().toList();
        Map<Long, YukiQuestionMain> mainMap = new HashMap<>();
        for (List<Long> batch : partition(mainIds, 500)) {
            mainMapper.selectBatchIds(batch).forEach(m -> mainMap.put(m.getId(), m));
        }

        Map<String, int[]> sectionData = new HashMap<>();
        for (YukiExamAnswer ans : answers) {
            YukiQuestionItem item = itemMap.get(ans.getQuestionItemId());
            if (item == null) continue;
            YukiQuestionMain main = mainMap.get(item.getMainId());
            if (main == null) continue;
            String section = main.getSection();
            int[] counts = sectionData.computeIfAbsent(section, k -> new int[]{0, 0});
            counts[0]++;
            if (ans.getIsCorrect() == 1) counts[1]++;
        }

        Map<String, LearningStatsResponse.SectionStats> result = new HashMap<>();
        sectionData.forEach((section, counts) -> {
            LearningStatsResponse.SectionStats ss = new LearningStatsResponse.SectionStats();
            ss.setTotal(counts[0]);
            ss.setCorrect(counts[1]);
            ss.setCorrectRate(counts[0] > 0 ? Math.round(counts[1] * 10000.0 / counts[0]) / 100.0 : 0);
            result.put(section, ss);
        });
        return result;
    }

    private List<List<Long>> partition(List<Long> list, int size) {
        return java.util.stream.IntStream.range(0, (list.size() + size - 1) / size)
                .mapToObj(i -> list.subList(i * size, Math.min((i + 1) * size, list.size())))
                .toList();
    }
}
