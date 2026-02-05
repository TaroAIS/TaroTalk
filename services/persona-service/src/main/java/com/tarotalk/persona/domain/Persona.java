package com.tarotalk.persona.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "personas")
public class Persona {
    @Id
    @Column(name = "persona_id", nullable = false, updatable = false)
    private UUID personaId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Lob
    @Column(nullable = false)
    private String description;

    @Lob
    @Column(name = "traits_json")
    private String traitsJson;

    @Lob
    @Column(name = "embedding_vector")
    private String embeddingVector;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Persona() {
    }

    public Persona(UUID personaId, UUID userId, String description) {
        this.personaId = personaId;
        this.userId = userId;
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public UUID getPersonaId() {
        return personaId;
    }

    public void setPersonaId(UUID personaId) {
        this.personaId = personaId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTraitsJson() {
        return traitsJson;
    }

    public void setTraitsJson(String traitsJson) {
        this.traitsJson = traitsJson;
    }

    public String getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(String embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
