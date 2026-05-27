package com.yuki.test.service;

import com.yuki.test.dto.exam.ExamSubmitRequest;
import com.yuki.test.entity.YukiPaper;
import com.yuki.test.entity.YukiQuestionItem;
import com.yuki.test.entity.YukiQuestionMain;
import com.yuki.test.mapper.YukiExamAnswerMapper;
import com.yuki.test.mapper.YukiExamAttemptMapper;
import com.yuki.test.mapper.YukiPaperMapper;
import com.yuki.test.mapper.YukiQuestionItemMapper;
import com.yuki.test.mapper.YukiQuestionMainMapper;
import com.yuki.test.mapper.YukiUserWrongBookMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class YukiExamScoringServiceTest {

    @Test
    void scoresAttemptAndUpsertsWrongQuestions() {
        YukiQuestionItemMapper itemMapper = mock(YukiQuestionItemMapper.class);
        YukiUserWrongBookMapper wrongBookMapper = mock(YukiUserWrongBookMapper.class);
        YukiExamAttemptMapper attemptMapper = mock(YukiExamAttemptMapper.class);
        YukiExamAnswerMapper answerMapper = mock(YukiExamAnswerMapper.class);
        YukiPaperMapper paperMapper = mock(YukiPaperMapper.class);
        YukiQuestionMainMapper questionMainMapper = mock(YukiQuestionMainMapper.class);

        YukiPaper paper = new YukiPaper();
        paper.setId(11L);
        paper.setIsPublished(1);
        when(paperMapper.selectById(11L)).thenReturn(paper);

        YukiQuestionMain main = new YukiQuestionMain();
        main.setId(100L);
        main.setPaperId(11L);
        when(questionMainMapper.selectBatchIds(anyCollection())).thenReturn(List.of(main));

        YukiQuestionItem first = item(1L, 100L, "A");
        YukiQuestionItem second = item(2L, 100L, "C");
        when(itemMapper.selectBatchIds(anyCollection())).thenReturn(List.of(first, second));
        doAnswer(invocation -> {
            invocation.getArgument(0, com.yuki.test.entity.YukiExamAttempt.class).setId(99L);
            return 1;
        }).when(attemptMapper).insert(any(com.yuki.test.entity.YukiExamAttempt.class));

        YukiExamScoringService service = new YukiExamScoringService(
                itemMapper, wrongBookMapper, attemptMapper, answerMapper, paperMapper, questionMainMapper);

        ExamSubmitRequest request = new ExamSubmitRequest();
        request.setUserId(7L);
        request.setPaperId(11L);
        request.setAnswers(List.of(
                new ExamSubmitRequest.AnswerItem(1L, "a"),
                new ExamSubmitRequest.AnswerItem(2L, "B")
        ));

        var response = service.submit(request);

        assertThat(response.getAttemptId()).isEqualTo(99L);
        assertThat(response.getTotal()).isEqualTo(2);
        assertThat(response.getCorrect()).isEqualTo(1);
        assertThat(response.getWrong()).isEqualTo(1);
        assertThat(response.getScore()).isEqualTo(50);

        verify(wrongBookMapper).upsertWrongQuestion(7L, 2L, "B");
        ArgumentCaptor<com.yuki.test.entity.YukiExamAnswer> answerCaptor =
                ArgumentCaptor.forClass(com.yuki.test.entity.YukiExamAnswer.class);
        verify(answerMapper, times(2)).insert(answerCaptor.capture());
        assertThat(answerCaptor.getAllValues()).extracting("isCorrect").containsExactly(1, 0);
    }

    private YukiQuestionItem item(Long id, Long mainId, String correctAnswer) {
        YukiQuestionItem item = new YukiQuestionItem();
        item.setId(id);
        item.setMainId(mainId);
        item.setCorrectAnswer(correctAnswer);
        return item;
    }
}
