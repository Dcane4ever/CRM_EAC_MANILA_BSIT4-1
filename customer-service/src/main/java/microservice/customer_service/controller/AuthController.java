package microservice.customer_service.controller;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.User;
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
    public String registerCustomer(User user) {
        user.setRole(User.Role.CUSTOMER);
        userService.registerUser(user);
        return "redirect:/login?registered";
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
            userService.registerUser(user);
            redirectAttributes.addFlashAttribute("success", "Agent registration successful! You can now login.");
            return "redirect:/register-agent";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register-agent";
        }
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        // Get user role and redirect to appropriate dashboard
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        
        if (role.equals("ROLE_AGENT")) {
            return "redirect:/agent/dashboard";
        } else if (role.equals("ROLE_CUSTOMER")) {
            return "redirect:/customer/dashboard";
        } else if (role.equals("ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        
        return "dashboard"; // fallback
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
    public String agentChatPage(Model model) {
        return "agent-chat";
    }
}
