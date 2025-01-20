package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.ErrorCode;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.UserResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.User;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private UserRequest userRequest;
    private User user;
    private final String username = "testUser";
    private final String email = "test@example.com";
    private final String password = "password123";
    private final String encodedPassword = "encodedPassword123";

    @BeforeEach
    void setUp() {
        // Initialize test data
        userRequest = UserRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .role(UserRole.CUSTOMER)
                .build();

        user = User.builder()
                .id(1L)
                .username(username)
                .email(email)
                .role(UserRole.CUSTOMER)
                .build();
    }

    @Test
    void registerUser_Success() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.registerUser(userRequest);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(username, savedUser.getUsername());
        assertEquals(email, savedUser.getEmail());
        assertEquals(UserRole.CUSTOMER, savedUser.getRole());

        // Verify security details were inserted
        verify(userRepository).insertSecurityDetails(
                eq(username),
                eq(encodedPassword),
                eq(UserRole.CUSTOMER),
                any(JdbcTemplate.class)
        );
    }

    @Test
    void registerUser_UsernameExists() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> userService.registerUser(userRequest));
        assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS, exception.getErrorCode());

        // Verify no saves or security inserts were performed
        verify(userRepository, never()).save(any(User.class));
        verify(userRepository, never()).insertSecurityDetails(
                any(), any(), any(), any()
        );
    }

    @Test
    void registerUser_AdminRole() {
        // Arrange
        userRequest.setRole(UserRole.ADMIN);
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.registerUser(userRequest);

        // Assert
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserRole.ADMIN, savedUser.getRole());

        // Verify correct role was used in security details
        verify(userRepository).insertSecurityDetails(
                eq(username),
                eq(encodedPassword),
                eq(UserRole.ADMIN),
                any(JdbcTemplate.class)
        );
    }

    @Test
    void getCurrentUser_Success() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserResponse result = userService.getCurrentUser(username);

        // Assert
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(username, result.getUsername());
        assertEquals(email, result.getEmail());
        assertEquals(UserRole.CUSTOMER, result.getRole());
    }

    @Test
    void getCurrentUser_NotFound() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> userService.getCurrentUser(username));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void registerUser_VerifyPasswordEncoding() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.registerUser(userRequest);

        // Assert
        verify(passwordEncoder).encode(password);
        verify(userRepository).insertSecurityDetails(
                eq(username),
                eq(encodedPassword), // Verify encoded password is used
                any(),
                any()
        );
    }

    @Test
    void registerUser_ValidateJdbcOperations() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        userService.registerUser(userRequest);

        // Assert
        verify(userRepository).insertSecurityDetails(
                eq(username),
                eq(encodedPassword),
                eq(UserRole.CUSTOMER),
                eq(jdbcTemplate) // Verify correct JdbcTemplate is used
        );
    }

    @Test
    void getCurrentUser_VerifyResponseMapping() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        UserResponse result = userService.getCurrentUser(username);

        // Assert
        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getEmail(), result.getEmail());
        assertEquals(user.getRole(), result.getRole());
    }
}