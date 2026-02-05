package com.tarotalk.chat.api;

import com.tarotalk.chat.domain.ChatMessage;
import com.tarotalk.chat.service.MessageService;
import com.tarotalk.common.api.ApiResponse;
import com.tarotalk.common.api.PageResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Validated
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResponse<MessageResponse> send(@PathVariable UUID conversationId,
                                             @Valid @RequestBody MessageSendRequest request) {
        return ApiResponse.ok(MessageResponse.from(messageService.sendMessage(conversationId, request)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<PageResponse<MessageResponse>> list(@PathVariable UUID conversationId,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        PageResponse<ChatMessage> messages = messageService.listMessages(conversationId, page, size);
        PageResponse<MessageResponse> response = new PageResponse<>();
        response.setItems(messages.getItems().stream().map(MessageResponse::from).collect(Collectors.toList()));
        response.setPage(page);
        response.setSize(size);
        response.setTotal(messages.getTotal());
        return ApiResponse.ok(response);
    }

    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<MessageResponse> delete(@PathVariable String messageId) {
        return ApiResponse.ok(MessageResponse.from(messageService.deleteMessage(messageId)));
    }

    @PostMapping("/media/upload")
    public ApiResponse<MediaUploadResponse> upload() {
        return ApiResponse.ok(new MediaUploadResponse("https://example.com/media/placeholder"));
    }
}
