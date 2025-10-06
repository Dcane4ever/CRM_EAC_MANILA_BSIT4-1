# CRM Customer Service Chat System - AI Developer Guide

## Architecture Overview

This is a **real-time customer service chat system** built with Spring Boot 3.5.5, featuring WebSocket-based bidirectional communication between customers and agents.

### Core Components
- **ChatService**: Manages chat sessions, queuing, and agent assignment with `ConcurrentLinkedQueue`
- **ChatWebSocketController**: Handles WebSocket messaging (`/app/chat.*`, `/app/agent.*`)
- **ChatApiController**: REST endpoints for chat operations (`/api/chat/*`)
- **SimpMessagingTemplate**: Real-time notifications to specific users and broadcast topics

### Data Flow
1. Customer joins → Creates `ChatSession` with WAITING status → Added to in-memory queue
2. Agent becomes available → Auto-assignment OR manual accept via `/api/chat/accept/{sessionId}`
3. Real-time updates via WebSocket channels: `/user/{username}/queue/session` and `/topic/queue-updates`

## Critical WebSocket Patterns

### Guest User Handling
- **Guest username transformation**: `"guest-" + nickname.replaceAll("\\s", "_")` for WebSocket subscriptions
- **Dual subscription paths**: Registered users use `username`, guests use transformed ID
- **Important**: All WebSocket notifications must handle both user types in controllers

### Message Routing
```java
// Customer (handles both registered and guest)
String destination = user.isGuest() ? 
    "guest-" + user.getUsername().replaceAll("\\s", "_") : 
    user.getUsername();
messagingTemplate.convertAndSendToUser(destination, "/queue/session", data);

// Agent notifications
messagingTemplate.convertAndSend("/topic/queue-updates", queueUpdate);
```

### Session States & Notifications
- **WAITING**: Customer in queue, agent sees in queue list
- **ACTIVE**: Chat ongoing, both parties get `/queue/session` update
- **CLOSED**: Chat ended, cleanup and agent availability reset

## Database & Models

### Key Relationships
- `ChatSession` ↔ `User` (customer/agent), `ChatMessage`
- **Circular reference prevention**: `@JsonIgnore` on bidirectional JPA relationships
- **MySQL config**: `spring.jpa.hibernate.ddl-auto=update` for schema management

### Critical Model Annotations
```java
@OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
@JsonIgnore  // Prevents JSON serialization depth errors
private List<ChatMessage> messages;
```

## Development Workflows

### Running & Testing
```bash
# Start application (auto-creates MySQL DB)
mvn spring-boot:run

# Test dual-browser scenario:
# 1. Agent: http://localhost:8080/agent/chat
# 2. Customer: http://localhost:8080/customer/chat?guest=true
```

### Common Debugging Patterns
- **Check WebSocket subscriptions**: Guest users must use transformed names
- **Monitor queue state**: `System.out.println` logging throughout ChatService
- **Session status tracking**: WAITING → ACTIVE → CLOSED transitions
- **JSON circular references**: Look for nesting depth > 1000 errors

### Security Context
- **WebSocket endpoints**: `/chat-websocket/**` permits all for guest access
- **Role-based routing**: Agent (`/agent/**`), Customer (`/customer/**`), API (`/api/chat/**`)
- **Guest authentication**: Temporary users with `isGuest=true`, no password validation

## Frontend Integration

### Thymeleaf Templates
- **agent-chat.html**: Queue management, real-time chat interface
- **customer-chat.html**: Waiting room → active chat transitions
- **WebSocket libraries**: SockJS + STOMP.js CDN integration

### Key JavaScript Patterns
```javascript
// Guest user WebSocket subscription
const subscriptionId = isGuest ? 
    ('guest-' + username.replaceAll(' ', '_')) : 
    username;
stompClient.subscribe('/user/' + subscriptionId + '/queue/session', onSessionUpdate);
```

## Common Issues & Solutions

1. **Customer not receiving agent acceptance**: Check guest username transformation in ChatApiController
2. **Circular JSON errors**: Verify `@JsonIgnore` on JPA relationships  
3. **Queue not updating**: Ensure `/topic/queue-updates` broadcasts include queue size
4. **Auto-assignment bypassing manual accept**: Remove `assignAgentIfAvailable()` from `createChatSession()`

## Testing Strategy

- **Multi-browser testing**: Edge (agent) + Chrome/Brave (customer)
- **Guest flow**: Use `?guest=true` parameter for anonymous customers
- **WebSocket debugging**: Browser dev tools → Network → WS tab for message inspection
- **Database state**: Check `chat_sessions` table for status transitions
