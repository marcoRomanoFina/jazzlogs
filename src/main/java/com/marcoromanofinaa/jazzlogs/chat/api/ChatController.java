package com.marcoromanofinaa.jazzlogs.chat.api;

import com.marcoromanofinaa.jazzlogs.auth.security.AuthenticatedUser;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.ChatSessionDetailDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.ChatSessionSummaryDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.CreateChatRequestDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.CreateChatResponseDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.SendChatMessageRequestDTO;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.SendChatMessageResponseDTO;
import com.marcoromanofinaa.jazzlogs.chat.application.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<CreateChatResponseDTO> createChat(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateChatRequestDTO request
    ) {
        var response = chatService.createChat(authenticatedUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{chatSessionId}/exchanges")
    public ResponseEntity<SendChatMessageResponseDTO> sendMessage(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID chatSessionId,
            @Valid @RequestBody SendChatMessageRequestDTO request
    ) {
        return ResponseEntity.ok(chatService.sendMessage(authenticatedUser.id(), chatSessionId, request));
    }

    @GetMapping
    public ResponseEntity<List<ChatSessionSummaryDTO>> getMyChats(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(chatService.getUserChats(authenticatedUser.id()));
    }

    @GetMapping("/{chatSessionId}")
    public ResponseEntity<ChatSessionDetailDTO> getChat(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID chatSessionId
    ) {
        return ResponseEntity.ok(chatService.getChat(authenticatedUser.id(), chatSessionId));
    }

    @DeleteMapping("/{chatSessionId}")
    public ResponseEntity<Void> deleteChat(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID chatSessionId
    ) {
        chatService.deleteChat(authenticatedUser.id(), chatSessionId);
        return ResponseEntity.noContent().build();
    }
}
