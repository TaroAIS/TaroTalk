package com.tarotalk.auth.api;

import javax.validation.constraints.NotBlank;

public class RefreshRequest {
    @NotBlank
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
