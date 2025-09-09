package microservice.customer_service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HelpTopicRepository extends JpaRepository<HelpTopic, Long> {
    // Search by title or description (case-insensitive)
    @Query("SELECT h FROM HelpTopic h WHERE " +
           "LOWER(h.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(h.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<HelpTopic> findByTitleOrDescriptionContainingIgnoreCase(
            @Param("query") String query, Pageable pageable);
    
    // Find by title containing (for suggestions)
    List<HelpTopic> findTop5ByTitleContainingIgnoreCaseOrderByTitleAsc(String title);
    
    // Find all ordered by title
    List<HelpTopic> findAllByOrderByTitleAsc();
}
