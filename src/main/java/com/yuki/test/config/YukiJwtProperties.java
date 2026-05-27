package com.yuki.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "yuki.jwt")
public class YukiJwtProperties {
    private String secret = "YukiTestDefaultSecretKeyMustBe32BytesLong!!";
    private long expirationMs = 86400000;
}
