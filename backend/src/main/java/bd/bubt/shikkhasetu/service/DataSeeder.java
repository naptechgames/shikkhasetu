package bd.bubt.shikkhasetu.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import bd.bubt.shikkhasetu.model.Enums.Category;
import bd.bubt.shikkhasetu.model.Enums.ItemCondition;
import bd.bubt.shikkhasetu.model.Enums.ItemMode;
import bd.bubt.shikkhasetu.model.Enums.Role;
import bd.bubt.shikkhasetu.model.User;
import bd.bubt.shikkhasetu.repo.Repositories.UserRepository;

/**
 * Creates the coordinator account on first start. With app.seed.demo-data=true
 * it also adds one demo student and a few SYNTHETIC listings for demonstration.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final AuthService authService;
    private final ItemService itemService;

    @Value("${app.seed.coordinator-email:}")
    private String coordinatorEmail;
    @Value("${app.seed.coordinator-password:}")
    private String coordinatorPassword;
    @Value("${app.seed.demo-data:false}")
    private boolean demoData;
    @Value("${app.seed.student-email:}")
    private String studentEmail;
    @Value("${app.seed.student-password:}")
    private String studentPassword;

    public DataSeeder(UserRepository users, AuthService authService, ItemService itemService) {
        this.users = users;
        this.authService = authService;
        this.itemService = itemService;
    }

    @Override
    public void run(String... args) {
        if (!users.findByRole(Role.COORDINATOR).isEmpty()) {
            return; // already seeded
        }
        if (coordinatorEmail.isBlank() || coordinatorPassword.isBlank()) {
            System.out.println("[SEED] No coordinator configured (app.seed.coordinator-email/-password). "
                    + "Nobody can approve requests until one is set. See README.");
            return;
        }
        User coordinator = authService.createUser("Club Coordinator", coordinatorEmail, coordinatorPassword,
                Role.COORDINATOR);
        if (!demoData || studentEmail.isBlank() || studentPassword.isBlank()) {
            return;
        }
        authService.createUser("Demo Student", studentEmail, studentPassword, Role.STUDENT);
        itemService.create(coordinator, "Casio fx-991ES Plus calculator", "Demo data. Scientific calculator.",
                Category.CALCULATOR, ItemMode.LOAN, ItemCondition.GOOD);
        itemService.create(coordinator, "Software Engineering (Pressman), 7th ed.", "Demo data. CSE 327 text.",
                Category.BOOK, ItemMode.LOAN, ItemCondition.FAIR);
        itemService.create(coordinator, "Discrete Mathematics (Rosen)", "Demo data. Donated by an alumnus.",
                Category.BOOK, ItemMode.DONATION, ItemCondition.GOOD);
        itemService.create(coordinator, "Geometry box", "Demo data. Compass, set squares, protractor.",
                Category.OTHER, ItemMode.DONATION, ItemCondition.NEW);
    }
}
