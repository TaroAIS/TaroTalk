package com.tarotalk.persona.service;

import com.tarotalk.common.exception.ApiException;
import com.tarotalk.persona.api.CreatePersonaRequest;
import com.tarotalk.persona.api.UpdatePersonaRequest;
import com.tarotalk.persona.domain.Persona;
import com.tarotalk.persona.repo.PersonaRepository;
import com.tarotalk.persona.service.dto.PersonaGenerationResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class PersonaService {
    private final PersonaRepository personaRepository;
    private final AiClient aiClient;
    private final EmbeddingService embeddingService;
    private final VectorStoreClient vectorStoreClient;

    public PersonaService(PersonaRepository personaRepository,
                          AiClient aiClient,
                          EmbeddingService embeddingService,
                          VectorStoreClient vectorStoreClient) {
        this.personaRepository = personaRepository;
        this.aiClient = aiClient;
        this.embeddingService = embeddingService;
        this.vectorStoreClient = vectorStoreClient;
    }

    public Persona createPersona(CreatePersonaRequest request) {
        if (personaRepository.findByUserId(request.getUserId()).isPresent()) {
            throw new ApiException("ALREADY_EXISTS", "persona already exists");
        }
        PersonaGenerationResult result = aiClient.generatePersona(request.getUserId().toString(), request.getDescription());
        Persona persona = new Persona(UUID.randomUUID(), request.getUserId(), result.getSummary());
        persona.setTraitsJson(result.toTraitsJson());
        double[] embedding = embeddingService.embed(result.getSummary());
        persona.setEmbeddingVector(embeddingService.serialize(embedding));
        persona.setUpdatedAt(Instant.now());
        Persona saved = personaRepository.save(persona);
        vectorStoreClient.upsert(saved.getPersonaId().toString(), embedding, saved.getDescription());
        return saved;
    }

    public Persona getByUserId(UUID userId) {
        return personaRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "persona not found"));
    }

    public Persona updatePersona(UUID personaId, UpdatePersonaRequest request) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "persona not found"));
        persona.setDescription(request.getDescription());
        double[] embedding = embeddingService.embed(request.getDescription());
        persona.setEmbeddingVector(embeddingService.serialize(embedding));
        persona.setUpdatedAt(Instant.now());
        Persona saved = personaRepository.save(persona);
        vectorStoreClient.upsert(saved.getPersonaId().toString(), embedding, saved.getDescription());
        return saved;
    }
}
