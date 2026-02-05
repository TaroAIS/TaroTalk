package com.tarotalk.chat.repo;

import com.tarotalk.chat.domain.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {
    List<ConversationParticipant> findByUserId(UUID userId);
    List<ConversationParticipant> findByConversationId(UUID conversationId);
}
