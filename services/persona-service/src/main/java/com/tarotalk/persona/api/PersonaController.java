package com.tarotalk.persona.api;

import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.persona.service.PersonaService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/personas")
@Validated
public class PersonaController {
    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<PersonaResponse> getByUser(@PathVariable UUID userId) {
        return ApiResponse.ok(PersonaResponse.from(personaService.getByUserId(userId)));
    }

    @PostMapping
    public ApiResponse<PersonaResponse> create(@Valid @RequestBody CreatePersonaRequest request) {
        return ApiResponse.ok(PersonaResponse.from(personaService.createPersona(request)));
    }

    @PutMapping("/{personaId}")
    public ApiResponse<PersonaResponse> update(@PathVariable UUID personaId,
                                               @Valid @RequestBody UpdatePersonaRequest request) {
        return ApiResponse.ok(PersonaResponse.from(personaService.updatePersona(personaId, request)));
    }
}
