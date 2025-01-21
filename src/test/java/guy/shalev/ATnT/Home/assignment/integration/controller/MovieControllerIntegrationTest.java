package guy.shalev.ATnT.Home.assignment.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import guy.shalev.ATnT.Home.assignment.integration.BaseIntegrationTest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.MovieRepository;
import guy.shalev.ATnT.Home.assignment.service.MovieService;
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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Movie Controller Integration Tests")
class MovieControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MovieService movieService;

    @Autowired
    private UserService userService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminAuthHeader;
    private String customerAuthHeader;

    @BeforeEach
    void setUp() {
        // Clean up the database
        movieRepository.deleteAll();
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

    private MovieRequest createValidMovieRequest() {
        return new MovieRequest(
                "Test Movie",
                "Action",
                120,
                "PG-13",
                2024
        );
    }

    @Nested
    @DisplayName("Movie Creation Endpoint Tests")
    class MovieCreationTests {

        @Test
        @DisplayName("Successfully create movie as admin")
        void createMovieAsAdmin() throws Exception {
            // Given
            MovieRequest request = createValidMovieRequest();

            // When
            ResultActions result = mockMvc.perform(post("/api/movies")
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value(request.getTitle()))
                    .andExpect(jsonPath("$.genre").value(request.getGenre()))
                    .andExpect(jsonPath("$.duration").value(request.getDuration()))
                    .andExpect(jsonPath("$.rating").value(request.getRating()))
                    .andExpect(jsonPath("$.releaseYear").value(request.getReleaseYear()));
        }

        @Test
        @DisplayName("Fail to create movie as customer")
        void createMovieAsCustomer() throws Exception {
            // Given
            MovieRequest request = createValidMovieRequest();

            // When
            ResultActions result = mockMvc.perform(post("/api/movies")
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Fail to create movie with invalid data")
        void createMovieWithInvalidData() throws Exception {
            // Given
            MovieRequest request = createValidMovieRequest();
            request.setDuration(-120); // Invalid duration

            // When
            ResultActions result = mockMvc.perform(post("/api/movies")
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.duration").exists());
        }
    }

    @Nested
    @DisplayName("Movie Retrieval Endpoint Tests")
    class MovieRetrievalTests {

        private MovieResponse savedMovie;

        @BeforeEach
        void setUp() {
            MovieRequest request = createValidMovieRequest();
            savedMovie = movieService.createMovie(request);
        }

        @Test
        @DisplayName("Successfully get movie by ID")
        void getMovieById() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/movies/{id}", savedMovie.getId())
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedMovie.getId()))
                    .andExpect(jsonPath("$.title").value(savedMovie.getTitle()));
        }

        @Test
        @DisplayName("Successfully get all movies")
        void getAllMovies() throws Exception {
            // Given
            MovieRequest secondMovie = createValidMovieRequest();
            secondMovie.setTitle("Second Movie");
            movieService.createMovie(secondMovie);

            // When
            ResultActions result = mockMvc.perform(get("/api/movies")
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[*].title", containsInAnyOrder("Test Movie", "Second Movie")));
        }

        @Test
        @DisplayName("Successfully search movies")
        void searchMovies() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/movies/search")
                    .param("genre", "Action")
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].genre").value("Action"));
        }
    }

    @Nested
    @DisplayName("Movie Update Endpoint Tests")
    class MovieUpdateTests {

        private MovieResponse savedMovie;

        @BeforeEach
        void setUp() {
            MovieRequest request = createValidMovieRequest();
            savedMovie = movieService.createMovie(request);
        }

        @Test
        @DisplayName("Successfully update movie as admin")
        void updateMovieAsAdmin() throws Exception {
            // Given
            MovieRequest updateRequest = createValidMovieRequest();
            updateRequest.setTitle("Updated Title");

            // When
            ResultActions result = mockMvc.perform(put("/api/movies/{id}", savedMovie.getId())
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(savedMovie.getId()))
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }

        @Test
        @DisplayName("Fail to update movie as customer")
        void updateMovieAsCustomer() throws Exception {
            // Given
            MovieRequest updateRequest = createValidMovieRequest();

            // When
            ResultActions result = mockMvc.perform(put("/api/movies/{id}", savedMovie.getId())
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)));

            // Then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Fail to update non-existent movie")
        void updateNonExistentMovie() throws Exception {
            // Given
            MovieRequest updateRequest = createValidMovieRequest();

            // When
            ResultActions result = mockMvc.perform(put("/api/movies/{id}", 999)
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)));

            // Then
            result.andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Movie Deletion Endpoint Tests")
    class MovieDeletionTests {

        private MovieResponse savedMovie;

        @BeforeEach
        void setUp() {
            MovieRequest request = createValidMovieRequest();
            savedMovie = movieService.createMovie(request);
        }

        @Test
        @DisplayName("Successfully delete movie as admin")
        void deleteMovieAsAdmin() throws Exception {
            // When
            ResultActions result = mockMvc.perform(delete("/api/movies/{id}", savedMovie.getId())
                    .header("Authorization", adminAuthHeader));

            // Then
            result.andExpect(status().isNoContent());

            // Verify deletion
            mockMvc.perform(get("/api/movies/{id}", savedMovie.getId())
                            .header("Authorization", adminAuthHeader))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Fail to delete movie as customer")
        void deleteMovieAsCustomer() throws Exception {
            // When
            ResultActions result = mockMvc.perform(delete("/api/movies/{id}", savedMovie.getId())
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Fail to delete non-existent movie")
        void deleteNonExistentMovie() throws Exception {
            // When
            ResultActions result = mockMvc.perform(delete("/api/movies/{id}", Long.MAX_VALUE)
                    .header("Authorization", adminAuthHeader));

            // Then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Movie not found with id: " + Long.MAX_VALUE))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}