package com.yuki.test.dto.user;

import com.yuki.test.entity.YukiUser;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserAiConfigResponse {
    private Long userId;
    private String apiProvider;
    private Boolean hasApiKey;
    private String baseUrl;
    private String apiModel;

    public static UserAiConfigResponse from(YukiUser user) {
        return new UserAiConfigResponse(
                user.getId(),
                user.getApiProvider(),
                user.getApiKey() != null && !user.getApiKey().isBlank(),
                user.getBaseUrl(),
                user.getApiModel()
        );
    }
}
