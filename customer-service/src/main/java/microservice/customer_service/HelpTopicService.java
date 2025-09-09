package microservice.customer_service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HelpTopicService {
    
    @Autowired
    private HelpTopicRepository helpTopicRepository;
    
    public List<HelpTopic> getAllHelpTopics() {
        return helpTopicRepository.findAllByOrderByTitleAsc();
    }
    
    public Optional<HelpTopic> getHelpTopicById(Long id) {
        return helpTopicRepository.findById(id);
    }
    
    public Page<HelpTopic> searchHelpTopics(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        if (query == null || query.trim().isEmpty()) {
            return helpTopicRepository.findAll(pageable);
        }
        return helpTopicRepository.findByTitleOrDescriptionContainingIgnoreCase(query, pageable);
    }
    
    public List<HelpTopic> getSuggestions(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return helpTopicRepository.findTop5ByTitleContainingIgnoreCaseOrderByTitleAsc(query);
    }
    
    public HelpTopic createHelpTopic(HelpTopic helpTopic) {
        return helpTopicRepository.save(helpTopic);
    }
    
    public HelpTopic updateHelpTopic(Long id, HelpTopic helpTopicDetails) {
        return helpTopicRepository.findById(id)
                .map(helpTopic -> {
                    helpTopic.setTitle(helpTopicDetails.getTitle());
                    helpTopic.setDescription(helpTopicDetails.getDescription());
                    helpTopic.setUrl(helpTopicDetails.getUrl());
                    return helpTopicRepository.save(helpTopic);
                })
                .orElse(null);
    }
    
    public boolean deleteHelpTopic(Long id) {
        return helpTopicRepository.findById(id)
                .map(helpTopic -> {
                    helpTopicRepository.delete(helpTopic);
                    return true;
                })
                .orElse(false);
    }
}
