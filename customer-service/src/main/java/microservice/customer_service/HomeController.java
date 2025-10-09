package microservice.customer_service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/LandingPage") 
    public String landingPage() {
        // Redirect to root to avoid duplicate mappings
        return "redirect:/";
    }
}



