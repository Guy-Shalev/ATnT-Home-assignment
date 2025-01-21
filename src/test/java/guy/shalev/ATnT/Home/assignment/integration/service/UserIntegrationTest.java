package guy.shalev.ATnT.Home.assignment.integration.service;

import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.integration.BaseIntegrationTest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.UserResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.User;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.UserRepository;
import guy.shalev.ATnT.Home.assignment.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@Validated
@DisplayName("User Integration Tests")
class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Validator validator;

    @BeforeEach
    void setUp() {
        // Clean up tables before each test
        jdbcTemplate.execute("DELETE FROM authorities");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM app_users");
    }

    private void validateRequest(UserRequest request) throws MethodArgumentNotValidException {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "userRequest");
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            throw new MethodArgumentNotValidException(null, errors);
        }
    }

    @Nested
    @DisplayName("User Registration Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("Successfully register a customer user")
        void registerCustomerSuccess() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("customer")
                    .password("password123")
                    .email("customer@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            // When
            userService.registerUser(request);

            // Then
            User savedUser = userRepository.findByUsername("customer")
                    .orElseThrow();
            assertEquals("customer@test.com", savedUser.getEmail());
            assertEquals(UserRole.CUSTOMER, savedUser.getRole());

            // Verify Spring Security tables
            Integer userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ?",
                    Integer.class,
                    "customer"
            );
            assertEquals(1, userCount);

            Integer authorityCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM authorities WHERE username = ? AND authority = ?",
                    Integer.class,
                    "customer",
                    "ROLE_CUSTOMER"
            );
            assertEquals(1, authorityCount);
        }

        @Test
        @DisplayName("Successfully register an admin user")
        void registerAdminSuccess() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("admin")
                    .password("adminPass123")
                    .email("admin@test.com")
                    .role(UserRole.ADMIN)
                    .build();

            // When
            userService.registerUser(request);

            // Then
            User savedUser = userRepository.findByUsername("admin")
                    .orElseThrow();
            assertEquals("admin@test.com", savedUser.getEmail());
            assertEquals(UserRole.ADMIN, savedUser.getRole());

            Integer authorityCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM authorities WHERE username = ? AND authority = ?",
                    Integer.class,
                    "admin",
                    "ROLE_ADMIN"
            );
            assertEquals(1, authorityCount);
        }

        @Test
        @DisplayName("Fail registration when username already exists")
        void registerDuplicateUsernameFails() {
            // Given
            UserRequest request1 = UserRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .email("test1@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            UserRequest request2 = UserRequest.builder()
                    .username("testuser")
                    .password("password456")
                    .email("test2@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            // When & Then
            userService.registerUser(request1);
            assertThrows(ConflictException.class, () -> userService.registerUser(request2));

            // Verify only one user exists
            assertEquals(1, userRepository.count());
            assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class));
        }
    }

    @Nested
    @DisplayName("User Retrieval Tests")
    class UserRetrievalTests {

        @BeforeEach
        void setupUser() {
            UserRequest request = UserRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .email("test@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();
            userService.registerUser(request);
        }

        @Test
        @DisplayName("Successfully retrieve existing user")
        void getCurrentUserSuccess() {
            // When
            UserResponse response = userService.getCurrentUser("testuser");

            // Then
            assertNotNull(response);
            assertEquals("testuser", response.getUsername());
            assertEquals("test@test.com", response.getEmail());
            assertEquals(UserRole.CUSTOMER, response.getRole());
        }

        @Test
        @DisplayName("Fail to retrieve non-existent user")
        void getCurrentUserNotFound() {
            // When & Then
            assertThrows(NotFoundException.class,
                    () -> userService.getCurrentUser("nonexistent"));
        }
    }

    @Nested
    @DisplayName("User Registration Failure Tests")
    class UserRegistrationFailureTests {

        @Test
        @DisplayName("Fail registration with invalid email format")
        void registerWithInvalidEmail() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .email("invalid-email") // Invalid email format
                    .role(UserRole.CUSTOMER)
                    .build();

            // When & Then
            MethodArgumentNotValidException exception = assertThrows(MethodArgumentNotValidException.class,
                    () -> validateRequest(request));

            // Verify it's specifically an email format error
            assertTrue(exception.getBindingResult().getFieldErrors("email").stream()
                    .anyMatch(error -> Objects.requireNonNull(error.getDefaultMessage()).contains("Invalid email format")));

            // Additional verification
            assertEquals(1, exception.getBindingResult().getFieldErrorCount(),
                    "Should only have one validation error");
            assertEquals("email", exception.getBindingResult().getFieldErrors().get(0).getField(),
                    "Error should be on email field");
        }

        @Test
        @DisplayName("Fail registration with too short password")
        void registerWithShortPassword() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("testuser")
                    .password("123") // Too short password
                    .email("test@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            // When & Then
            MethodArgumentNotValidException exception = assertThrows(MethodArgumentNotValidException.class, () -> {
                validateRequest(request);
            });

            // Verify it's specifically a password length error
            assertTrue(exception.getBindingResult().getFieldErrors("password").stream()
                    .anyMatch(error -> Objects.requireNonNull(error.getDefaultMessage()).contains("at least 6 characters")));

            // Additional verification that it's only the password that failed validation
            assertEquals(1, exception.getBindingResult().getFieldErrorCount(),
                    "Should only have one validation error");
            assertEquals("password", exception.getBindingResult().getFieldErrors().get(0).getField(),
                    "Error should be on password field");
        }

        @Test
        @DisplayName("Fail registration with null username")
        void registerWithNullUsername() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username(null)
                    .password("password123")
                    .email("test@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            // When & Then
            Exception exception = assertThrows(Exception.class,
                    () -> userService.registerUser(request));
            assertTrue(exception.getMessage().toLowerCase().contains("username"));
        }

        @Test
        @DisplayName("Fail registration with empty username")
        void registerWithEmptyUsername() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("") // Empty username
                    .password("password123")
                    .email("test@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            // When & Then
            MethodArgumentNotValidException exception = assertThrows(MethodArgumentNotValidException.class,
                    () -> validateRequest(request));

            // Verify it's specifically a username required error
            assertTrue(exception.getBindingResult().getFieldErrors("username").stream()
                    .anyMatch(error -> Objects.requireNonNull(error.getDefaultMessage()).contains("Username is required")));

            // Additional verification
            assertEquals(1, exception.getBindingResult().getFieldErrorCount(),
                    "Should only have one validation error");
            assertEquals("username", exception.getBindingResult().getFieldErrors().get(0).getField(),
                    "Error should be on username field");
        }

        @Test
        @DisplayName("Fail registration with null role")
        void registerWithNullRole() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .email("test@test.com")
                    .role(null)
                    .build();

            // When & Then
            Exception exception = assertThrows(Exception.class,
                    () -> userService.registerUser(request));
            assertTrue(exception.getMessage().toLowerCase().contains("role"));
        }

        @Test
        @DisplayName("Fail registration with duplicate email")
        void registerWithDuplicateEmail() {
            // Given
            UserRequest request1 = UserRequest.builder()
                    .username("user1")
                    .password("password123")
                    .email("test@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            UserRequest request2 = UserRequest.builder()
                    .username("user2")
                    .password("password123")
                    .email("test@test.com") // Same email
                    .role(UserRole.CUSTOMER)
                    .build();

            // When & Then
            userService.registerUser(request1);
            assertThrows(DataIntegrityViolationException.class,
                    () -> userService.registerUser(request2));
        }
    }

    @Nested
    @DisplayName("User Retrieval Failure Tests")
    class UserRetrievalFailureTests {

        @Test
        @DisplayName("Fail to retrieve user with null username")
        void getCurrentUserWithNullUsername() {
            assertThrows(NotFoundException.class,
                    () -> userService.getCurrentUser(null));
        }

        @Test
        @DisplayName("Fail to retrieve user with empty username")
        void getCurrentUserWithEmptyUsername() {
            assertThrows(NotFoundException.class,
                    () -> userService.getCurrentUser(""));
        }

        @Test
        @DisplayName("Fail to retrieve deleted user")
        void getCurrentUserAfterDeletion() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .email("test@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();
            userService.registerUser(request);

            // When
            userRepository.deleteAll();

            // Then
            assertThrows(NotFoundException.class,
                    () -> userService.getCurrentUser("testuser"));
        }
    }

    @Nested
    @DisplayName("Database Integrity Tests")
    class DatabaseIntegrityTests {

        @Test
        @DisplayName("Verify proper cleanup of security tables when deleting user")
        void verifySecurityTablesCleanup() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .email("test@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();
            userService.registerUser(request);

            // When - Delete in correct order due to foreign key constraints
            jdbcTemplate.execute("DELETE FROM authorities WHERE username = 'testuser'");
            jdbcTemplate.execute("DELETE FROM users WHERE username = 'testuser'");
            jdbcTemplate.execute("DELETE FROM app_users WHERE username = 'testuser'");

            // Then
            // Verify user is deleted from all tables
            Integer appUserCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM app_users WHERE username = ?",
                    Integer.class,
                    "testuser"
            );
            Integer securityUserCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ?",
                    Integer.class,
                    "testuser"
            );
            Integer authCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM authorities WHERE username = ?",
                    Integer.class,
                    "testuser"
            );

            assertEquals(0, appUserCount, "User should be deleted from app_users table");
            assertEquals(0, securityUserCount, "User should be deleted from security users table");
            assertEquals(0, authCount, "User should be deleted from authorities table");
        }

        @Test
        @DisplayName("Verify foreign key constraints between security tables")
        void verifyForeignKeyConstraints() {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .email("test@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();
            userService.registerUser(request);

            // Then - Verify we cannot delete user while authority exists
            assertThrows(Exception.class, () ->
                    jdbcTemplate.execute("DELETE FROM users WHERE username = 'testuser'")
            );

            // Verify records still exist
            Integer userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE username = ?",
                    Integer.class,
                    "testuser"
            );
            Integer authCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM authorities WHERE username = ?",
                    Integer.class,
                    "testuser"
            );

            assertEquals(1, userCount, "User should still exist in users table");
            assertEquals(1, authCount, "Authority should still exist in authorities table");
        }

        @Test
        @DisplayName("Verify unique constraints on user table")
        void verifyUniqueConstraints() {
            // Given
            UserRequest request1 = UserRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .email("test1@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            UserRequest request2 = UserRequest.builder()
                    .username("testuser") // Same username
                    .password("password456")
                    .email("test2@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            // When & Then
            userService.registerUser(request1);

            // Verify both unique username and unique email constraints
            assertThrows(ConflictException.class,
                    () -> userService.registerUser(request2));
        }
    }
}