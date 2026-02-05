package com.tarotalk.ai.api;

public class AiFeedRequest {
    private String personaSummary;
    private String topic;

    public String getPersonaSummary() {
        return personaSummary;
    }

    public void setPersonaSummary(String personaSummary) {
        this.personaSummary = personaSummary;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
