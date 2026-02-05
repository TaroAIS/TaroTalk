package com.tarotalk.persona.api;

import com.tarotalk.persona.domain.Persona;

import java.time.Instant;
import java.util.UUID;

public class PersonaResponse {
    private UUID personaId;
    private UUID userId;
    private String description;
    private String traitsJson;
    private Instant updatedAt;

    public static PersonaResponse from(Persona persona) {
        PersonaResponse response = new PersonaResponse();
        response.personaId = persona.getPersonaId();
        response.userId = persona.getUserId();
        response.description = persona.getDescription();
        response.traitsJson = persona.getTraitsJson();
        response.updatedAt = persona.getUpdatedAt();
        return response;
    }

    public UUID getPersonaId() {
        return personaId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public String getTraitsJson() {
        return traitsJson;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
