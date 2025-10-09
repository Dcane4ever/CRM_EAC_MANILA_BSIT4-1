package microservice.customer_service.controller;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.User;
import microservice.customer_service.service.ChatService;
import microservice.customer_service.service.EmailVerificationService;
import microservice.customer_service.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    private final ChatService chatService;
    private final EmailVerificationService emailVerificationService;
    private static final String AGENT_REGISTRATION_CODE = "AGENT123"; // This would be stored securely in a real app
    
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    
    @GetMapping("/register")
    public String registerCustomerPage(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }
    
    @GetMapping("/register-agent")
    public String registerAgentPage(Model model) {
        model.addAttribute("user", new User());
        return "register-agent";
    }
    
    @PostMapping("/register")
    public String registerCustomer(User user, RedirectAttributes redirectAttributes) {
        try {
            user.setRole(User.Role.CUSTOMER);
            User savedUser = userService.registerUser(user);
            
            // Send verification email
            emailVerificationService.sendVerificationEmail(savedUser);
            
            redirectAttributes.addFlashAttribute("message", 
                "Registration successful! Please check your email to verify your account before logging in.");
            return "redirect:/login?verification-sent";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }
    
    @PostMapping("/register-agent")
    public String registerAgent(User user, @RequestParam("agentCode") String agentCode, 
                                RedirectAttributes redirectAttributes) {
        // Verify agent registration code
        if (!AGENT_REGISTRATION_CODE.equals(agentCode)) {
            redirectAttributes.addFlashAttribute("error", "Invalid agent registration code");
            return "redirect:/register-agent";
        }
        
        try {
            user.setRole(User.Role.AGENT);
            User savedUser = userService.registerUser(user);
            
            // Send verification email
            emailVerificationService.sendVerificationEmail(savedUser);
            
            redirectAttributes.addFlashAttribute("success", 
                "Agent registration successful! Please check your email to verify your account before logging in.");
            return "redirect:/register-agent";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register-agent";
        }
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        // Get user role and redirect to appropriate page
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        
        if (role.equals("ROLE_AGENT")) {
            return "redirect:/agent/dashboard";
        } else if (role.equals("ROLE_CUSTOMER")) {
            // Redirect customers to landing page (they'll see logged-in state)
            return "redirect:/";
        } else if (role.equals("ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        
        return "redirect:/"; // fallback to landing page
    }
    
    @GetMapping("/agent/dashboard")
    public String agentDashboard(Model model, Authentication authentication) {
        // Get current agent info
        String username = authentication.getName();
        Optional<User> currentAgent = userService.findByUsername(username);
        
        if (currentAgent.isPresent()) {
            model.addAttribute("agent", currentAgent.get());
            model.addAttribute("agentStatus", currentAgent.get().isAvailable() ? "Ready" : "Unavailable");
        }
        
        return "agent-dashboard";
    }
    
    @GetMapping("/customer/dashboard")
    public String customerDashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        Optional<User> currentCustomer = userService.findByUsername(username);
        
        if (currentCustomer.isPresent()) {
            model.addAttribute("customer", currentCustomer.get());
        }
        
        return "customer-dashboard";
    }
    
    @GetMapping("/customer/chat")
    public String customerChatPage() {
        return "customer-chat";
    }
    
    @GetMapping("/customer/chat-anonymous")
    public String anonymousChatPage() {
        return "chat-anonymous";
    }
    
    @GetMapping("/agent/chat")
    public String agentChatPage(Model model, Authentication authentication) {
        // Add waiting customers to the model so they show up in the agent interface
        model.addAttribute("waitingCustomers", chatService.getWaitingCustomers());
        
        // Add authenticated user info to the model
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("username", authentication.getName());
        } else {
            model.addAttribute("username", "agent");
        }
        
        return "agent-chat";
    }
    
    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam("token") String token, Model model) {
        try {
            boolean verified = emailVerificationService.verifyEmail(token);
            
            if (verified) {
                model.addAttribute("message", "Email verified successfully! You can now login to your account.");
                model.addAttribute("messageType", "success");
            } else {
                model.addAttribute("message", "Invalid or expired verification link. Please register again or request a new verification email.");
                model.addAttribute("messageType", "error");
            }
        } catch (Exception e) {
            model.addAttribute("message", "An error occurred during email verification. Please try again.");
            model.addAttribute("messageType", "error");
        }
        
        return "email-verification-result";
    }
    
    @GetMapping("/resend-verification")
    public String resendVerificationPage() {
        return "resend-verification";
    }
    
    @PostMapping("/resend-verification")
    public String resendVerification(@RequestParam("email") String email, 
                                   RedirectAttributes redirectAttributes) {
        try {
            emailVerificationService.resendVerificationEmail(email);
            redirectAttributes.addFlashAttribute("message", 
                "Verification email sent! Please check your inbox.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        
        return "redirect:/resend-verification";
    }
}
