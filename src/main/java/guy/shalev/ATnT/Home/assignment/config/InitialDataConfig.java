package guy.shalev.ATnT.Home.assignment.config;

import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class InitialDataConfig implements ApplicationRunner {

    private final UserService userService;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void run(ApplicationArguments args) {
        try {
            UserRequest adminUser = UserRequest.builder()
                    .username(adminUsername)
                    .password(adminPassword)
                    .email(adminEmail)
                    .role(UserRole.ADMIN)
                    .build();

            userService.registerUser(adminUser);
        } catch (ConflictException e) {
            // User already exists, ignore
        }
    }
}
