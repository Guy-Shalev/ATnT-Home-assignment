package guy.shalev.ATnT.Home.assignment.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import guy.shalev.ATnT.Home.assignment.integration.BaseIntegrationTest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.TheaterRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.MovieRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Showtime Controller Integration Tests")
class ShowtimeControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private TheaterService theaterService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminAuthHeader;
    private String customerAuthHeader;
    private MovieResponse testMovie;
    private TheaterResponse testTheater;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        // Clean up the database
        showtimeRepository.deleteAll();
        movieRepository.deleteAll();
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

        // Create test movie
        MovieRequest movieRequest = new MovieRequest(
                "Test Movie",
                "Action",
                120,
                "PG-13",
                2024
        );
        testMovie = movieService.createMovie(movieRequest);

        // Create test theater
        TheaterRequest theaterRequest = new TheaterRequest(
                "Test Theater",
                100
        );
        testTheater = theaterService.createTheater(theaterRequest);

        // Set base datetime
        baseDateTime = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
    }

    private ShowtimeRequest createValidShowtimeRequest() {
        return new ShowtimeRequest(
                testMovie.getId(),
                testTheater.getId(),
                baseDateTime,
                50
        );
    }

    @Nested
    @DisplayName("Showtime Creation Endpoint Tests")
    class ShowtimeCreationTests {

        @Test
        @DisplayName("Successfully create showtime as admin")
        void createShowtimeAsAdmin() throws Exception {
            // Given
            ShowtimeRequest request = createValidShowtimeRequest();

            // When
            ResultActions result = mockMvc.perform(post("/api/showtimes")
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.movie.id").value(testMovie.getId()))
                    .andExpect(jsonPath("$.theater.id").value(testTheater.getId()))
                    .andExpect(jsonPath("$.startTime").exists())
                    .andExpect(jsonPath("$.maxSeats").value(50))
                    .andExpect(jsonPath("$.availableSeats").value(50));
        }

        @Test
        @DisplayName("Fail to create showtime as customer")
        void createShowtimeAsCustomer() throws Exception {
            // Given
            ShowtimeRequest request = createValidShowtimeRequest();

            // When
            ResultActions result = mockMvc.perform(post("/api/showtimes")
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Fail to create overlapping showtime")
        void createOverlappingShowtime() throws Exception {
            // Given
            ShowtimeRequest request1 = createValidShowtimeRequest();
            showtimeService.createShowtime(request1);

            ShowtimeRequest request2 = createValidShowtimeRequest();
            request2.setStartTime(baseDateTime.plusMinutes(30)); // Overlapping time

            // When
            ResultActions result = mockMvc.perform(post("/api/showtimes")
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)));

            // Then
            result.andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("Showtime Retrieval Endpoint Tests")
    class ShowtimeRetrievalTests {

        private ShowtimeResponse savedShowtime;

        @BeforeEach
        void setUp() {
            ShowtimeRequest request = createValidShowtimeRequest();
            savedShowtime = showtimeService.createShowtime(request);
        }

        @Test
        @DisplayName("Successfully get showtime by ID")
        void getShowtimeById() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/showtimes/{id}", savedShowtime.getId())
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedShowtime.getId()))
                    .andExpect(jsonPath("$.movie.id").value(testMovie.getId()))
                    .andExpect(jsonPath("$.theater.id").value(testTheater.getId()));
        }

        @Test
        @DisplayName("Successfully get showtimes by movie")
        void getShowtimesByMovie() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/showtimes/movie/{movieId}", testMovie.getId())
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(savedShowtime.getId()));
        }

        @Test
        @DisplayName("Successfully get showtimes by theater")
        void getShowtimesByTheater() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/showtimes/theater/{theaterId}", testTheater.getId())
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id").value(savedShowtime.getId()));
        }
    }

    @Nested
    @DisplayName("Showtime Update Endpoint Tests")
    class ShowtimeUpdateTests {

        private ShowtimeResponse savedShowtime;

        @BeforeEach
        void setUp() {
            ShowtimeRequest request = createValidShowtimeRequest();
            savedShowtime = showtimeService.createShowtime(request);
        }

        @Test
        @DisplayName("Successfully update showtime as admin")
        void updateShowtimeAsAdmin() throws Exception {
            // Given
            ShowtimeRequest updateRequest = createValidShowtimeRequest();
            updateRequest.setStartTime(baseDateTime.plusHours(1));
            updateRequest.setMaxSeats(75);

            // When
            ResultActions result = mockMvc.perform(put("/api/showtimes/{id}", savedShowtime.getId())
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedShowtime.getId()))
                    .andExpect(jsonPath("$.maxSeats").value(75))
                    .andExpect(jsonPath("$.availableSeats").value(75));
        }

        @Test
        @DisplayName("Fail to update showtime as customer")
        void updateShowtimeAsCustomer() throws Exception {
            // Given
            ShowtimeRequest updateRequest = createValidShowtimeRequest();

            // When
            ResultActions result = mockMvc.perform(put("/api/showtimes/{id}", savedShowtime.getId())
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)));

            // Then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Showtime Deletion Endpoint Tests")
    class ShowtimeDeletionTests {

        private ShowtimeResponse savedShowtime;

        @BeforeEach
        void setUp() {
            ShowtimeRequest request = createValidShowtimeRequest();
            savedShowtime = showtimeService.createShowtime(request);
        }

        @Test
        @DisplayName("Successfully delete showtime as admin")
        void deleteShowtimeAsAdmin() throws Exception {
            // When
            ResultActions result = mockMvc.perform(delete("/api/showtimes/{id}", savedShowtime.getId())
                    .header("Authorization", adminAuthHeader));

            // Then
            result.andExpect(status().isNoContent());

            // Verify deletion
            mockMvc.perform(get("/api/showtimes/{id}", savedShowtime.getId())
                            .header("Authorization", adminAuthHeader))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Fail to delete showtime as customer")
        void deleteShowtimeAsCustomer() throws Exception {
            // When
            ResultActions result = mockMvc.perform(delete("/api/showtimes/{id}", savedShowtime.getId())
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Showtime Availability Endpoint Tests")
    class ShowtimeAvailabilityTests {

        @Test
        @DisplayName("Successfully check available time slot")
        void checkAvailableTimeSlot() throws Exception {
            // Given
            LocalDateTime startTime = baseDateTime.plusHours(2);
            LocalDateTime endTime = startTime.plusHours(2);

            // When
            ResultActions result = mockMvc.perform(get("/api/showtimes/available")
                    .param("theaterId", testTheater.getId().toString())
                    .param("startTime", startTime.format(DateTimeFormatter.ISO_DATE_TIME))
                    .param("endTime", endTime.format(DateTimeFormatter.ISO_DATE_TIME))
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
        }

        @Test
        @DisplayName("Successfully detect unavailable time slot")
        void checkUnavailableTimeSlot() throws Exception {
            // Given
            ShowtimeRequest request = createValidShowtimeRequest();
            showtimeService.createShowtime(request);

            // When
            ResultActions result = mockMvc.perform(get("/api/showtimes/available")
                    .param("theaterId", testTheater.getId().toString())
                    .param("startTime", baseDateTime.plusMinutes(30).format(DateTimeFormatter.ISO_DATE_TIME))
                    .param("endTime", baseDateTime.plusMinutes(150).format(DateTimeFormatter.ISO_DATE_TIME))
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(false));
        }
    }
}