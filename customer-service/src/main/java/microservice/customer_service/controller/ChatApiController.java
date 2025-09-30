package microservice.customer_service.controller;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.ChatSession;
import microservice.customer_service.model.User;
import microservice.customer_service.service.ChatService;
import microservice.customer_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatApiController {
    
    private final ChatService chatService;
    private final UserService userService;
    
    @PostMapping("/accept/{sessionId}")
    public ResponseEntity<Map<String, Object>> acceptChat(
            @PathVariable Long sessionId,
            Authentication authentication) {
        
        User agent = userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        
        if (agent.getRole() != User.Role.AGENT) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only agents can accept chats"));
        }
        
        ChatSession session = chatService.assignAgentToSession(sessionId, agent);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", session.getId());
        response.put("status", session.getStatus());
        response.put("customerName", session.getCustomer().getUsername());
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/session/{sessionId}/messages")
    public ResponseEntity<?> getSessionMessages(@PathVariable Long sessionId) {
        return ResponseEntity.ok(chatService.getSessionMessages(sessionId));
    }
    
    @GetMapping("/queue-position/{sessionId}")
    public ResponseEntity<Map<String, Object>> getQueuePosition(@PathVariable Long sessionId) {
        int position = chatService.getQueuePosition(sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("position", position);
        response.put("estimatedWaitTime", position * 2); // Rough estimate in minutes
        
        return ResponseEntity.ok(response);
    }
}
