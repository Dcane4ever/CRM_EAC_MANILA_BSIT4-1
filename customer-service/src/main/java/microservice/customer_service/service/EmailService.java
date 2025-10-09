package microservice.customer_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    
    private final JavaMailSender mailSender;
    
    public void sendVerificationEmail(String toEmail, String username, String verificationToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("CRM Customer Service - Email Verification");
            message.setText(buildVerificationEmailContent(username, verificationToken));
            message.setFrom("carwashpimentel@gmail.com"); // Will be configured via environment variables
            
            System.out.println("📧 Sending verification email to: " + toEmail);
            mailSender.send(message);
            System.out.println("✅ Verification email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send verification email to: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }
    
    private String buildVerificationEmailContent(String username, String verificationToken) {
        String baseUrl = System.getenv("RENDER_EXTERNAL_URL") != null ? 
            System.getenv("RENDER_EXTERNAL_URL") : "http://localhost:8080";
        String verificationUrl = baseUrl + "/verify-email?token=" + verificationToken;
        
        return String.format(
            "Dear %s,\n\n" +
            "Thank you for registering with CRM Customer Service!\n\n" +
            "Please click the following link to verify your email address:\n" +
            "%s\n\n" +
            "This link will expire in 24 hours.\n\n" +
            "If you did not create an account, please ignore this email.\n\n" +
            "Best regards,\n" +
            "CRM Customer Service Team",
            username,
            verificationUrl
        );
    }
    
    public void sendWelcomeEmail(String toEmail, String username) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Welcome to CRM Customer Service!");
            message.setText(buildWelcomeEmailContent(username));
            message.setFrom("carwashpimentel@gmail.com"); // Will be configured via environment variables
            
            System.out.println("📧 Sending welcome email to: " + toEmail);
            mailSender.send(message);
            System.out.println("✅ Welcome email sent successfully to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Failed to send welcome email to: " + toEmail);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String buildWelcomeEmailContent(String username) {
        return String.format(
            "Dear %s,\n\n" +
            "Welcome to CRM Customer Service!\n\n" +
            "Your email has been successfully verified and your account is now active.\n" +
            "You can now access all our services including live chat support.\n\n" +
            "Visit our platform: http://localhost:8080\n\n" +
            "Thank you for choosing our service!\n\n" +
            "Best regards,\n" +
            "CRM Customer Service Team",
            username
        );
    }
}
