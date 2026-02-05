package com.tarotalk.chat.repo;

import com.tarotalk.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    Page<ChatMessage> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);
}
