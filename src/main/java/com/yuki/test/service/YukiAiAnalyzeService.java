package com.yuki.test.service;

import com.yuki.test.config.YukiAiProperties;
import com.yuki.test.dto.ai.AiAnalyzeRequest;
import com.yuki.test.entity.YukiQuestionItem;
import com.yuki.test.entity.YukiQuestionMain;
import com.yuki.test.entity.YukiUser;
import com.yuki.test.mapper.YukiQuestionItemMapper;
import com.yuki.test.mapper.YukiQuestionMainMapper;
import com.yuki.test.mapper.YukiUserMapper;
import com.yuki.test.util.YukiCryptoUtil;
import com.yuki.test.util.YukiRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class YukiAiAnalyzeService {

    private final YukiUserMapper userMapper;
    private final YukiQuestionItemMapper itemMapper;
    private final YukiQuestionMainMapper mainMapper;
    private final WebClient.Builder webClientBuilder;
    private final YukiAiProperties aiProperties;
    private final Executor yukiAiExecutor;
    private final YukiCryptoUtil cryptoUtil;
    private final YukiRateLimiter rateLimiter;

    public SseEmitter analyze(AiAnalyzeRequest request) {
        long timeoutMillis = Duration.ofSeconds(aiProperties.getStreamTimeoutSeconds()).toMillis();
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        if (request.getUserId() == null) {
            sendAndComplete(emitter, "error", "userId 不能为空");
            return emitter;
        }
        if (!rateLimiter.tryAcquire(request.getUserId(), aiProperties.getMaxConcurrentPerUser())) {
            sendAndComplete(emitter, "error", "并发解析请求过多，请稍后再试");
            return emitter;
        }
        emitter.onCompletion(() -> rateLimiter.release(request.getUserId()));
        emitter.onError(ex -> rateLimiter.release(request.getUserId()));
        emitter.onTimeout(() -> rateLimiter.release(request.getUserId()));
        yukiAiExecutor.execute(() -> doAnalyze(request, emitter));
        return emitter;
    }

    private void doAnalyze(AiAnalyzeRequest request, SseEmitter emitter) {
        try {
            YukiUser user = userMapper.selectById(request.getUserId());
            if (user == null) {
                sendAndComplete(emitter, "error", "用户不存在");
                return;
            }

            YukiQuestionItem item = itemMapper.selectById(request.getQuestionItemId());
            if (item == null) {
                sendAndComplete(emitter, "error", "题目不存在");
                return;
            }

            if (!hasApiKey(user)) {
                safeSend(emitter, "no_key", "未配置有效 ApiKey，仅展示标准答案。");
                safeSend(emitter, "answer", item.getCorrectAnswer());
                safeSend(emitter, "done", "");
                emitter.complete();
                return;
            }

            YukiQuestionMain main = mainMapper.selectById(item.getMainId());
            if (main == null) {
                sendAndComplete(emitter, "error", "题目主表材料不存在");
                return;
            }

            proxyDeepSeekCompatibleStream(emitter, user, main, item);
        } catch (Exception ex) {
            log.warn("Yuki AI analyze failed", ex);
            sendAndComplete(emitter, "error", "解析失败：" + ex.getMessage());
        }
    }

    private void proxyDeepSeekCompatibleStream(SseEmitter emitter,
                                               YukiUser user,
                                               YukiQuestionMain main,
                                               YukiQuestionItem item) {
        String baseUrl = resolveBaseUrl(user);
        String model = resolveModel(user);
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", buildSystemPrompt()),
                        Map.of("role", "user", "content", buildUserPrompt(main, item))
                )
        );

        WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();
        webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .headers(headers -> headers.setBearerAuth(cryptoUtil.decrypt(user.getApiKey()).trim()))
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnNext(chunk -> safeSend(emitter, "message", chunk))
                .doOnError(ex -> {
                    log.warn("AI upstream stream failed", ex);
                    sendAndComplete(emitter, "error", "AI解析流中断：" + ex.getMessage());
                })
                .doOnComplete(() -> {
                    safeSend(emitter, "done", "");
                    emitter.complete();
                })
                .blockLast(Duration.ofSeconds(aiProperties.getStreamTimeoutSeconds()));
    }

    boolean hasApiKey(YukiUser user) {
        return user != null && user.getApiKey() != null && !user.getApiKey().isBlank();
    }

    String resolveBaseUrl(YukiUser user) {
        String value = user != null && user.getBaseUrl() != null && !user.getBaseUrl().isBlank()
                ? user.getBaseUrl().trim()
                : aiProperties.getDefaultBaseUrl();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    String resolveModel(YukiUser user) {
        if (user != null && user.getApiModel() != null && !user.getApiModel().isBlank()) {
            return user.getApiModel().trim();
        }
        return aiProperties.getDefaultModel();
    }

    private String buildSystemPrompt() {
        return """
                你是专业的 JLPT 日语考级讲师，只讲解 N1-N5 的选择题。
                必须基于用户提供的官方正确答案进行分析，不得自行更改答案。
                输出 Markdown，结构固定为：
                ## 中文翻译
                ## 核心考点
                ## 正确答案解析
                ## 干扰项错因分析
                语言简洁、准确，面向中文母语学习者。
                """;
    }

    private String buildUserPrompt(YukiQuestionMain main, YukiQuestionItem item) {
        return """
                JLPT等级：%s
                题型：%s
                大题标题：%s

                材料：
                %s

                题干：
                %s

                选项：
                %s

                官方正确答案：
                %s

                请严格围绕官方正确答案解析。不要猜答案，不要改答案。
                """.formatted(
                main.getLevel(),
                main.getSection(),
                main.getTitle() == null ? "无" : main.getTitle(),
                main.getMaterial() == null || main.getMaterial().isBlank() ? "无" : main.getMaterial(),
                item.getContent(),
                item.getOptions(),
                item.getCorrectAnswer()
        );
    }

    private void safeSend(SseEmitter emitter, String eventName, String message) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(message));
        } catch (IOException ex) {
            log.info("SSE client disconnected");
            emitter.completeWithError(ex);
        }
    }

    private void sendAndComplete(SseEmitter emitter, String eventName, String message) {
        safeSend(emitter, eventName, message);
        emitter.complete();
    }
}
