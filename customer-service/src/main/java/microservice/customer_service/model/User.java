package microservice.customer_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private String email;
    
    @Enumerated(EnumType.STRING)
    private Role role;
    
    @Column(nullable = false)
    private boolean available = true;
    
    @Column(nullable = false)
    private boolean guest = false;
    
    @Column(nullable = false)
    private boolean emailVerified = false;
    
    @Column
    private String verificationToken;
    
    @Column
    private java.time.LocalDateTime tokenExpiryDate;
    
    public enum Role {
        CUSTOMER,
        AGENT,
        ADMIN
    }
}
