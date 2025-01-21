package guy.shalev.ATnT.Home.assignment.integration.service;

import guy.shalev.ATnT.Home.assignment.exception.exceptions.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.integration.BaseIntegrationTest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.*;
import guy.shalev.ATnT.Home.assignment.model.dto.response.BookingResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.BookingRepository;
import guy.shalev.ATnT.Home.assignment.repository.MovieRepository;
import guy.shalev.ATnT.Home.assignment.repository.ShowtimeRepository;
import guy.shalev.ATnT.Home.assignment.repository.TheaterRepository;
import guy.shalev.ATnT.Home.assignment.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Booking Integration Tests")
class BookingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserService userService;

    @Autowired
    private MovieService movieService;

    @Autowired
    private TheaterService theaterService;

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Validator validator;

    private MovieResponse testMovie;
    private TheaterResponse testTheater;
    private ShowtimeResponse testShowtime;
    private String testUsername;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        // Clean up all data in correct order due to foreign key constraints
        bookingRepository.deleteAll();
        showtimeRepository.deleteAll();
        movieRepository.deleteAll();
        theaterRepository.deleteAll();

        // Clean up users
        jdbcTemplate.execute("DELETE FROM authorities");
        jdbcTemplate.execute("DELETE FROM users");
        jdbcTemplate.execute("DELETE FROM app_users");

        // Create test user
        testUsername = "testuser";
        UserRequest userRequest = UserRequest.builder()
                .username(testUsername)
                .password("password123")
                .email("test@example.com")
                .role(UserRole.CUSTOMER)
                .build();
        userService.registerUser(userRequest);

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

        // Set base datetime and create test showtime
        baseDateTime = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0);
        ShowtimeRequest showtimeRequest = new ShowtimeRequest(
                testMovie.getId(),
                testTheater.getId(),
                baseDateTime,
                50 // maxSeats
        );
        testShowtime = showtimeService.createShowtime(showtimeRequest);
    }

    private BookingRequest createValidBookingRequest() {
        return new BookingRequest(
                testShowtime.getId(),
                Arrays.asList(new SeatRequest(1), new SeatRequest(2))
        );
    }

    private void validateRequest(BookingRequest request) throws MethodArgumentNotValidException {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(request, "bookingRequest");
        validator.validate(request, errors);
        if (errors.hasErrors()) {
            throw new MethodArgumentNotValidException(null, errors);
        }
    }

    @Nested
    @DisplayName("Booking Creation Tests")
    class BookingCreationTests {

        @Test
        @DisplayName("Successfully create a booking")
        void createBookingSuccess() {
            // Given
            BookingRequest request = createValidBookingRequest();

            // When
            List<BookingResponse> responses = bookingService.createBooking(testUsername, request);

            // Then
            assertEquals(2, responses.size());
            BookingResponse firstBooking = responses.get(0);
            assertNotNull(firstBooking.getId());
            assertEquals(testShowtime.getId(), firstBooking.getShowtime().getId());
            assertEquals(1, firstBooking.getSeatNumber());

            // Verify available seats were updated
            ShowtimeResponse updatedShowtime = showtimeService.getShowtime(testShowtime.getId());
            assertEquals(testShowtime.getAvailableSeats() - 2, updatedShowtime.getAvailableSeats());
        }

        @Test
        @DisplayName("Fail booking with duplicate seats")
        void createBookingDuplicateSeats() {
            // Given
            BookingRequest request = new BookingRequest(
                    testShowtime.getId(),
                    Arrays.asList(new SeatRequest(1), new SeatRequest(1))
            );

            // When & Then
            assertThrows(ConflictException.class,
                    () -> bookingService.createBooking(testUsername, request));
        }

        @Test
        @DisplayName("Fail booking with invalid seat number")
        void createBookingInvalidSeatNumber() {
            // Given
            BookingRequest request = new BookingRequest(
                    testShowtime.getId(),
                    List.of(new SeatRequest(51)) // Exceeds max seats (50)
            );

            // When & Then
            assertThrows(BadRequestException.class,
                    () -> bookingService.createBooking(testUsername, request));
        }

        @Test
        @DisplayName("Fail booking when no seats available")
        void createBookingNoSeatsAvailable() {
            // Given
            // Book all seats first
            List<SeatRequest> allSeats = IntStream.rangeClosed(1, 50)
                    .mapToObj(SeatRequest::new)
                    .collect(Collectors.toList());
            BookingRequest allSeatsRequest = new BookingRequest(testShowtime.getId(), allSeats);
            bookingService.createBooking(testUsername, allSeatsRequest);

            // Try to book another seat
            BookingRequest newRequest = new BookingRequest(
                    testShowtime.getId(),
                    List.of(new SeatRequest(1))
            );

            // When & Then
            assertThrows(ConflictException.class,
                    () -> bookingService.createBooking(testUsername, newRequest));
        }

        @Test
        @DisplayName("Prevent same seat being booked by different users")
        void preventDuplicateBookingByDifferentUsers() {
            // Given
            // Create second user
            String secondUsername = "testuser2";
            UserRequest secondUserRequest = UserRequest.builder()
                    .username(secondUsername)
                    .password("password123")
                    .email("test2@example.com")
                    .role(UserRole.CUSTOMER)
                    .build();
            userService.registerUser(secondUserRequest);

            // First user books seat 1
            BookingRequest firstBooking = new BookingRequest(
                    testShowtime.getId(),
                    List.of(new SeatRequest(1))
            );
            bookingService.createBooking(testUsername, firstBooking);

            // When - Second user tries to book the same seat
            BookingRequest secondBooking = new BookingRequest(
                    testShowtime.getId(),
                    List.of(new SeatRequest(1))
            );

            // Then
            assertThrows(ConflictException.class,
                    () -> bookingService.createBooking(secondUsername, secondBooking));

            // Verify the seat is still marked as booked
            assertFalse(bookingService.isSeatAvailable(testShowtime.getId(), 1));
        }
    }

    @Nested
    @DisplayName("Booking Retrieval Tests")
    class BookingRetrievalTests {

        private List<BookingResponse> savedBookings;

        @BeforeEach
        void setUp() {
            BookingRequest request = createValidBookingRequest();
            savedBookings = bookingService.createBooking(testUsername, request);
        }

        @Test
        @DisplayName("Successfully retrieve booking by ID")
        void getBookingByIdSuccess() {
            // When
            BookingResponse response = bookingService.getBooking(savedBookings.get(0).getId());

            // Then
            assertNotNull(response);
            assertEquals(savedBookings.get(0).getId(), response.getId());
            assertEquals(testShowtime.getId(), response.getShowtime().getId());
        }

        @Test
        @DisplayName("Successfully retrieve user bookings")
        void getUserBookingsSuccess() {
            // When
            List<BookingResponse> userBookings = bookingService.getUserBookings(testUsername);

            // Then
            assertEquals(2, userBookings.size());
            assertTrue(userBookings.stream()
                    .allMatch(booking -> booking.getShowtime().getId().equals(testShowtime.getId())));
        }

        @Test
        @DisplayName("Verify seat availability")
        void checkSeatAvailability() {
            // When & Then
            assertFalse(bookingService.isSeatAvailable(testShowtime.getId(), 1));
            assertTrue(bookingService.isSeatAvailable(testShowtime.getId(), 3));
        }
    }

    @Nested
    @DisplayName("Concurrent Booking Tests")
    class ConcurrentBookingTests {

        @Test
        @DisplayName("Handle concurrent bookings for same seat")
        void handleConcurrentBookings() throws InterruptedException {
            // Given
            int numberOfThreads = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch latch = new CountDownLatch(numberOfThreads);

            // Create all users first, before any concurrent operations
            for (int i = 0; i < numberOfThreads; i++) {
                UserRequest userRequest = UserRequest.builder()
                        .username("concurrent_user" + i)
                        .password("password123")
                        .email("concurrent_user" + i + "@example.com")
                        .role(UserRole.CUSTOMER)
                        .build();
                userService.registerUser(userRequest);
            }

            // Track successful and failed bookings
            final int[] successfulBookings = {0};
            final int[] failedBookings = {0};

            // When - Run concurrent bookings
            for (int i = 0; i < numberOfThreads; i++) {
                final int index = i;
                executorService.submit(() -> {
                    try {
                        // Create new booking request for each attempt
                        BookingRequest request = new BookingRequest(
                                testShowtime.getId(),
                                List.of(new SeatRequest(1))
                        );

                        bookingService.createBooking("concurrent_user" + index, request);
                        successfulBookings[0]++;
                    } catch (ConflictException e) {
                        // Expected for concurrent bookings
                        failedBookings[0]++;
                    } catch (Exception e) {
                        System.err.println("Unexpected error: " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            latch.await();
            executorService.shutdown();

            // Then
            assertEquals(1, successfulBookings[0], "Only one booking should succeed");
            assertEquals(numberOfThreads - 1, failedBookings[0], "All other bookings should fail");
            assertFalse(bookingService.isSeatAvailable(testShowtime.getId(), 1),
                    "Seat should be marked as unavailable");
        }

        @Test
        @DisplayName("Prevent concurrent booking of same seat by different users")
        void preventConcurrentBookingBySameUsers() throws InterruptedException {
            // Given
            int numberOfThreads = 10;  // More threads to increase chance of concurrent access
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
            CountDownLatch readyLatch = new CountDownLatch(numberOfThreads); // For synchronizing start
            CountDownLatch startLatch = new CountDownLatch(1); // For simultaneous start
            CountDownLatch completionLatch = new CountDownLatch(numberOfThreads);

            // Create users first
            for (int i = 0; i < numberOfThreads; i++) {
                UserRequest userRequest = UserRequest.builder()
                        .username("concurrent_user" + i)
                        .password("password123")
                        .email("concurrent_user" + i + "@example.com")
                        .role(UserRole.CUSTOMER)
                        .build();
                userService.registerUser(userRequest);
            }

            // Track results
            AtomicInteger successfulBookings = new AtomicInteger(0);
            AtomicInteger failedBookings = new AtomicInteger(0);
            Set<String> successfulUsers = ConcurrentHashMap.newKeySet();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // When - Submit booking attempts for all users
            for (int i = 0; i < numberOfThreads; i++) {
                final int userIndex = i;
                executorService.submit(() -> {
                    try {
                        String username = "concurrent_user" + userIndex;
                        BookingRequest request = new BookingRequest(
                                testShowtime.getId(),
                                List.of(new SeatRequest(1)) // All trying to book the same seat
                        );

                        // Signal ready and wait for simultaneous start
                        readyLatch.countDown();
                        startLatch.await(); // Wait for all threads to be ready

                        // Try to book
                        List<BookingResponse> response = bookingService.createBooking(username, request);
                        successfulBookings.incrementAndGet();
                        successfulUsers.add(username);
                    } catch (ConflictException e) {
                        failedBookings.incrementAndGet();
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }

            // Wait for all threads to be ready
            readyLatch.await();
            // Start all threads simultaneously
            startLatch.countDown();
            // Wait for completion
            completionLatch.await(10, TimeUnit.SECONDS);
            executorService.shutdown();

            // Then
            assertTrue(exceptions.isEmpty(),
                    "Unexpected exceptions: " + exceptions.stream()
                            .map(Exception::getMessage)
                            .collect(Collectors.joining(", ")));
            assertEquals(1, successfulBookings.get(),
                    "Only one booking should succeed");
            assertEquals(numberOfThreads - 1, failedBookings.get(),
                    "All other bookings should fail");
            assertEquals(1, successfulUsers.size(),
                    "Only one user should succeed");
            assertFalse(bookingService.isSeatAvailable(testShowtime.getId(), 1),
                    "Seat should be marked as unavailable");

            // Verify the successful booking was properly saved
            List<BookingResponse> allBookings = bookingService.getUserBookings(successfulUsers.iterator().next());
            assertEquals(1, allBookings.size(),
                    "Should have exactly one booking for the successful user");
            assertEquals(1, allBookings.get(0).getSeatNumber(),
                    "Should have booked seat number 1");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Fail with non-existent showtime")
        void bookingNonExistentShowtime() {
            // Given
            BookingRequest request = new BookingRequest(
                    999L,
                    List.of(new SeatRequest(1))
            );

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> bookingService.createBooking(testUsername, request));
        }

        @Test
        @DisplayName("Fail with non-existent user")
        void bookingNonExistentUser() {
            // Given
            BookingRequest request = createValidBookingRequest();

            // When & Then
            assertThrows(NotFoundException.class,
                    () -> bookingService.createBooking("nonexistentuser", request));
        }

        @Test
        @DisplayName("Fail with null seat numbers")
        void bookingNullSeatNumbers() {
            // Given
            BookingRequest request = new BookingRequest(
                    testShowtime.getId(),
                    List.of(new SeatRequest(null))
            );

            // When & Then
            MethodArgumentNotValidException exception = assertThrows(
                    MethodArgumentNotValidException.class,
                    () -> validateRequest(request)
            );

            assertTrue(exception.getBindingResult().getFieldErrors()
                    .stream()
                    .anyMatch(error -> error.getField().contains("seatNumber")));
        }
    }
}