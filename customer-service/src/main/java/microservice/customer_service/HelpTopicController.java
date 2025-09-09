package microservice.customer_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/help-topics")
@CrossOrigin(origins = "*") // Configure this properly for production
public class HelpTopicController {
    
    @Autowired
    private HelpTopicService helpTopicService;
    
    @GetMapping
    public ResponseEntity<List<HelpTopic>> getAllHelpTopics() {
        List<HelpTopic> helpTopics = helpTopicService.getAllHelpTopics();
        return ResponseEntity.ok(helpTopics);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<HelpTopic> getHelpTopicById(@PathVariable Long id) {
        return helpTopicService.getHelpTopicById(id)
                .map(helpTopic -> ResponseEntity.ok(helpTopic))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/search")
    public ResponseEntity<Page<HelpTopic>> searchHelpTopics(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<HelpTopic> results = helpTopicService.searchHelpTopics(q, page, size);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/suggestions")
    public ResponseEntity<List<HelpTopic>> getSuggestions(
            @RequestParam String q) {
        
        List<HelpTopic> suggestions = helpTopicService.getSuggestions(q);
        return ResponseEntity.ok(suggestions);
    }
    
    @PostMapping
    public ResponseEntity<HelpTopic> createHelpTopic(@RequestBody HelpTopic helpTopic) {
        HelpTopic created = helpTopicService.createHelpTopic(helpTopic);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<HelpTopic> updateHelpTopic(
            @PathVariable Long id, 
            @RequestBody HelpTopic helpTopicDetails) {
        
        HelpTopic updated = helpTopicService.updateHelpTopic(id, helpTopicDetails);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        }
        return ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHelpTopic(@PathVariable Long id) {
        if (helpTopicService.deleteHelpTopic(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}

