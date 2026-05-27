package com.yuki.test.dto.user;

import lombok.Data;

@Data
public class UpdateAiConfigRequest {
    private String apiProvider;
    private String apiKey;
    private String baseUrl;
    private String apiModel;
}
