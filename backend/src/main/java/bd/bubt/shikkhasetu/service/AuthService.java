package bd.bubt.shikkhasetu.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import bd.bubt.shikkhasetu.model.AuthToken;
import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.repo.Repositories.AuthTokenRepository;
import bd.bubt.shikkhasetu.repo.Repositories.UserRepository;
import bd.bubt.shikkhasetu.web.ApiException;

@Service
public class AuthService {

    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AuthService(UserRepository users, AuthTokenRepository tokens, PasswordEncoder passwordEncoder,
            Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Self-registration always creates a STUDENT. Coordinators are created by the seeder. */
    @Transactional
    public User registerStudent(String name, String email, String password) {
        return createUser(name, email, password, Role.STUDENT);
    }

    @Transactional
    public User createUser(String name, String email, String password, Role role) {
        String cleanEmail = email.trim().toLowerCase();
        if (users.findByEmailIgnoreCase(cleanEmail).isPresent()) {
            throw ApiException.conflict("An account with this e-mail already exists");
        }
        // Only the BCrypt hash is stored, never the password itself.
        return users.save(new User(name.trim(), cleanEmail, passwordEncoder.encode(password), role));
    }

    /** Returns a new session token if the e-mail and password are correct. */
    @Transactional
    public AuthToken login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("Wrong e-mail or password");
        }
        String token = UUID.randomUUID().toString() + UUID.randomUUID();
        return tokens.save(new AuthToken(token, user, LocalDateTime.now(clock)));
    }

    @Transactional
    public void logout(String token) {
        tokens.deleteById(token);
    }

    /** Finds the user that belongs to a token, or fails with 401. */
    public User resolve(String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.unauthorized("Please log in");
        }
        return tokens.findById(token)
                .map(AuthToken::getUser)
                .orElseThrow(() -> ApiException.unauthorized("Session expired, please log in again"));
    }
}
