package guy.shalev.ATnT.Home.assignment.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import guy.shalev.ATnT.Home.assignment.integration.BaseIntegrationTest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.TheaterRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.ShowtimeRepository;
import guy.shalev.ATnT.Home.assignment.repository.TheaterRepository;
import guy.shalev.ATnT.Home.assignment.service.MovieService;
import guy.shalev.ATnT.Home.assignment.service.ShowtimeService;
import guy.shalev.ATnT.Home.assignment.service.TheaterService;
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

import java.time.LocalDateTime;
import java.util.Base64;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Theater Controller Integration Tests")
class TheaterControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TheaterService theaterService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private UserService userService;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminAuthHeader;
    private String customerAuthHeader;

    @BeforeEach
    void setUp() {
        // Clean up the database
        showtimeRepository.deleteAll();
        theaterRepository.deleteAll();
        jdbcTemplate.execute("DELETE FROM authorities");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM app_users");

        // Create admin user
        UserRequest adminRequest = UserRequest.builder()
                .username("admin")
                .password("adminPass123")
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .build();
        userService.registerUser(adminRequest);
        adminAuthHeader = "Basic " + Base64.getEncoder().encodeToString("admin:adminPass123".getBytes());

        // Create customer user
        UserRequest customerRequest = UserRequest.builder()
                .username("customer")
                .password("customerPass123")
                .email("customer@test.com")
                .role(UserRole.CUSTOMER)
                .build();
        userService.registerUser(customerRequest);
        customerAuthHeader = "Basic " + Base64.getEncoder().encodeToString("customer:customerPass123".getBytes());
    }

    private TheaterRequest createValidTheaterRequest() {
        return new TheaterRequest(
                "Test Theater",
                100
        );
    }

    @Nested
    @DisplayName("Theater Creation Endpoint Tests")
    class TheaterCreationTests {

        @Test
        @DisplayName("Successfully create theater as admin")
        void createTheaterAsAdmin() throws Exception {
            // Given
            TheaterRequest request = createValidTheaterRequest();

            // When
            ResultActions result = mockMvc.perform(post("/api/theaters")
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value(request.getName()))
                    .andExpect(jsonPath("$.capacity").value(request.getCapacity()));
        }

        @Test
        @DisplayName("Fail to create theater as customer")
        void createTheaterAsCustomer() throws Exception {
            // Given
            TheaterRequest request = createValidTheaterRequest();

            // When
            ResultActions result = mockMvc.perform(post("/api/theaters")
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Fail to create theater with invalid data")
        void createTheaterWithInvalidData() throws Exception {
            // Given
            TheaterRequest request = new TheaterRequest("", -1); // Invalid name and capacity

            // When
            ResultActions result = mockMvc.perform(post("/api/theaters")
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.name").exists())
                    .andExpect(jsonPath("$.errors.capacity").exists());
        }

        @Test
        @DisplayName("Fail to create theater with duplicate name")
        void createTheaterWithDuplicateName() throws Exception {
            // Given
            TheaterRequest request = createValidTheaterRequest();
            theaterService.createTheater(request);

            // When
            ResultActions result = mockMvc.perform(post("/api/theaters")
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Theater already exists with name: " + request.getName()));
        }
    }

    @Nested
    @DisplayName("Theater Retrieval Endpoint Tests")
    class TheaterRetrievalTests {

        private TheaterResponse savedTheater;

        @BeforeEach
        void setUp() {
            TheaterRequest request = createValidTheaterRequest();
            savedTheater = theaterService.createTheater(request);
        }

        @Test
        @DisplayName("Successfully get theater by ID")
        void getTheaterById() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/theaters/{id}", savedTheater.getId())
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedTheater.getId()))
                    .andExpect(jsonPath("$.name").value(savedTheater.getName()))
                    .andExpect(jsonPath("$.capacity").value(savedTheater.getCapacity()));
        }

        @Test
        @DisplayName("Successfully get all theaters")
        void getAllTheaters() throws Exception {
            // Given
            TheaterRequest secondTheater = new TheaterRequest("Second Theater", 150);
            theaterService.createTheater(secondTheater);

            // When
            ResultActions result = mockMvc.perform(get("/api/theaters")
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value(savedTheater.getName()))
                    .andExpect(jsonPath("$[1].name").value("Second Theater"));
        }

        @Test
        @DisplayName("Fail to get non-existent theater")
        void getNonExistentTheater() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/theaters/{id}", 999L)
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Theater not found with id: 999"));
        }
    }

    @Nested
    @DisplayName("Theater Update Endpoint Tests")
    class TheaterUpdateTests {

        private TheaterResponse savedTheater;

        @BeforeEach
        void setUp() {
            TheaterRequest request = createValidTheaterRequest();
            savedTheater = theaterService.createTheater(request);
        }

        @Test
        @DisplayName("Successfully update theater as admin")
        void updateTheaterAsAdmin() throws Exception {
            // Given
            TheaterRequest updateRequest = new TheaterRequest(
                    "Updated Theater Name",
                    150
            );

            // When
            ResultActions result = mockMvc.perform(put("/api/theaters/{id}", savedTheater.getId())
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedTheater.getId()))
                    .andExpect(jsonPath("$.name").value("Updated Theater Name"))
                    .andExpect(jsonPath("$.capacity").value(150));
        }

        @Test
        @DisplayName("Fail to update theater as customer")
        void updateTheaterAsCustomer() throws Exception {
            // Given
            TheaterRequest updateRequest = createValidTheaterRequest();

            // When
            ResultActions result = mockMvc.perform(put("/api/theaters/{id}", savedTheater.getId())
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)));

            // Then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Fail to update with duplicate name")
        void updateTheaterWithDuplicateName() throws Exception {
            // Given
            TheaterRequest anotherTheater = new TheaterRequest("Another Theater", 200);
            theaterService.createTheater(anotherTheater);

            TheaterRequest updateRequest = new TheaterRequest("Another Theater", 150);

            // When
            ResultActions result = mockMvc.perform(put("/api/theaters/{id}", savedTheater.getId())
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)));

            // Then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Theater already exists with name: Another Theater"));
        }
    }

    @Nested
    @DisplayName("Theater Deletion Endpoint Tests")
    class TheaterDeletionTests {

        private TheaterResponse savedTheater;

        @BeforeEach
        void setUp() {
            TheaterRequest request = createValidTheaterRequest();
            savedTheater = theaterService.createTheater(request);
        }

        @Test
        @DisplayName("Successfully delete theater as admin")
        void deleteTheaterAsAdmin() throws Exception {
            // When
            ResultActions result = mockMvc.perform(delete("/api/theaters/{id}", savedTheater.getId())
                    .header("Authorization", adminAuthHeader));

            // Then
            result.andExpect(status().isNoContent());

            // Verify deletion
            mockMvc.perform(get("/api/theaters/{id}", savedTheater.getId())
                            .header("Authorization", adminAuthHeader))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Fail to delete theater as customer")
        void deleteTheaterAsCustomer() throws Exception {
            // When
            ResultActions result = mockMvc.perform(delete("/api/theaters/{id}", savedTheater.getId())
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Fail to delete theater with scheduled showtimes")
        void deleteTheaterWithShowtimes() throws Exception {
            // Given
            MovieResponse movie = createTestMovie();
            createTestShowtime(movie, savedTheater);

            // When
            ResultActions result = mockMvc.perform(delete("/api/theaters/{id}", savedTheater.getId())
                    .header("Authorization", adminAuthHeader));

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cannot delete theater with scheduled showtimes"));
        }

        private MovieResponse createTestMovie() {
            MovieRequest movieRequest = new MovieRequest(
                    "Test Movie",
                    "Action",
                    120,
                    "PG-13",
                    2024
            );
            return movieService.createMovie(movieRequest);
        }

        private void createTestShowtime(MovieResponse movie, TheaterResponse theater) {
            ShowtimeRequest showtimeRequest = new ShowtimeRequest(
                    movie.getId(),
                    theater.getId(),
                    LocalDateTime.now().plusDays(1),
                    50
            );
            showtimeService.createShowtime(showtimeRequest);
        }
    }
}