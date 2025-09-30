package microservice.customer_service.repository;

import microservice.customer_service.model.ChatSession;
import microservice.customer_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByStatus(ChatSession.ChatStatus status);
    List<ChatSession> findByCustomerAndStatus(User customer, ChatSession.ChatStatus status);
    List<ChatSession> findByAgentAndStatus(User agent, ChatSession.ChatStatus status);
}
