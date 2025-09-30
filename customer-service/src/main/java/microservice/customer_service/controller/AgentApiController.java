package microservice.customer_service.controller;

import lombok.RequiredArgsConstructor;
import microservice.customer_service.model.User;
import microservice.customer_service.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentApiController {
    
    private final UserService userService;
    
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> updateAgentStatus(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            Optional<User> agentOpt = userService.findByUsername(username);
            
            if (agentOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Agent not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User agent = agentOpt.get();
            Boolean available = (Boolean) request.get("available");
            String auxStatus = (String) request.get("auxStatus");
            
            // Update agent availability
            if (available != null) {
                agent.setAvailable(available);
                userService.updateAgentAvailability(agent.getId(), available);
            }
            
            response.put("success", true);
            response.put("message", "Status updated successfully");
            response.put("currentStatus", auxStatus);
            response.put("available", agent.isAvailable());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error updating status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAgentStatus(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = authentication.getName();
            Optional<User> agentOpt = userService.findByUsername(username);
            
            if (agentOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Agent not found");
                return ResponseEntity.badRequest().body(response);
            }
            
            User agent = agentOpt.get();
            response.put("success", true);
            response.put("username", agent.getUsername());
            response.put("available", agent.isAvailable());
            response.put("email", agent.getEmail());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error getting status: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/queue")
    public ResponseEntity<Map<String, Object>> getQueueInfo() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // TODO: Implement real queue management
            // For now, return mock data
            response.put("success", true);
            response.put("queueLength", 0);
            response.put("waitTime", "< 2 minutes");
            response.put("availableAgents", userService.findAvailableAgents().size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error getting queue info: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
