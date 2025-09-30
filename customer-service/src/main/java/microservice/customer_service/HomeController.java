package microservice.customer_service;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.beans.factory.annotation.Autowired;


@Controller
public class HomeController {
    @Autowired

// Root URL mapping removed to avoid conflict with WebController
    // Optional: http://localhost:8080/LandingPage
    @GetMapping("/LandingPage")
    public String landingPage() {
        return "LandingPage";
    }

};



