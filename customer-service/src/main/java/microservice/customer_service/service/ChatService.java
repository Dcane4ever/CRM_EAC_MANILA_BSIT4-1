package microservice.customer_service.service;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.ChatMessage;
import microservice.customer_service.model.ChatSession;
import microservice.customer_service.model.User;
import microservice.customer_service.repository.ChatMessageRepository;
import microservice.customer_service.repository.ChatSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;
    
    // Queue for customers waiting for an agent
    private final Queue<ChatSession> customerQueue = new ConcurrentLinkedQueue<>();
    
    public ChatSession createChatSession(User customer) {
        ChatSession chatSession = new ChatSession();
        chatSession.setCustomer(customer);
        chatSession.setStatus(ChatSession.ChatStatus.WAITING);
        chatSession.setCreatedAt(LocalDateTime.now());
        ChatSession savedSession = chatSessionRepository.save(chatSession);
        
        // Add to waiting queue
        customerQueue.add(savedSession);
        
        // Try to assign an agent
        assignAgentIfAvailable(savedSession);
        
        return savedSession;
    }
    
    public ChatSession createAnonymousChatSession(String nickname) {
        // Create a temporary user for the guest
        User guestUser = new User();
        guestUser.setUsername(nickname);
        guestUser.setEmail("guest@example.com");  // Placeholder email
        guestUser.setPassword("guest");           // Not used for authentication
        guestUser.setRole(User.Role.CUSTOMER);
        guestUser.setGuest(true);
        
        // Save the temporary user
        User savedGuestUser = userService.saveTemporaryUser(guestUser);
        
        // Create chat session with the guest user
        return createChatSession(savedGuestUser);
    }
    
    @Transactional
    public ChatMessage addMessage(Long sessionId, User sender, String content) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
        
        ChatMessage message = new ChatMessage();
        message.setChatSession(session);
        message.setSender(sender);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        
        return chatMessageRepository.save(message);
    }
    
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
        return chatMessageRepository.findByChatSessionOrderByTimestampAsc(session);
    }
    
    @Transactional
    public ChatSession assignAgentIfAvailable(ChatSession session) {
        if (session.getStatus() != ChatSession.ChatStatus.WAITING) {
            return session;
        }
        
        List<User> availableAgents = userService.findAvailableAgents();
        if (availableAgents.isEmpty()) {
            return session; // No agents available, keep in queue
        }
        
        // Assign the first available agent
        User agent = availableAgents.get(0);
        session.setAgent(agent);
        session.setStatus(ChatSession.ChatStatus.ACTIVE);
        
        // Mark agent as unavailable
        userService.updateAgentAvailability(agent.getId(), false);
        
        // Remove from queue if assigned
        customerQueue.remove(session);
        
        // Add system message about agent joining
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setChatSession(session);
        systemMessage.setSender(agent);
        systemMessage.setContent("Agent " + agent.getUsername() + " has joined the chat.");
        systemMessage.setType(ChatMessage.MessageType.SYSTEM);
        chatMessageRepository.save(systemMessage);
        
        return chatSessionRepository.save(session);
    }
    
    @Transactional
    public ChatSession assignAgentToSession(Long sessionId, User agent) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
        
        if (session.getStatus() != ChatSession.ChatStatus.WAITING) {
            throw new RuntimeException("Chat session is not in waiting status");
        }
        
        // Assign the agent
        session.setAgent(agent);
        session.setStatus(ChatSession.ChatStatus.ACTIVE);
        
        // Mark agent as unavailable
        userService.updateAgentAvailability(agent.getId(), false);
        
        // Remove from queue if assigned
        customerQueue.remove(session);
        
        // Add system message about agent joining
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setChatSession(session);
        systemMessage.setSender(agent);
        systemMessage.setContent("Agent " + agent.getUsername() + " has joined the chat.");
        systemMessage.setType(ChatMessage.MessageType.SYSTEM);
        chatMessageRepository.save(systemMessage);
        
        return chatSessionRepository.save(session);
    }
    
    @Transactional
    public ChatSession endChatSession(Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
        
        session.setStatus(ChatSession.ChatStatus.CLOSED);
        session.setEndedAt(LocalDateTime.now());
        
        // Make agent available again if there was one
        if (session.getAgent() != null) {
            userService.updateAgentAvailability(session.getAgent().getId(), true);
            
            // Process next customer in queue if any
            if (!customerQueue.isEmpty()) {
                ChatSession nextSession = customerQueue.peek();
                assignAgentIfAvailable(nextSession);
            }
        }
        
        return chatSessionRepository.save(session);
    }
    
    public List<ChatSession> getWaitingCustomers() {
        return chatSessionRepository.findByStatus(ChatSession.ChatStatus.WAITING);
    }
    
    public List<ChatSession> getActiveSessionsForAgent(User agent) {
        return chatSessionRepository.findByAgentAndStatus(agent, ChatSession.ChatStatus.ACTIVE);
    }
    
    public Optional<ChatSession> getActiveSessionForCustomer(User customer) {
        List<ChatSession> sessions = chatSessionRepository.findByCustomerAndStatus(customer, ChatSession.ChatStatus.ACTIVE);
        return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0));
    }
    
    public int getQueuePosition(Long sessionId) {
        int position = 0;
        for (ChatSession session : customerQueue) {
            position++;
            if (session.getId().equals(sessionId)) {
                return position;
            }
        }
        return -1; // Not in queue
    }
}
