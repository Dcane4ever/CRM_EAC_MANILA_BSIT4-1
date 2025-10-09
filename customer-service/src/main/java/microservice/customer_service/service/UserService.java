package microservice.customer_service.service;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.User;
import microservice.customer_service.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        // Check if username or email already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Don't override emailVerified - let AuthController set it
        // user.setEmailVerified(false); // New users need email verification
        return userRepository.save(user);
    }
    
    public User saveTemporaryUser(User user) {
        // For guest users, we don't need to encrypt the password as it's not used for login
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public List<User> findAvailableAgents() {
        List<User> agents = userRepository.findByRoleAndAvailable(User.Role.AGENT, true);
        System.out.println("UserService.findAvailableAgents() found: " + agents.size() + " agents");
        for (User agent : agents) {
            System.out.println("Available agent: " + agent.getUsername() + " (available: " + agent.isAvailable() + ")");
        }
        return agents;
    }

    public User updateAgentAvailability(Long agentId, boolean available) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        agent.setAvailable(available);
        return userRepository.save(agent);
    }
    
    public User save(User user) {
        return userRepository.save(user);
    }
}
