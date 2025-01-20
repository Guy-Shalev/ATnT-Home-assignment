package guy.shalev.ATnT.Home.assignment.integration;

import guy.shalev.ATnT.Home.assignment.exception.exceptions.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.TheaterRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import guy.shalev.ATnT.Home.assignment.repository.MovieRepository;
import guy.shalev.ATnT.Home.assignment.repository.ShowtimeRepository;
import guy.shalev.ATnT.Home.assignment.repository.TheaterRepository;
import guy.shalev.ATnT.Home.assignment.service.MovieService;
import guy.shalev.ATnT.Home.assignment.service.ShowtimeService;
import guy.shalev.ATnT.Home.assignment.service.TheaterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Theater Integration Tests")
class TheaterIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TheaterService theaterService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private Validator validator;

    @BeforeEach
    void setUp() {
        // Clean up in correct order due to foreign key constraints
        showtimeRepository.deleteAll();
        movieRepository.deleteAll();
        theaterRepository.deleteAll();
    }

    private TheaterRequest createValidTheaterRequest() {
        return new TheaterRequest(
                "Test Theater",
                100 // capacity
        );
    }

    private void validateRequest(TheaterRequest request) throws MethodArgumentNotValidException {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "theaterRequest");
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            throw new MethodArgumentNotValidException(null, errors);
        }
    }

    @Nested
    @DisplayName("Theater Creation Tests")
    class TheaterCreationTests {

        @Test
        @DisplayName("Successfully create a theater")
        void createTheaterSuccess() {
            // Given
            TheaterRequest request = createValidTheaterRequest();

            // When
            TheaterResponse response = theaterService.createTheater(request);

            // Then
            assertNotNull(response.getId());
            assertEquals(request.getName(), response.getName());
            assertEquals(request.getCapacity(), response.getCapacity());

            // Verify persistence
            assertTrue(theaterRepository.findById(response.getId()).isPresent());
        }

        @Test
        @DisplayName("Fail creation with duplicate name")
        void createDuplicateTheaterName() {
            // Given
            TheaterRequest request1 = createValidTheaterRequest();
            theaterService.createTheater(request1);

            TheaterRequest request2 = new TheaterRequest(
                    "Test Theater", // Same name
                    150
            );

            // When & Then
            assertThrows(ConflictException.class,
                    () -> theaterService.createTheater(request2));
        }

        @Test
        @DisplayName("Fail creation with invalid capacity")
        void createTheaterWithInvalidCapacity() {
            // Given
            TheaterRequest request = new TheaterRequest(
                    "Test Theater",
                    0 // Invalid capacity
            );

            // When & Then
            MethodArgumentNotValidException exception = assertThrows(
                    MethodArgumentNotValidException.class,
                    () -> validateRequest(request)
            );

            assertTrue(exception.getBindingResult().getFieldErrors("capacity").stream()
                    .anyMatch(error -> error.getDefaultMessage().contains("must be positive")));
        }
    }

    @Nested
    @DisplayName("Theater Update Tests")
    class TheaterUpdateTests {

        private TheaterResponse savedTheater;

        @BeforeEach
        void setUp() {
            TheaterRequest request = createValidTheaterRequest();
            savedTheater = theaterService.createTheater(request);
        }

        @Test
        @DisplayName("Successfully update a theater")
        void updateTheaterSuccess() {
            // Given
            TheaterRequest updateRequest = new TheaterRequest(
                    "Updated Theater",
                    150
            );

            // When
            TheaterResponse updated = theaterService.updateTheater(savedTheater.getId(), updateRequest);

            // Then
            assertEquals("Updated Theater", updated.getName());
            assertEquals(150, updated.getCapacity());
            assertEquals(savedTheater.getId(), updated.getId());
        }

        @Test
        @DisplayName("Fail update with duplicate name")
        void updateTheaterDuplicateName() {
            // Given
            TheaterRequest anotherTheater = new TheaterRequest(
                    "Another Theater",
                    100
            );
            theaterService.createTheater(anotherTheater);

            TheaterRequest updateRequest = new TheaterRequest(
                    "Another Theater", // Duplicate name
                    150
            );

            // When & Then
            assertThrows(ConflictException.class,
                    () -> theaterService.updateTheater(savedTheater.getId(), updateRequest));
        }

        @Test
        @DisplayName("Fail update of non-existent theater")
        void updateNonExistentTheater() {
            // Given
            TheaterRequest updateRequest = createValidTheaterRequest();

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> theaterService.updateTheater(999L, updateRequest));
        }
    }

    @Nested
    @DisplayName("Theater Deletion Tests")
    class TheaterDeletionTests {

        private TheaterResponse savedTheater;

        @BeforeEach
        void setUp() {
            TheaterRequest request = createValidTheaterRequest();
            savedTheater = theaterService.createTheater(request);
        }

        @Test
        @DisplayName("Successfully delete a theater")
        void deleteTheaterSuccess() {
            // When
            theaterService.deleteTheater(savedTheater.getId());

            // Then
            assertFalse(theaterRepository.existsById(savedTheater.getId()));
        }

        @Test
        @DisplayName("Fail deletion of theater with scheduled showtimes")
        void deleteTheaterWithShowtimes() {
            // Given
            // Create a movie and showtime for this theater
            MovieRequest movieRequest = new MovieRequest(
                    "Test Movie",
                    "Action",
                    120,
                    "PG-13",
                    2024
            );
            MovieResponse movie = movieService.createMovie(movieRequest);

            ShowtimeRequest showtimeRequest = new ShowtimeRequest(
                    movie.getId(),
                    savedTheater.getId(),
                    LocalDateTime.now().plusDays(1),
                    50
            );
            showtimeService.createShowtime(showtimeRequest);

            // When & Then
            assertThrows(BadRequestException.class,
                    () -> theaterService.deleteTheater(savedTheater.getId()));
        }

        @Test
        @DisplayName("Fail deletion of non-existent theater")
        void deleteNonExistentTheater() {
            // When & Then
            assertThrows(NotFoundException.class,
                    () -> theaterService.deleteTheater(999L));
        }
    }

    @Nested
    @DisplayName("Theater Retrieval Tests")
    class TheaterRetrievalTests {

        @Test
        @DisplayName("Successfully retrieve all theaters")
        void getAllTheatersSuccess() {
            // Given
            TheaterRequest request1 = new TheaterRequest("Theater 1", 100);
            TheaterRequest request2 = new TheaterRequest("Theater 2", 150);
            theaterService.createTheater(request1);
            theaterService.createTheater(request2);

            // When
            var theaters = theaterService.getAllTheaters();

            // Then
            assertEquals(2, theaters.size());
            assertTrue(theaters.stream()
                    .anyMatch(theater -> theater.getName().equals("Theater 1")));
            assertTrue(theaters.stream()
                    .anyMatch(theater -> theater.getName().equals("Theater 2")));
        }

        @Test
        @DisplayName("Successfully retrieve theater by ID")
        void getTheaterByIdSuccess() {
            // Given
            TheaterResponse savedTheater = theaterService.createTheater(createValidTheaterRequest());

            // When
            TheaterResponse retrieved = theaterService.getTheater(savedTheater.getId());

            // Then
            assertEquals(savedTheater.getId(), retrieved.getId());
            assertEquals(savedTheater.getName(), retrieved.getName());
            assertEquals(savedTheater.getCapacity(), retrieved.getCapacity());
        }

        @Test
        @DisplayName("Fail to retrieve non-existent theater")
        void getTheaterByIdNotFound() {
            assertThrows(NotFoundException.class,
                    () -> theaterService.getTheater(999L));
        }
    }
}