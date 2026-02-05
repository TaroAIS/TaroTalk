package com.tarotalk.ai.api;

import com.tarotalk.ai.service.AiService;
import com.tarotalk.common.api.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/ai")
@Validated
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/persona")
    public ApiResponse<PersonaGenerateResponse> generatePersona(@Valid @RequestBody PersonaGenerateRequest request) {
        return ApiResponse.ok(aiService.generatePersona(request));
    }

    @PostMapping("/reply")
    public ApiResponse<AiReplyResponse> reply(@Valid @RequestBody AiReplyRequest request) {
        return ApiResponse.ok(aiService.generateReply(request));
    }

    @PostMapping("/feed")
    public ApiResponse<AiFeedResponse> feed(@Valid @RequestBody AiFeedRequest request) {
        return ApiResponse.ok(aiService.generateFeed(request));
    }
}
