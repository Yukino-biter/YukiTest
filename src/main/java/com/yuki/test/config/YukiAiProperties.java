package com.yuki.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "yuki.ai")
public class YukiAiProperties {
    private String defaultBaseUrl = "https://api.deepseek.com/v1";
    private String defaultModel = "deepseek-chat";
    private long streamTimeoutSeconds = 120;
    private int maxConcurrentPerUser = 3;
}
