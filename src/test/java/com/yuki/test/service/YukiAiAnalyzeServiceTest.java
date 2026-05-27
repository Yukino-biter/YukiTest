package com.yuki.test.service;

import com.yuki.test.config.YukiAiProperties;
import com.yuki.test.entity.YukiUser;
import com.yuki.test.util.YukiCryptoUtil;
import com.yuki.test.util.YukiRateLimiter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class YukiAiAnalyzeServiceTest {

    @Test
    void usesUserBaseUrlAndModelWhenConfigured() {
        YukiAiAnalyzeService service = newService();
        YukiUser user = new YukiUser();
        user.setBaseUrl("https://custom.example.com/v1/");
        user.setApiModel("custom-model");

        assertThat(service.resolveBaseUrl(user)).isEqualTo("https://custom.example.com/v1");
        assertThat(service.resolveModel(user)).isEqualTo("custom-model");
    }

    @Test
    void fallsBackToDeepSeekDefaultsWhenUserOnlyHasApiKey() {
        YukiAiAnalyzeService service = newService();
        YukiUser user = new YukiUser();
        user.setApiKey("sk-test");

        assertThat(service.hasApiKey(user)).isTrue();
        assertThat(service.resolveBaseUrl(user)).isEqualTo("https://api.deepseek.com/v1");
        assertThat(service.resolveModel(user)).isEqualTo("deepseek-chat");
    }

    @Test
    void treatsBlankApiKeyAsNoKey() {
        YukiAiAnalyzeService service = newService();
        YukiUser user = new YukiUser();
        user.setApiKey(" ");

        assertThat(service.hasApiKey(user)).isFalse();
    }

    private YukiAiAnalyzeService newService() {
        YukiAiProperties properties = new YukiAiProperties();
        properties.setDefaultBaseUrl("https://api.deepseek.com/v1");
        properties.setDefaultModel("deepseek-chat");
        return new YukiAiAnalyzeService(null, null, null, null, properties, Runnable::run,
                mock(YukiCryptoUtil.class), mock(YukiRateLimiter.class));
    }
}
