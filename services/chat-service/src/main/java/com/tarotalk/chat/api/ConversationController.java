package com.tarotalk.chat.api;

import com.tarotalk.chat.domain.Conversation;
import com.tarotalk.chat.domain.ConversationParticipant;
import com.tarotalk.chat.service.ConversationService;
import com.tarotalk.common.api.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversations")
@Validated
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    public ApiResponse<ConversationResponse> create(@Valid @RequestBody CreateConversationRequest request) {
        Conversation conversation = conversationService.createConversation(request);
        List<UUID> participants = conversationService.getParticipants(conversation.getConversationId())
                .stream().map(ConversationParticipant::getUserId).collect(Collectors.toList());
        return ApiResponse.ok(ConversationResponse.from(conversation, participants));
    }

    @GetMapping
    public ApiResponse<List<ConversationResponse>> list(@RequestParam UUID userId) {
        List<ConversationResponse> responses = conversationService.listForUser(userId).stream()
                .map(conversation -> {
                    List<UUID> participants = conversationService.getParticipants(conversation.getConversationId())
                            .stream().map(ConversationParticipant::getUserId).collect(Collectors.toList());
                    return ConversationResponse.from(conversation, participants);
                })
                .collect(Collectors.toList());
        return ApiResponse.ok(responses);
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> detail(@PathVariable UUID conversationId) {
        Conversation conversation = conversationService.getConversation(conversationId);
        List<UUID> participants = conversationService.getParticipants(conversationId)
                .stream().map(ConversationParticipant::getUserId).collect(Collectors.toList());
        return ApiResponse.ok(ConversationResponse.from(conversation, participants));
    }
}
