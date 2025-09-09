package microservice.customer_service;

import jakarta.persistence.*;
import java.util.List;
import jakarta.persistence.ElementCollection;


@Entity
public class HelpTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String url;

    @ElementCollection
    private List<String> steps;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
}
