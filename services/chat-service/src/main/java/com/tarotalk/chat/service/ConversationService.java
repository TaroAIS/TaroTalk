package com.tarotalk.chat.service;

import com.tarotalk.chat.api.CreateConversationRequest;
import com.tarotalk.chat.domain.Conversation;
import com.tarotalk.chat.domain.ConversationParticipant;
import com.tarotalk.chat.repo.ConversationParticipantRepository;
import com.tarotalk.chat.repo.ConversationRepository;
import com.tarotalk.common.exception.ApiException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationParticipantRepository participantRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
    }

    public Conversation createConversation(CreateConversationRequest request) {
        Conversation conversation = new Conversation(UUID.randomUUID(), request.getType(), request.getTitle());
        conversation.setUpdatedAt(Instant.now());
        Conversation saved = conversationRepository.save(conversation);
        List<ConversationParticipant> participants = new ArrayList<>();
        boolean first = true;
        for (String participantId : request.getParticipantIds()) {
            UUID userId = UUID.fromString(participantId);
            ConversationParticipant.Role role = first ? ConversationParticipant.Role.OWNER : ConversationParticipant.Role.MEMBER;
            first = false;
            participants.add(new ConversationParticipant(UUID.randomUUID(), saved.getConversationId(), userId, role));
        }
        participantRepository.saveAll(participants);
        return saved;
    }

    public Conversation getConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", "conversation not found"));
    }

    public List<Conversation> listForUser(UUID userId) {
        List<ConversationParticipant> participants = participantRepository.findByUserId(userId);
        List<Conversation> conversations = new ArrayList<>();
        for (ConversationParticipant participant : participants) {
            conversationRepository.findById(participant.getConversationId()).ifPresent(conversations::add);
        }
        return conversations;
    }

    public List<ConversationParticipant> getParticipants(UUID conversationId) {
        return participantRepository.findByConversationId(conversationId);
    }
}
