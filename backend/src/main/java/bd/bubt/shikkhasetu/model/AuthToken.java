package bd.bubt.shikkhasetu.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A login session. The random token is sent by the app in the Authorization header. */
@Entity
@Table(name = "auth_tokens")
public class AuthToken {

    @Id
    private String token;

    @ManyToOne(optional = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected AuthToken() {
    }

    public AuthToken(String token, User user, LocalDateTime createdAt) {
        this.token = token;
        this.user = user;
        this.createdAt = createdAt;
    }

    public String getToken() { return token; }
    public User getUser() { return user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
