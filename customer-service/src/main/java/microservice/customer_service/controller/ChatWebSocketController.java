package microservice.customer_service.controller;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.ChatMessage;
import microservice.customer_service.model.ChatSession;
import microservice.customer_service.model.User;
import microservice.customer_service.service.ChatService;
import microservice.customer_service.service.UserService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final UserService userService;
    
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Map<String, String> payload, Principal principal) {
        Long sessionId = Long.parseLong(payload.get("sessionId"));
        String content = payload.get("content");
        
        userService.findByUsername(principal.getName()).ifPresent(user -> {
            ChatMessage message = chatService.addMessage(sessionId, user, content);
            
            // Send to specific users in the chat session
            ChatSession session = message.getChatSession();
            
            // Send to both customer and agent topics
            messagingTemplate.convertAndSendToUser(
                session.getCustomer().getUsername(),
                "/queue/messages",
                message
            );
            
            if (session.getAgent() != null) {
                messagingTemplate.convertAndSendToUser(
                    session.getAgent().getUsername(),
                    "/queue/messages",
                    message
                );
            }
        });
    }
    
    @MessageMapping("/chat.join")
    public void joinChat(@Payload Map<String, String> payload, Principal principal) {
        // Handle guest chat
        if (payload.containsKey("guestNickname")) {
            String nickname = payload.get("guestNickname");
            
            // Create anonymous chat session
            ChatSession session = chatService.createAnonymousChatSession(nickname);
            
            // Send session info to guest
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", session.getId());
            sessionInfo.put("status", session.getStatus());
            sessionInfo.put("queuePosition", chatService.getQueuePosition(session.getId()));
            
            // Using a custom principal name for the anonymous user
            String anonymousId = "guest-" + nickname.replaceAll("\\s", "_");
            
            messagingTemplate.convertAndSendToUser(
                anonymousId,
                "/queue/session",
                sessionInfo
            );
            
            // Notify available agents of a new customer in queue
            messagingTemplate.convertAndSend(
                "/topic/queue-updates",
                Map.of("action", "NEW_CUSTOMER", "queueSize", chatService.getWaitingCustomers().size())
            );
            return;
        }
        
        // Handle logged-in user chat
        userService.findByUsername(principal.getName()).ifPresent(user -> {
            if (user.getRole() == User.Role.CUSTOMER) {
                // Customer joining - create a new chat session
                ChatSession session = chatService.createChatSession(user);
                
                // Send session info to customer
                Map<String, Object> sessionInfo = new HashMap<>();
                sessionInfo.put("sessionId", session.getId());
                sessionInfo.put("status", session.getStatus());
                sessionInfo.put("queuePosition", chatService.getQueuePosition(session.getId()));
                
                messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/session",
                    sessionInfo
                );
                
                // Notify available agents of a new customer in queue
                messagingTemplate.convertAndSend(
                    "/topic/queue-updates",
                    Map.of("action", "NEW_CUSTOMER", "queueSize", chatService.getWaitingCustomers().size())
                );
            }
        });
    }
    
    @MessageMapping("/chat.leave")
    public void leaveChat(@Payload Map<String, String> payload, Principal principal) {
        Long sessionId = Long.parseLong(payload.get("sessionId"));
        
        userService.findByUsername(principal.getName()).ifPresent(user -> {
            ChatSession endedSession = chatService.endChatSession(sessionId);
            
            // Notify both parties that the chat has ended
            Map<String, Object> endInfo = new HashMap<>();
            endInfo.put("sessionId", endedSession.getId());
            endInfo.put("status", "CLOSED");
            
            messagingTemplate.convertAndSendToUser(
                endedSession.getCustomer().getUsername(),
                "/queue/session",
                endInfo
            );
            
            if (endedSession.getAgent() != null) {
                messagingTemplate.convertAndSendToUser(
                    endedSession.getAgent().getUsername(),
                    "/queue/session",
                    endInfo
                );
                
                // Notify agents of queue updates
                messagingTemplate.convertAndSend(
                    "/topic/queue-updates",
                    Map.of("action", "QUEUE_UPDATE", "queueSize", chatService.getWaitingCustomers().size())
                );
            }
        });
    }
    
    @MessageMapping("/agent.available")
    public void setAgentAvailability(@Payload Map<String, Object> payload, Principal principal) {
        boolean available = (boolean) payload.get("available");
        
        userService.findByUsername(principal.getName()).ifPresent(agent -> {
            if (agent.getRole() == User.Role.AGENT) {
                userService.updateAgentAvailability(agent.getId(), available);
                
                if (available) {
                    // Process waiting customers if agent becomes available
                    chatService.getWaitingCustomers().stream()
                        .findFirst()
                        .ifPresent(chatService::assignAgentIfAvailable);
                }
            }
        });
    }
}
