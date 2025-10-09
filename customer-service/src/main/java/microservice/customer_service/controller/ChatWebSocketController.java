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
        
        System.out.println("Received message for session " + sessionId + ": " + content);
        System.out.println("Principal: " + (principal != null ? principal.getName() : "null (guest user)"));
        
        // Handle both guest users (no principal) and registered users
        if (principal != null) {
            // Registered user (agent)
            userService.findByUsername(principal.getName()).ifPresent(user -> {
                System.out.println("Found registered user: " + user.getUsername());
                sendChatMessage(sessionId, content, user);
            });
        } else {
            // Guest user - find customer by session
            chatService.getSessionById(sessionId).ifPresent(session -> {
                User customer = session.getCustomer();
                if (customer != null && customer.isGuest()) {
                    System.out.println("Found guest customer: " + customer.getUsername());
                    sendChatMessage(sessionId, content, customer);
                }
            });
        }
    }
    
    private void sendChatMessage(Long sessionId, String content, User sender) {
        System.out.println("Sending message from " + sender.getUsername() + " (guest: " + sender.isGuest() + ")");
        ChatMessage message = chatService.addMessage(sessionId, sender, content);
        
        // Create a message DTO with session ID since chatSession is @JsonIgnore
        Map<String, Object> messageDto = new HashMap<>();
        messageDto.put("id", message.getId());
        messageDto.put("sessionId", sessionId);
        messageDto.put("sender", message.getSender());
        messageDto.put("content", message.getContent());
        messageDto.put("timestamp", message.getTimestamp());
        messageDto.put("type", message.getType());
        
        // Send to specific users in the chat session
        ChatSession session = message.getChatSession();
        
        // Send to customer - handle guest vs registered differently
        if (session.getCustomer().isGuest()) {
            // For guest users, send messages to session-specific topic
            System.out.println("Sending to guest customer via topic: /topic/session/" + sessionId + "/messages");
            messagingTemplate.convertAndSend(
                "/topic/session/" + sessionId + "/messages",
                messageDto
            );
        } else {
            // For registered users, send to user-specific destination
            System.out.println("Sending to registered customer: " + session.getCustomer().getUsername());
            messagingTemplate.convertAndSendToUser(
                session.getCustomer().getUsername(),
                "/queue/messages",
                messageDto
            );
        }
        
        // Always send to agent (registered user)
        if (session.getAgent() != null) {
            System.out.println("Sending to agent: " + session.getAgent().getUsername());
            messagingTemplate.convertAndSendToUser(
                session.getAgent().getUsername(),
                "/queue/messages",
                messageDto
            );
        }
    }
    
    @MessageMapping("/chat.join")
    public void joinChat(@Payload Map<String, String> payload, Principal principal) {
        // Handle guest chat
        if (payload.containsKey("guestNickname")) {
            String nickname = payload.get("guestNickname");
            System.out.println("Creating anonymous chat session for: " + nickname);
            
            // Create anonymous chat session
            ChatSession session = chatService.createAnonymousChatSession(nickname);
            System.out.println("Created session with ID: " + session.getId() + ", Status: " + session.getStatus());
            
            // Send session info to guest
            Map<String, Object> sessionInfo = new HashMap<>();
            sessionInfo.put("sessionId", session.getId());
            sessionInfo.put("status", session.getStatus());
            sessionInfo.put("queuePosition", chatService.getQueuePosition(session.getId()));
            
            // For guest users, send to guest-specific topic first
            String guestTopic = "/topic/guest/guest-" + nickname.replaceAll("\\s", "_");
            System.out.println("Sending session info to guest user via topic: " + guestTopic);
            
            messagingTemplate.convertAndSend(
                guestTopic,
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
                System.out.println("Creating chat session for logged-in customer: " + user.getUsername());
                // Customer joining - create a new chat session
                ChatSession session = chatService.createChatSession(user);
                System.out.println("Created session with ID: " + session.getId() + ", Status: " + session.getStatus());
                
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
                int queueSize = chatService.getWaitingCustomers().size();
                System.out.println("Sending queue update - NEW_CUSTOMER, queueSize: " + queueSize);
                messagingTemplate.convertAndSend(
                    "/topic/queue-updates",
                    Map.of("action", "NEW_CUSTOMER", "queueSize", queueSize)
                );
            }
        });
    }
    
    @MessageMapping("/chat.leave")
    public void leaveChat(@Payload Map<String, String> payload, Principal principal) {
        Long sessionId = Long.parseLong(payload.get("sessionId"));
        
        System.out.println("Chat leave request for session: " + sessionId);
        System.out.println("Principal: " + (principal != null ? principal.getName() : "null (guest user)"));
        
        // Handle both registered users and guest users
        if (principal != null) {
            // Registered user (agent)
            userService.findByUsername(principal.getName()).ifPresent(user -> {
                endChatSessionAndNotify(sessionId, user.getUsername() + " left the chat");
            });
        } else {
            // Guest user - find by session
            chatService.getSessionById(sessionId).ifPresent(session -> {
                if (session.getCustomer() != null && session.getCustomer().isGuest()) {
                    endChatSessionAndNotify(sessionId, session.getCustomer().getUsername() + " left the chat");
                }
            });
        }
    }
    
    private void endChatSessionAndNotify(Long sessionId, String leaveMessage) {
        ChatSession endedSession = chatService.endChatSession(sessionId);
        
        System.out.println("Chat session " + sessionId + " ended. " + leaveMessage);
        
        // Create end notification with thank you message
        Map<String, Object> endInfo = new HashMap<>();
        endInfo.put("sessionId", endedSession.getId());
        endInfo.put("status", "CLOSED");
        endInfo.put("message", "Thank you for using our chat service!");
        endInfo.put("redirectTo", "/");
        
        // Notify customer
        if (endedSession.getCustomer().isGuest()) {
            // Guest customer - send to guest topic
            String guestTopic = "/topic/guest/guest-" + endedSession.getCustomer().getUsername().replaceAll("\\s", "_");
            System.out.println("Sending chat end notification to guest: " + guestTopic);
            messagingTemplate.convertAndSend(guestTopic, endInfo);
        } else {
            // Registered customer
            System.out.println("Sending chat end notification to registered customer: " + endedSession.getCustomer().getUsername());
            messagingTemplate.convertAndSendToUser(
                endedSession.getCustomer().getUsername(),
                "/queue/session",
                endInfo
            );
        }
        
        // Notify agent
        if (endedSession.getAgent() != null) {
            Map<String, Object> agentEndInfo = new HashMap<>();
            agentEndInfo.put("sessionId", endedSession.getId());
            agentEndInfo.put("status", "CLOSED");
            agentEndInfo.put("message", "Chat session ended");
            agentEndInfo.put("action", "REMOVE_FROM_ACTIVE");
            
            System.out.println("Sending chat end notification to agent: " + endedSession.getAgent().getUsername());
            messagingTemplate.convertAndSendToUser(
                endedSession.getAgent().getUsername(),
                "/queue/session",
                agentEndInfo
            );
            
            // Notify all agents of queue updates (agent is now available)
            messagingTemplate.convertAndSend(
                "/topic/queue-updates",
                Map.of("action", "AGENT_AVAILABLE", "queueSize", chatService.getWaitingCustomers().size())
            );
        }
    }
    
    @MessageMapping("/agent.available")
    public void setAgentAvailability(@Payload Map<String, Object> payload, Principal principal) {
        boolean available = (boolean) payload.get("available");
        System.out.println("Setting agent availability - Agent: " + principal.getName() + ", Available: " + available);
        
        userService.findByUsername(principal.getName()).ifPresent(agent -> {
            if (agent.getRole() == User.Role.AGENT) {
                userService.updateAgentAvailability(agent.getId(), available);
                System.out.println("Updated agent " + agent.getUsername() + " availability to: " + available);
                
                if (available) {
                    // Agent became available - but don't auto-assign, wait for manual accept
                    int waitingCount = chatService.getWaitingCustomers().size();
                    System.out.println("Agent became available, " + waitingCount + " customers waiting for manual accept");
                    // Don't auto-assign - agents must manually accept customers
                }
            }
        });
    }
}
