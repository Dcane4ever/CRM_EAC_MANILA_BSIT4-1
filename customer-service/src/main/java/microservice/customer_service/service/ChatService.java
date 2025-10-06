package microservice.customer_service.service;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.ChatMessage;
import microservice.customer_service.model.ChatSession;
import microservice.customer_service.model.User;
import microservice.customer_service.repository.ChatMessageRepository;
import microservice.customer_service.repository.ChatSessionRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Queue for customers waiting for an agent
    private final Queue<ChatSession> customerQueue = new ConcurrentLinkedQueue<>();
    
    public ChatSession createChatSession(User customer) {
        System.out.println("Creating chat session for customer: " + customer.getUsername());
        ChatSession chatSession = new ChatSession();
        chatSession.setCustomer(customer);
        chatSession.setStatus(ChatSession.ChatStatus.WAITING);
        chatSession.setCreatedAt(LocalDateTime.now());
        ChatSession savedSession = chatSessionRepository.save(chatSession);
        System.out.println("Saved chat session with ID: " + savedSession.getId());
        
        // Add to waiting queue
        customerQueue.add(savedSession);
        System.out.println("Added to queue, current queue size: " + customerQueue.size());
        
        // Notify all agents about new customer in queue
        Map<String, Object> queueUpdate = new HashMap<>();
        queueUpdate.put("type", "NEW_CUSTOMER");
        queueUpdate.put("sessionId", savedSession.getId());
        queueUpdate.put("customerName", customer.getUsername());
        queueUpdate.put("queueSize", customerQueue.size());
        messagingTemplate.convertAndSend("/topic/queue-updates", queueUpdate);
        
        // Don't auto-assign - wait for agent to manually accept
        // assignAgentIfAvailable(savedSession);
        
        return savedSession;
    }
    
    public ChatSession createAnonymousChatSession(String nickname) {
        // Check if guest user already exists
        User guestUser = userService.findByUsername(nickname).orElse(null);
        
        if (guestUser == null) {
            // Create a new temporary user for the guest
            guestUser = new User();
            guestUser.setUsername(nickname);
            guestUser.setEmail("guest@example.com");  // Placeholder email
            guestUser.setPassword("guest");           // Not used for authentication
            guestUser.setRole(User.Role.CUSTOMER);
            guestUser.setGuest(true);
            
            // Save the temporary user
            guestUser = userService.saveTemporaryUser(guestUser);
        }
        
        // Create chat session with the guest user
        return createChatSession(guestUser);
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
        System.out.println("Attempting to assign agent to session: " + session.getId());
        if (session.getStatus() != ChatSession.ChatStatus.WAITING) {
            System.out.println("Session status is not WAITING: " + session.getStatus());
            return session;
        }
        
        List<User> availableAgents = userService.findAvailableAgents();
        System.out.println("Found " + availableAgents.size() + " available agents");
        if (availableAgents.isEmpty()) {
            System.out.println("No agents available, keeping in queue");
            return session; // No agents available, keep in queue
        }
        
        // Assign the first available agent
        User agent = availableAgents.get(0);
        System.out.println("Assigning agent " + agent.getUsername() + " to customer " + session.getCustomer().getUsername());
        session.setAgent(agent);
        session.setStatus(ChatSession.ChatStatus.ACTIVE);
        
        // Mark agent as unavailable
        userService.updateAgentAvailability(agent.getId(), false);
        
        // Remove from queue if assigned
        customerQueue.remove(session);
        System.out.println("Removed from queue, new queue size: " + customerQueue.size());
        
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
        System.out.println("Assigning agent " + agent.getUsername() + " to session " + sessionId);
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Chat session not found"));
        
        System.out.println("Session status before assignment: " + session.getStatus());
        System.out.println("Session current agent: " + (session.getAgent() != null ? session.getAgent().getUsername() : "none"));
        
        // Check if session is already assigned to this agent
        if (session.getStatus() == ChatSession.ChatStatus.ACTIVE && session.getAgent() != null) {
            if (session.getAgent().getId().equals(agent.getId())) {
                System.out.println("Session " + sessionId + " is already assigned to agent " + agent.getUsername());
                return session; // Already assigned to this agent, return as-is
            } else {
                throw new RuntimeException("Chat session is already assigned to another agent");
            }
        }
        
        if (session.getStatus() != ChatSession.ChatStatus.WAITING) {
            throw new RuntimeException("Chat session is not in waiting status. Current status: " + session.getStatus());
        }
        
        System.out.println("Proceeding with assignment...");
        
        // Assign the agent
        session.setAgent(agent);
        session.setStatus(ChatSession.ChatStatus.ACTIVE);
        
        // Mark agent as unavailable
        userService.updateAgentAvailability(agent.getId(), false);
        
        // Remove from queue if assigned
        boolean removedFromQueue = customerQueue.remove(session);
        System.out.println("Removed from queue: " + removedFromQueue + ", new queue size: " + customerQueue.size());
        
        // Add system message about agent joining
        ChatMessage systemMessage = new ChatMessage();
        systemMessage.setChatSession(session);
        systemMessage.setSender(agent);
        systemMessage.setContent("Agent " + agent.getUsername() + " has joined the chat.");
        systemMessage.setType(ChatMessage.MessageType.SYSTEM);
        chatMessageRepository.save(systemMessage);
        
        ChatSession savedSession = chatSessionRepository.save(session);
        System.out.println("Session " + sessionId + " successfully assigned to agent " + agent.getUsername() + " with status: " + savedSession.getStatus());
        
        return savedSession;
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
    
    public Optional<ChatSession> getSessionById(Long sessionId) {
        return chatSessionRepository.findById(sessionId);
    }
}
