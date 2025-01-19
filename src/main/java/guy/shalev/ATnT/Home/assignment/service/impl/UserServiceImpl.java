package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.NotFoundException;
import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.UserResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.User;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.UserRepository;
import guy.shalev.ATnT.Home.assignment.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void registerUser(UserRequest request) {
        // Check if username already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ConflictException("Username already exists");
        }

        // Save to Spring Security users table
        jdbcTemplate.update(
                "INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)",
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                true
        );

        // Save to Spring Security authorities table
        jdbcTemplate.update(
                "INSERT INTO authorities (username, authority) VALUES (?, ?)",
                request.getUsername(),
                request.getRole() == UserRole.ADMIN ? "ROLE_ADMIN" : "ROLE_CUSTOMER"
        );

        // Save to our application users table
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .role(request.getRole())
                .build();

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    @Override
    public UserResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}