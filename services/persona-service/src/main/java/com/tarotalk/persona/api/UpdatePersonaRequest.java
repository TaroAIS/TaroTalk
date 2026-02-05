package com.tarotalk.persona.api;

import javax.validation.constraints.NotBlank;

public class UpdatePersonaRequest {
    @NotBlank
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
