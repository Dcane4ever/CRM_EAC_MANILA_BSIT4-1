package microservice.customer_service.service;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.User;
import microservice.customer_service.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    
    private final UserRepository userRepository;
    private final EmailService emailService;
    
    public void sendVerificationEmail(User user) {
        // Generate verification token
        String verificationToken = UUID.randomUUID().toString();
        
        // Set token and expiry (24 hours from now)
        user.setVerificationToken(verificationToken);
        user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));
        
        // Save user with token
        userRepository.save(user);
        
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationToken);
        
        System.out.println("Verification email sent for user: " + user.getUsername());
    }
    
    public boolean verifyEmail(String token) {
        Optional<User> userOptional = userRepository.findByVerificationToken(token);
        
        if (userOptional.isEmpty()) {
            System.out.println("Invalid verification token: " + token);
            return false;
        }
        
        User user = userOptional.get();
        
        // Check if token is expired
        if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
            System.out.println("Verification token expired for user: " + user.getUsername());
            return false;
        }
        
        // Mark email as verified
        user.setEmailVerified(true);
        user.setVerificationToken(null); // Clear the token
        user.setTokenExpiryDate(null);   // Clear expiry date
        
        userRepository.save(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        
        System.out.println("Email verified successfully for user: " + user.getUsername());
        return true;
    }
    
    public boolean isEmailVerified(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.map(User::isEmailVerified).orElse(false);
    }
    
    public void resendVerificationEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("User not found with email: " + email);
        }
        
        User user = userOptional.get();
        
        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }
        
        // Send new verification email
        sendVerificationEmail(user);
    }
}
