package guy.shalev.ATnT.Home.assignment.integration.service;

import guy.shalev.ATnT.Home.assignment.exception.exceptions.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.integration.BaseIntegrationTest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.TheaterRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Showtime Integration Tests")
class ShowtimeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private TheaterService theaterService;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private Validator validator;

    private MovieResponse testMovie;
    private TheaterResponse testTheater;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        showtimeRepository.deleteAll();
        movieRepository.deleteAll();
        theaterRepository.deleteAll();

        // Create test movie
        MovieRequest movieRequest = new MovieRequest(
                "Test Movie",
                "Action",
                120, // 2 hours duration
                "PG-13",
                2024
        );
        testMovie = movieService.createMovie(movieRequest);

        // Create test theater
        TheaterRequest theaterRequest = new TheaterRequest(
                "Test Theater",
                100 // capacity
        );
        testTheater = theaterService.createTheater(theaterRequest);

        // Set base date time for tests
        baseDateTime = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
    }

    private void validateRequest(ShowtimeRequest request) throws MethodArgumentNotValidException {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "showtimeRequest");
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            throw new MethodArgumentNotValidException(null, errors);
        }
    }

    private ShowtimeRequest createValidShowtimeRequest() {
        return new ShowtimeRequest(
                testMovie.getId(),
                testTheater.getId(),
                baseDateTime,
                50 // maxSeats
        );
    }

    @Nested
    @DisplayName("Showtime Creation Tests")
    class ShowtimeCreationTests {

        @Test
        @DisplayName("Successfully create a showtime")
        void createShowtimeSuccess() {
            // Given
            ShowtimeRequest request = createValidShowtimeRequest();

            // When
            ShowtimeResponse response = showtimeService.createShowtime(request);

            // Then
            assertNotNull(response.getId());
            assertEquals(testMovie.getId(), response.getMovie().getId());
            assertEquals(testTheater.getId(), response.getTheater().getId());
            assertEquals(baseDateTime, response.getStartTime());
            assertEquals(baseDateTime.plusMinutes(testMovie.getDuration()), response.getEndTime());
            assertEquals(50, response.getMaxSeats());
            assertEquals(50, response.getAvailableSeats());
        }

        @Test
        @DisplayName("Fail creation with overlapping showtime")
        void createOverlappingShowtime() {
            // Given
            ShowtimeRequest request1 = createValidShowtimeRequest();
            showtimeService.createShowtime(request1);

            ShowtimeRequest request2 = createValidShowtimeRequest();
            request2.setStartTime(baseDateTime.plusMinutes(30)); // Overlapping time

            // When & Then
            assertThrows(ConflictException.class,
                    () -> showtimeService.createShowtime(request2));
        }

        @Test
        @DisplayName("Fail creation with invalid theater capacity")
        void createShowtimeExceedingTheaterCapacity() {
            // Given
            ShowtimeRequest request = createValidShowtimeRequest();
            request.setMaxSeats(150); // Exceeds theater capacity of 100

            // When & Then
            assertThrows(BadRequestException.class,
                    () -> showtimeService.createShowtime(request));
        }

        @Test
        @DisplayName("Fail creation with non-existent movie")
        void createShowtimeWithNonExistentMovie() {
            // Given
            ShowtimeRequest request = createValidShowtimeRequest();
            request.setMovieId(999L);

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> showtimeService.createShowtime(request));
        }
    }

    @Nested
    @DisplayName("Showtime Retrieval Tests")
    class ShowtimeRetrievalTests {

        private ShowtimeResponse savedShowtime;

        @BeforeEach
        void setUp() {
            ShowtimeRequest request = createValidShowtimeRequest();
            savedShowtime = showtimeService.createShowtime(request);
        }

        @Test
        @DisplayName("Successfully retrieve showtime by ID")
        void getShowtimeByIdSuccess() {
            // When
            ShowtimeResponse response = showtimeService.getShowtime(savedShowtime.getId());

            // Then
            assertEquals(savedShowtime.getId(), response.getId());
            assertEquals(savedShowtime.getStartTime(), response.getStartTime());
        }

        @Test
        @DisplayName("Successfully retrieve showtimes by movie")
        void getShowtimesByMovie() {
            // When
            List<ShowtimeResponse> showtimes = showtimeService.getShowtimesByMovie(testMovie.getId());

            // Then
            assertEquals(1, showtimes.size());
            assertEquals(savedShowtime.getId(), showtimes.get(0).getId());
        }

        @Test
        @DisplayName("Successfully retrieve showtimes by theater")
        void getShowtimesByTheater() {
            // When
            List<ShowtimeResponse> showtimes = showtimeService.getShowtimesByTheater(testTheater.getId());

            // Then
            assertEquals(1, showtimes.size());
            assertEquals(savedShowtime.getId(), showtimes.get(0).getId());
        }
    }

    @Nested
    @DisplayName("Showtime Update Tests")
    class ShowtimeUpdateTests {

        private ShowtimeResponse savedShowtime;

        @BeforeEach
        void setUp() {
            ShowtimeRequest request = createValidShowtimeRequest();
            savedShowtime = showtimeService.createShowtime(request);
        }

        @Test
        @DisplayName("Successfully update showtime")
        void updateShowtimeSuccess() {
            // Given
            ShowtimeRequest updateRequest = createValidShowtimeRequest();
            updateRequest.setStartTime(baseDateTime.plusHours(1));
            updateRequest.setMaxSeats(75);

            // When
            ShowtimeResponse updated = showtimeService.updateShowtime(savedShowtime.getId(), updateRequest);

            // Then
            assertEquals(baseDateTime.plusHours(1), updated.getStartTime());
            assertEquals(75, updated.getMaxSeats());
            assertEquals(75, updated.getAvailableSeats());
        }

        @Test
        @DisplayName("Fail update with overlapping showtime")
        void updateToOverlappingTime() {
            // Given
            // Create another showtime
            ShowtimeRequest anotherRequest = createValidShowtimeRequest();
            anotherRequest.setStartTime(baseDateTime.plusHours(4));
            showtimeService.createShowtime(anotherRequest);

            // Try to update first showtime to overlap with second
            ShowtimeRequest updateRequest = createValidShowtimeRequest();
            updateRequest.setStartTime(baseDateTime.plusHours(4).plusMinutes(30));

            // When & Then
            assertThrows(ConflictException.class,
                    () -> showtimeService.updateShowtime(savedShowtime.getId(), updateRequest));
        }
    }

    @Nested
    @DisplayName("Showtime Deletion Tests")
    class ShowtimeDeletionTests {

        private ShowtimeResponse savedShowtime;

        @BeforeEach
        void setUp() {
            ShowtimeRequest request = createValidShowtimeRequest();
            savedShowtime = showtimeService.createShowtime(request);
        }

        @Test
        @DisplayName("Successfully delete showtime")
        void deleteShowtimeSuccess() {
            // When
            showtimeService.deleteShowtime(savedShowtime.getId());

            // Then
            assertFalse(showtimeRepository.existsById(savedShowtime.getId()));
        }

        @Test
        @DisplayName("Fail to delete non-existent showtime")
        void deleteNonExistentShowtime() {
            // When & Then
            assertThrows(NotFoundException.class,
                    () -> showtimeService.deleteShowtime(999L));
        }
    }

    @Nested
    @DisplayName("Showtime Availability Tests")
    class ShowtimeAvailabilityTests {

        @Test
        @DisplayName("Successfully check available time slot")
        void checkAvailableTimeSlot() {
            // Given
            LocalDateTime startTime = baseDateTime.plusHours(2);
            LocalDateTime endTime = startTime.plusHours(2);

            // When
            boolean isAvailable = showtimeService.isShowtimeAvailable(
                    testTheater.getId(),
                    startTime,
                    endTime
            );

            // Then
            assertTrue(isAvailable);
        }

        @Test
        @DisplayName("Successfully detect unavailable time slot")
        void checkUnavailableTimeSlot() {
            // Given
            ShowtimeRequest request = createValidShowtimeRequest();
            showtimeService.createShowtime(request);

            // When
            boolean isAvailable = showtimeService.isShowtimeAvailable(
                    testTheater.getId(),
                    baseDateTime.plusMinutes(30),
                    baseDateTime.plusMinutes(150)
            );

            // Then
            assertFalse(isAvailable);
        }
    }
}