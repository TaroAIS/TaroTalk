package com.tarotalk.chat.api;

public class MediaUploadResponse {
    private String mediaUrl;

    public MediaUploadResponse() {
    }

    public MediaUploadResponse(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
}
