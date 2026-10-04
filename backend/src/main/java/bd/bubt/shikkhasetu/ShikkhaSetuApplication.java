package bd.bubt.shikkhasetu;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
// The repository interfaces are nested inside Repositories.java.
@EnableJpaRepositories(considerNestedRepositories = true)
public class ShikkhaSetuApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShikkhaSetuApplication.class, args);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** The clock is a bean so that date rules (due dates, overdue) can be tested. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
