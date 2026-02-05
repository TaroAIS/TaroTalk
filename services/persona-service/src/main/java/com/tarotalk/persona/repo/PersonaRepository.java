package com.tarotalk.persona.repo;

import com.tarotalk.persona.domain.Persona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PersonaRepository extends JpaRepository<Persona, UUID> {
    Optional<Persona> findByUserId(UUID userId);
}
