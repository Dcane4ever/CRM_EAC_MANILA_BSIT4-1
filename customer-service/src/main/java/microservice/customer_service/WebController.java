package microservice.customer_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {
    
    @Autowired
    private HelpTopicService helpTopicService;
    
 
    
  @GetMapping({"/", "/index", "/home"})
    public String landingPage(Model model) {
        model.addAttribute("topics", helpTopicService.getAllHelpTopics());
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