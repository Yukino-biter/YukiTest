package com.yuki.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "yuki.crypto")
public class YukiCryptoProperties {
    private String secretKey = "YukiTestAesKeyMustBeExactly32B";
}
