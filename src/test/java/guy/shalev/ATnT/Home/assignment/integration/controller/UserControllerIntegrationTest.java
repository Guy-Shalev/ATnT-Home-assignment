package guy.shalev.ATnT.Home.assignment.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import guy.shalev.ATnT.Home.assignment.integration.BaseIntegrationTest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.UserRepository;
import guy.shalev.ATnT.Home.assignment.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Base64;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("User Controller Integration Tests")
class UserControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean up users
        jdbcTemplate.execute("DELETE FROM authorities");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM app_users");
    }

    private UserRequest createValidUserRequest(UserRole role) {
        return UserRequest.builder()
                .username("testuser")
                .password("password123")
                .email("test@example.com")
                .role(role)
                .build();
    }

    @Nested
    @DisplayName("User Registration Endpoint Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("Successfully register customer user")
        void registerCustomerSuccess() throws Exception {
            // Given
            UserRequest request = createValidUserRequest(UserRole.CUSTOMER);

            // When
            ResultActions result = mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isCreated());

            // Verify user was created in both app and security tables
            mockMvc.perform(get("/api/users/current")
                            .header("Authorization", "Basic " + Base64.getEncoder()
                                    .encodeToString((request.getUsername() + ":" + request.getPassword()).getBytes())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is(request.getUsername())))
                    .andExpect(jsonPath("$.email", is(request.getEmail())))
                    .andExpect(jsonPath("$.role", is(request.getRole().toString())));
        }

        @Test
        @DisplayName("Successfully register admin user")
        void registerAdminSuccess() throws Exception {
            // Given
            UserRequest request = createValidUserRequest(UserRole.ADMIN);

            // When
            ResultActions result = mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isCreated());

            // Verify admin was created with correct role
            mockMvc.perform(get("/api/users/current")
                            .header("Authorization", "Basic " + Base64.getEncoder()
                                    .encodeToString((request.getUsername() + ":" + request.getPassword()).getBytes())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", is("ADMIN")));
        }

        @Test
        @DisplayName("Fail registration with duplicate username")
        void registerDuplicateUsername() throws Exception {
            // Given
            UserRequest request1 = createValidUserRequest(UserRole.CUSTOMER);
            userService.registerUser(request1);

            UserRequest request2 = UserRequest.builder()
                    .username("testuser") // Same username
                    .password("password456")
                    .email("test2@example.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            // When
            ResultActions result = mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)));

            // Then
            result.andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Fail registration with invalid email")
        void registerInvalidEmail() throws Exception {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("testuser")
                    .password("password123")
                    .email("invalid-email") // Invalid email format
                    .role(UserRole.CUSTOMER)
                    .build();

            // When
            ResultActions result = mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email", notNullValue()));
        }

        @Test
        @DisplayName("Fail registration with short password")
        void registerShortPassword() throws Exception {
            // Given
            UserRequest request = UserRequest.builder()
                    .username("testuser")
                    .password("123") // Too short
                    .email("test@example.com")
                    .role(UserRole.CUSTOMER)
                    .build();

            // When
            ResultActions result = mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password", notNullValue()));
        }
    }

    @Nested
    @DisplayName("Current User Endpoint Tests")
    class CurrentUserTests {

        private String authHeader;

        @BeforeEach
        void setUp() {
            // Create test user
            UserRequest request = createValidUserRequest(UserRole.CUSTOMER);
            userService.registerUser(request);
            authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((request.getUsername() + ":" + request.getPassword()).getBytes());
        }

        @Test
        @DisplayName("Successfully get current user")
        void getCurrentUserSuccess() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/users/current")
                    .header("Authorization", authHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is("testuser")))
                    .andExpect(jsonPath("$.email", is("test@example.com")))
                    .andExpect(jsonPath("$.role", is("CUSTOMER")));
        }

        @Test
        @DisplayName("Fail to get current user without authentication")
        void getCurrentUserUnauthorized() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/users/current")
                    .accept(MediaType.APPLICATION_JSON));

            // Then
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Fail to get current user with invalid credentials")
        void getCurrentUserInvalidCredentials() throws Exception {
            // Given
            String invalidAuthHeader = "Basic " + Base64.getEncoder()
                    .encodeToString("testuser:wrongpassword".getBytes());

            // When
            ResultActions result = mockMvc.perform(get("/api/users/current")
                    .header("Authorization", invalidAuthHeader));

            // Then
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("User Authentication Tests")
    class UserAuthenticationTests {

        @Test
        @DisplayName("Verify admin access restrictions")
        void verifyAdminAccess() throws Exception {
            // Given
            UserRequest adminRequest = UserRequest.builder()
                    .username("admin")
                    .password("adminPass123")
                    .email("admin@example.com")
                    .role(UserRole.ADMIN)
                    .build();
            userService.registerUser(adminRequest);

            String adminAuthHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((adminRequest.getUsername() + ":" + adminRequest.getPassword()).getBytes());

            // When & Then
            mockMvc.perform(get("/api/users/current")
                            .header("Authorization", adminAuthHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", is("ADMIN")));
        }

        @Test
        @DisplayName("Verify customer access restrictions")
        void verifyCustomerAccess() throws Exception {
            // Given
            UserRequest customerRequest = UserRequest.builder()
                    .username("customer")
                    .password("customerPass123")
                    .email("customer@example.com")
                    .role(UserRole.CUSTOMER)
                    .build();
            userService.registerUser(customerRequest);

            String customerAuthHeader = "Basic " + Base64.getEncoder()
                    .encodeToString((customerRequest.getUsername() + ":" + customerRequest.getPassword()).getBytes());

            // When & Then
            mockMvc.perform(get("/api/users/current")
                            .header("Authorization", customerAuthHeader))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", is("CUSTOMER")));
        }
    }
}