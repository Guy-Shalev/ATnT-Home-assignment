package guy.shalev.ATnT.Home.assignment.integration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import guy.shalev.ATnT.Home.assignment.integration.BaseIntegrationTest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.*;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.BookingRepository;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("Booking Controller Integration Tests")
class BookingControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MovieService movieService;

    @Autowired
    private TheaterService theaterService;

    @Autowired
    private ShowtimeService showtimeService;

    @Autowired
    private UserService userService;

    @Autowired
    private BookingRepository bookingRepository;

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
    private ShowtimeResponse testShowtime;
    private LocalDateTime baseDateTime;

    @BeforeEach
    void setUp() {
        // Clean up the database
        bookingRepository.deleteAll();
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

    @Nested
    @DisplayName("Booking Creation Endpoint Tests")
    class BookingCreationTests {

        @Test
        @DisplayName("Successfully create booking as admin")
        void createBookingAsAdmin() throws Exception {
            // Given
            BookingRequest request = createValidBookingRequest();

            // When
            ResultActions result = mockMvc.perform(post("/api/bookings")
                    .header("Authorization", adminAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].showtime.id").value(testShowtime.getId()))
                    .andExpect(jsonPath("$[0].seatNumber").value(1))
                    .andExpect(jsonPath("$[1].seatNumber").value(2))
                    .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                    .andExpect(jsonPath("$[1].status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("Successfully create booking as customer")
        void createBookingAsCustomer() throws Exception {
            // Given
            BookingRequest request = createValidBookingRequest();

            // When
            ResultActions result = mockMvc.perform(post("/api/bookings")
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].showtime.id").value(testShowtime.getId()))
                    .andExpect(jsonPath("$[0].seatNumber").value(1))
                    .andExpect(jsonPath("$[1].seatNumber").value(2));
        }

        @Test
        @DisplayName("Fail to create booking with duplicate seats")
        void createBookingWithDuplicateSeats() throws Exception {
            // Given
            BookingRequest request = new BookingRequest(
                    testShowtime.getId(),
                    Arrays.asList(new SeatRequest(1), new SeatRequest(1))
            );

            // When
            ResultActions result = mockMvc.perform(post("/api/bookings")
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Cannot book the same seat twice: 1"));
        }

        @Test
        @DisplayName("Fail to create booking for already booked seat")
        void createBookingForAlreadyBookedSeat() throws Exception {
            // Given
            // First booking
            BookingRequest firstRequest = new BookingRequest(
                    testShowtime.getId(),
                    List.of(new SeatRequest(1))
            );
            mockMvc.perform(post("/api/bookings")
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest)));

            // Second booking for same seat
            BookingRequest secondRequest = new BookingRequest(
                    testShowtime.getId(),
                    List.of(new SeatRequest(1))
            );

            // When
            ResultActions result = mockMvc.perform(post("/api/bookings")
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondRequest)));

            // Then
            result.andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Seat 1 is already booked"));
        }

        @Test
        @DisplayName("Fail to create booking with invalid seat number")
        void createBookingWithInvalidSeatNumber() throws Exception {
            // Given
            BookingRequest request = new BookingRequest(
                    testShowtime.getId(),
                    List.of(new SeatRequest(51)) // Exceeds max seats (50)
            );

            // When
            ResultActions result = mockMvc.perform(post("/api/bookings")
                    .header("Authorization", customerAuthHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Invalid seat number 51. Maximum seat number is: 50"));
        }
    }

    @Nested
    @DisplayName("Booking Retrieval Endpoint Tests")
    class BookingRetrievalTests {

        @BeforeEach
        void setUp() throws Exception {
            // Create a booking first
            BookingRequest request = createValidBookingRequest();
            mockMvc.perform(post("/api/bookings")
                            .header("Authorization", customerAuthHeader)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Successfully get booking by ID")
        void getBookingById() throws Exception {
            // First get all user's bookings
            String response = mockMvc.perform(get("/api/bookings/user")
                            .header("Authorization", customerAuthHeader))
                    .andReturn().getResponse().getContentAsString();
            Long bookingId = objectMapper.readTree(response).get(0).get("id").asLong();

            // When
            ResultActions result = mockMvc.perform(get("/api/bookings/{id}", bookingId)
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.seatNumber").value(1))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"))
                    .andExpect(jsonPath("$.showtime.id").value(testShowtime.getId()));
        }

        @Test
        @DisplayName("Successfully get user bookings")
        void getUserBookings() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/bookings/user")
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].seatNumber").value(1))
                    .andExpect(jsonPath("$[1].seatNumber").value(2))
                    .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
                    .andExpect(jsonPath("$[1].status").value("CONFIRMED"))
                    .andExpect(jsonPath("$[0].showtime.id").value(testShowtime.getId()))
                    .andExpect(jsonPath("$[1].showtime.id").value(testShowtime.getId()));
        }

        @Test
        @DisplayName("Successfully check seat availability")
        void checkSeatAvailability() throws Exception {
            // When - Check booked seat
            ResultActions resultBooked = mockMvc.perform(get("/api/bookings/seat-available")
                    .param("showtimeId", testShowtime.getId().toString())
                    .param("seatNumber", "1")
                    .header("Authorization", customerAuthHeader));  // Added authentication

            // Then
            resultBooked.andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(false));

            // When - Check available seat
            ResultActions resultAvailable = mockMvc.perform(get("/api/bookings/seat-available")
                    .param("showtimeId", testShowtime.getId().toString())
                    .param("seatNumber", "3")
                    .header("Authorization", customerAuthHeader));  // Added authentication

            // Then
            resultAvailable.andExpect(status().isOk())
                    .andExpect(jsonPath("$").value(true));
        }

        @Test
        @DisplayName("Fail to get non-existent booking")
        void getNonExistentBooking() throws Exception {
            // When
            ResultActions result = mockMvc.perform(get("/api/bookings/{id}", 999L)
                    .header("Authorization", customerAuthHeader));

            // Then
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Booking not found with id: 999"));
        }

        @Test
        @DisplayName("Fail to access another user's booking")
        void getAnotherUserBooking() throws Exception {
            // Given - Create another customer
            UserRequest anotherCustomerRequest = UserRequest.builder()
                    .username("customer2")
                    .password("password123")
                    .email("customer2@test.com")
                    .role(UserRole.CUSTOMER)
                    .build();
            userService.registerUser(anotherCustomerRequest);
            String anotherCustomerAuth = "Basic " + Base64.getEncoder()
                    .encodeToString("customer2:password123".getBytes());

            // Get a booking ID from the first customer's bookings
            String response = mockMvc.perform(get("/api/bookings/user")
                            .header("Authorization", customerAuthHeader))
                    .andReturn().getResponse().getContentAsString();
            Long firstBookingId = objectMapper.readTree(response).get(0).get("id").asLong();

            // When - Try to access first customer's booking with second customer
            ResultActions result = mockMvc.perform(get("/api/bookings/{id}", firstBookingId)
                    .header("Authorization", anotherCustomerAuth));

            // Then
            result.andExpect(status().isForbidden());
        }
    }
}