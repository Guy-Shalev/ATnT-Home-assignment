package guy.shalev.ATnT.Home.assignment.integration;

import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import guy.shalev.ATnT.Home.assignment.repository.MovieRepository;
import guy.shalev.ATnT.Home.assignment.service.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Movie Integration Tests")
class MovieIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MovieService movieService;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private Validator validator;

    @BeforeEach
    void setUp() {
        movieRepository.deleteAll();
    }

    private void validateRequest(MovieRequest request) throws MethodArgumentNotValidException {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "movieRequest");
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            throw new MethodArgumentNotValidException(null, errors);
        }
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
    @DisplayName("Movie Creation Tests")
    class MovieCreationTests {

        @Test
        @DisplayName("Successfully create a movie")
        void createMovieSuccess() {
            // Given
            MovieRequest request = createValidMovieRequest();

            // When
            MovieResponse response = movieService.createMovie(request);

            // Then
            assertNotNull(response.getId());
            assertEquals(request.getTitle(), response.getTitle());
            assertEquals(request.getGenre(), response.getGenre());
            assertEquals(request.getDuration(), response.getDuration());
            assertEquals(request.getRating(), response.getRating());
            assertEquals(request.getReleaseYear(), response.getReleaseYear());

            // Verify persistence
            assertTrue(movieRepository.findById(response.getId()).isPresent());
        }

        @Test
        @DisplayName("Fail creation with invalid duration")
        void createMovieWithInvalidDuration() {
            // Given
            MovieRequest request = createValidMovieRequest();
            request.setDuration(-120); // Invalid duration

            // When & Then
            MethodArgumentNotValidException exception = assertThrows(
                    MethodArgumentNotValidException.class,
                    () -> validateRequest(request)
            );

            assertTrue(exception.getBindingResult().getFieldErrors("duration").stream()
                    .anyMatch(error -> Objects.requireNonNull(error.getDefaultMessage()).contains("must be positive")));
        }

        @Test
        @DisplayName("Fail creation with null title")
        void createMovieWithNullTitle() {
            // Given
            MovieRequest request = createValidMovieRequest();
            request.setTitle(null);

            // When & Then
            MethodArgumentNotValidException exception = assertThrows(
                    MethodArgumentNotValidException.class,
                    () -> validateRequest(request)
            );

            assertTrue(exception.getBindingResult().getFieldErrors("title").stream()
                    .anyMatch(error -> Objects.requireNonNull(error.getDefaultMessage()).contains("required")));
        }
    }

    @Nested
    @DisplayName("Movie Retrieval Tests")
    class MovieRetrievalTests {

        private MovieResponse savedMovie;

        @BeforeEach
        void setUp() {
            MovieRequest request = createValidMovieRequest();
            savedMovie = movieService.createMovie(request);
        }

        @Test
        @DisplayName("Successfully retrieve a movie by ID")
        void getMovieByIdSuccess() {
            // When
            MovieResponse response = movieService.getMovie(savedMovie.getId());

            // Then
            assertEquals(savedMovie.getId(), response.getId());
            assertEquals(savedMovie.getTitle(), response.getTitle());
        }

        @Test
        @DisplayName("Fail to retrieve non-existent movie")
        void getMovieByIdNotFound() {
            // When & Then
            assertThrows(NotFoundException.class,
                    () -> movieService.getMovie(999L));
        }

        @Test
        @DisplayName("Successfully retrieve all movies")
        void getAllMoviesSuccess() {
            // Given
            MovieRequest secondMovie = createValidMovieRequest();
            secondMovie.setTitle("Second Movie");
            movieService.createMovie(secondMovie);

            // When
            List<MovieResponse> movies = movieService.getAllMovies();

            // Then
            assertEquals(2, movies.size());
            assertTrue(movies.stream()
                    .map(MovieResponse::getTitle)
                    .anyMatch(title -> title.equals("Second Movie")));
        }
    }

    @Nested
    @DisplayName("Movie Update Tests")
    class MovieUpdateTests {

        private MovieResponse savedMovie;

        @BeforeEach
        void setUp() {
            MovieRequest request = createValidMovieRequest();
            savedMovie = movieService.createMovie(request);
        }

        @Test
        @DisplayName("Successfully update a movie")
        void updateMovieSuccess() {
            // Given
            MovieRequest updateRequest = createValidMovieRequest();
            updateRequest.setTitle("Updated Title");
            updateRequest.setDuration(150);

            // When
            MovieResponse updated = movieService.updateMovie(savedMovie.getId(), updateRequest);

            // Then
            assertEquals("Updated Title", updated.getTitle());
            assertEquals(150, updated.getDuration());
            assertEquals(savedMovie.getId(), updated.getId());

            // Verify persistence
            Movie persistedMovie = movieRepository.findById(updated.getId()).orElseThrow();
            assertEquals("Updated Title", persistedMovie.getTitle());
            assertEquals(150, persistedMovie.getDuration());
        }

        @Test
        @DisplayName("Fail to update non-existent movie")
        void updateNonExistentMovie() {
            // Given
            MovieRequest updateRequest = createValidMovieRequest();

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> movieService.updateMovie(999L, updateRequest));
        }
    }

    @Nested
    @DisplayName("Movie Deletion Tests")
    class MovieDeletionTests {

        private MovieResponse savedMovie;

        @BeforeEach
        void setUp() {
            MovieRequest request = createValidMovieRequest();
            savedMovie = movieService.createMovie(request);
        }

        @Test
        @DisplayName("Successfully delete a movie")
        void deleteMovieSuccess() {
            // When
            movieService.deleteMovie(savedMovie.getId());

            // Then
            assertFalse(movieRepository.existsById(savedMovie.getId()));
        }

        @Test
        @DisplayName("Fail to delete non-existent movie")
        void deleteNonExistentMovie() {
            // When & Then
            assertThrows(NotFoundException.class,
                    () -> movieService.deleteMovie(999L));
        }
    }

    @Nested
    @DisplayName("Movie Search Tests")
    class MovieSearchTests {

        @BeforeEach
        void setUp() {
            // Create multiple movies for search testing
            MovieRequest movie1 = new MovieRequest("Action Movie", "Action", 120, "PG-13", 2024);
            MovieRequest movie2 = new MovieRequest("Comedy Movie", "Comedy", 90, "PG", 2023);
            MovieRequest movie3 = new MovieRequest("Another Action", "Action", 150, "R", 2024);

            movieService.createMovie(movie1);
            movieService.createMovie(movie2);
            movieService.createMovie(movie3);
        }

        @Test
        @DisplayName("Successfully search movies by genre")
        void searchMoviesByGenre() {
            // When
            List<MovieResponse> actionMovies = movieService.searchMovies(null, "Action", null, null, null);

            // Then
            assertEquals(2, actionMovies.size());
            assertTrue(actionMovies.stream()
                    .allMatch(movie -> movie.getGenre().equals("Action")));
        }

        @Test
        @DisplayName("Successfully search movies by release year")
        void searchMoviesByYear() {
            // When
            List<MovieResponse> movies2024 = movieService.searchMovies(null, null, null, null, 2024);

            // Then
            assertEquals(2, movies2024.size());
            assertTrue(movies2024.stream()
                    .allMatch(movie -> movie.getReleaseYear() == 2024));
        }

        @Test
        @DisplayName("Successfully search movies by title pattern")
        void searchMoviesByTitlePattern() {
            // When
            List<MovieResponse> moviesWithAction = movieService.searchMovies("Action", null, null, null, null);

            // Then
            assertEquals(2, moviesWithAction.size());
            assertTrue(moviesWithAction.stream()
                    .allMatch(movie -> movie.getTitle().contains("Action")));
        }

        @Test
        @DisplayName("Search with no results")
        void searchMoviesNoResults() {
            // When
            List<MovieResponse> movies = movieService.searchMovies(null, "Horror", null, null, null);

            // Then
            assertTrue(movies.isEmpty());
        }

        @Test
        @DisplayName("Search with multiple criteria")
        void searchMoviesMultipleCriteria() {
            // When
            List<MovieResponse> movies = movieService.searchMovies(
                    null, "Action", null, null, 2024
            );

            // Then
            assertEquals(2, movies.size());
            assertTrue(movies.stream().allMatch(movie ->
                    movie.getGenre().equals("Action") && movie.getReleaseYear() == 2024
            ));
        }
    }
}