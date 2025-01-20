package guy.shalev.ATnT.Home.assignment.repository;

import guy.shalev.ATnT.Home.assignment.model.entities.User;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Transactional
    default void insertSecurityDetails(String username, String encodedPassword, UserRole role, JdbcTemplate jdbcTemplate) {
        // Insert into Spring Security users table
        jdbcTemplate.update(
                "INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)",
                username,
                encodedPassword,
                true
        );

        // Insert into Spring Security authorities table
        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                username,
                role == UserRole.ADMIN ? "ROLE_ADMIN" : "ROLE_CUSTOMER"
        );
    }
}
