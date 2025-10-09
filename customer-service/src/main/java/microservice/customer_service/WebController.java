package microservice.customer_service;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.User;
import microservice.customer_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class WebController {
    
    @Autowired
    private HelpTopicService helpTopicService;
    
    private final UserService userService;
    
  @GetMapping({"/", "/index", "/home"})
    public String landingPage(Model model, Authentication authentication) {
        model.addAttribute("topics", helpTopicService.getAllHelpTopics());
        
        // Add authentication info to model for the landing page
        if (authentication != null && authentication.isAuthenticated() 
            && !authentication.getName().equals("anonymousUser")) {
            
            Optional<User> currentUser = userService.findByUsername(authentication.getName());
            if (currentUser.isPresent()) {
                model.addAttribute("isLoggedIn", true);
                model.addAttribute("currentUser", currentUser.get());
                model.addAttribute("username", currentUser.get().getUsername());
                model.addAttribute("userRole", currentUser.get().getRole().name());
            }
        } else {
            model.addAttribute("isLoggedIn", false);
        }
        
        return "LandingPage";
    }
    
   
    @GetMapping("/topic/{id}")
    public String viewTopic(@PathVariable Long id, Model model) {
        return helpTopicService.getHelpTopicById(id)
                .map(topic -> {
                    model.addAttribute("topic", topic);
                    return "TopicDetail";
                })
                .orElse("redirect:/");
    }
}