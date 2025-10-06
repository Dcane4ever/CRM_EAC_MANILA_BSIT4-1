package microservice.customer_service.controller;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.ChatSession;
import microservice.customer_service.model.User;
import microservice.customer_service.service.ChatService;
import microservice.customer_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;
    
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
        
        // Send WebSocket notification to agent about the new active session
        Map<String, Object> agentSessionUpdate = new HashMap<>();
        agentSessionUpdate.put("sessionId", session.getId());
        agentSessionUpdate.put("status", session.getStatus().toString());
        agentSessionUpdate.put("customerName", session.getCustomer().getUsername());
        
        messagingTemplate.convertAndSendToUser(
            agent.getUsername(),
            "/queue/session",
            agentSessionUpdate
        );
        
        // Send WebSocket notification to customer that agent has joined
        Map<String, Object> customerSessionUpdate = new HashMap<>();
        customerSessionUpdate.put("sessionId", session.getId());
        customerSessionUpdate.put("status", session.getStatus().toString());
        customerSessionUpdate.put("agentName", agent.getUsername());
        
        System.out.println("=== CUSTOMER NOTIFICATION DEBUG ===");
        System.out.println("Customer username: " + session.getCustomer().getUsername());
        System.out.println("Customer isGuest: " + session.getCustomer().isGuest());
        System.out.println("Session ID: " + session.getId());
        
        // Handle both guest and registered users differently
        if (session.getCustomer().isGuest()) {
            // For guest users, send to guest-specific topic
            String guestTopic = "/topic/guest/guest-" + session.getCustomer().getUsername().replaceAll("\\s", "_");
            System.out.println("Sending to guest topic: " + guestTopic);
            messagingTemplate.convertAndSend(
                guestTopic,
                customerSessionUpdate
            );
        } else {
            // For registered users, use user-specific destination
            System.out.println("Sending to user: " + session.getCustomer().getUsername());
            messagingTemplate.convertAndSendToUser(
                session.getCustomer().getUsername(),
                "/queue/session",
                customerSessionUpdate
            );
        }
        System.out.println("Customer session update: " + customerSessionUpdate);
        System.out.println("===================================");
        
        // Also broadcast queue update to all agents
        messagingTemplate.convertAndSend("/topic/queue-updates", Map.of(
            "type", "CHAT_ACCEPTED",
            "sessionId", session.getId(),
            "agentId", agent.getId()
        ));
        
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
    
    @GetMapping("/waiting-customers")
    public ResponseEntity<?> getWaitingCustomers() {
        return ResponseEntity.ok(chatService.getWaitingCustomers());
    }
}
