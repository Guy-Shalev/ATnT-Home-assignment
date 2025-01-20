package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.ErrorCode;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.mapper.MovieMapper;
import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import guy.shalev.ATnT.Home.assignment.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private MovieMapper movieMapper;

    @InjectMocks
    private MovieServiceImpl movieService;

    private Movie movie;
    private MovieRequest movieRequest;
    private MovieResponse movieResponse;

    @BeforeEach
    void setUp() {
        // Initialize test data
        movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Test Movie");
        movie.setGenre("Action");
        movie.setDuration(120);
        movie.setRating("PG-13");
        movie.setReleaseYear(2024);

        movieRequest = new MovieRequest();
        movieRequest.setTitle("Test Movie");
        movieRequest.setGenre("Action");
        movieRequest.setDuration(120);
        movieRequest.setRating("PG-13");
        movieRequest.setReleaseYear(2024);

        movieResponse = new MovieResponse();
        movieResponse.setId(1L);
        movieResponse.setTitle("Test Movie");
        movieResponse.setGenre("Action");
        movieResponse.setDuration(120);
        movieResponse.setRating("PG-13");
        movieResponse.setReleaseYear(2024);
    }

    @Test
    void createMovie_Success() {
        // Arrange
        when(movieMapper.toEntity(any(MovieRequest.class))).thenReturn(movie);
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);
        when(movieMapper.toResponse(any(Movie.class))).thenReturn(movieResponse);

        // Act
        MovieResponse result = movieService.createMovie(movieRequest);

        // Assert
        assertNotNull(result);
        assertEquals(movieResponse.getId(), result.getId());
        assertEquals(movieResponse.getTitle(), result.getTitle());
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void getMovie_Success() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(movieMapper.toResponse(any(Movie.class))).thenReturn(movieResponse);

        // Act
        MovieResponse result = movieService.getMovie(1L);

        // Assert
        assertNotNull(result);
        assertEquals(movieResponse.getId(), result.getId());
        assertEquals(movieResponse.getTitle(), result.getTitle());
    }

    @Test
    void getMovie_NotFound() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> movieService.getMovie(1L));
        assertEquals(ErrorCode.MOVIE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getAllMovies_Success() {
        // Arrange
        List<Movie> movies = Collections.singletonList(movie);
        List<MovieResponse> movieResponses = Collections.singletonList(movieResponse);

        when(movieRepository.findAll()).thenReturn(movies);
        when(movieMapper.toResponseList(movies)).thenReturn(movieResponses);

        // Act
        List<MovieResponse> results = movieService.getAllMovies();

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(movieResponse.getId(), results.get(0).getId());
    }

    @Test
    void updateMovie_Success() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(movieRepository.save(any(Movie.class))).thenReturn(movie);
        when(movieMapper.toResponse(any(Movie.class))).thenReturn(movieResponse);

        // Act
        MovieResponse result = movieService.updateMovie(1L, movieRequest);

        // Assert
        assertNotNull(result);
        assertEquals(movieResponse.getId(), result.getId());
        assertEquals(movieResponse.getTitle(), result.getTitle());
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void updateMovie_NotFound() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> movieService.updateMovie(1L, movieRequest));
        assertEquals(ErrorCode.MOVIE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteMovie_Success() {
        // Arrange
        when(movieRepository.existsById(1L)).thenReturn(true);

        // Act
        movieService.deleteMovie(1L);

        // Assert
        verify(movieRepository).deleteById(1L);
    }

    @Test
    void deleteMovie_NotFound() {
        // Arrange
        when(movieRepository.existsById(1L)).thenReturn(false);

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> movieService.deleteMovie(1L));
        assertEquals(ErrorCode.MOVIE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void searchMovies_Success() {
        // Arrange
        List<Movie> movies = Collections.singletonList(movie);
        List<MovieResponse> movieResponses = Collections.singletonList(movieResponse);

        when(movieRepository.searchMovies(
                "Test Movie",
                "Action",
                120,
                "PG-13",
                2024
        )).thenReturn(movies);
        when(movieMapper.toResponseList(movies)).thenReturn(movieResponses);

        // Act
        List<MovieResponse> results = movieService.searchMovies(
                "Test Movie",
                "Action",
                120,
                "PG-13",
                2024
        );

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(movieResponse.getId(), results.get(0).getId());
    }

    @Test
    void searchMovies_NoResults() {
        // Arrange
        when(movieRepository.searchMovies(
                "Nonexistent Movie",
                null,
                null,
                null,
                null
        )).thenReturn(List.of());

        // Act
        List<MovieResponse> results = movieService.searchMovies(
                "Nonexistent Movie",
                null,
                null,
                null,
                null
        );

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchMovies_PartialCriteria() {
        // Arrange
        List<Movie> movies = Collections.singletonList(movie);
        List<MovieResponse> movieResponses = Collections.singletonList(movieResponse);

        when(movieRepository.searchMovies(
                "Test",
                null,
                null,
                null,
                2024
        )).thenReturn(movies);
        when(movieMapper.toResponseList(movies)).thenReturn(movieResponses);

        // Act
        List<MovieResponse> results = movieService.searchMovies(
                "Test",
                null,
                null,
                null,
                2024
        );

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(movieResponse.getId(), results.get(0).getId());
    }
}