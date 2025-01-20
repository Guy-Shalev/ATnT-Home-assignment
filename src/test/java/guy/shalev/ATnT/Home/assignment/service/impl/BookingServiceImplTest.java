package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.ErrorCode;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.mapper.BookingMapper;
import guy.shalev.ATnT.Home.assignment.model.dto.request.BookingRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.SeatRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.BookingResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.*;
import guy.shalev.ATnT.Home.assignment.model.enums.BookingStatus;
import guy.shalev.ATnT.Home.assignment.model.enums.UserRole;
import guy.shalev.ATnT.Home.assignment.repository.BookingRepository;
import guy.shalev.ATnT.Home.assignment.repository.ShowtimeRepository;
import guy.shalev.ATnT.Home.assignment.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User user;
    private Movie movie;
    private Theater theater;
    private Showtime showtime;
    private Booking booking;
    private BookingRequest bookingRequest;
    private BookingResponse bookingResponse;
    private final String username = "testUser";

    @BeforeEach
    void setUp() {
        // Set ticket price using reflection since it's normally set via @Value
        ReflectionTestUtils.setField(bookingService, "ticketPrice", new BigDecimal("10.00"));

        // Initialize test data
        user = User.builder()
                .id(1L)
                .username(username)
                .email("test@test.com")
                .role(UserRole.CUSTOMER)
                .build();

        movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Test Movie");
        movie.setDuration(120);

        theater = new Theater();
        theater.setId(1L);
        theater.setName("Test Theater");
        theater.setCapacity(100);

        showtime = Showtime.builder()
                .id(1L)
                .movie(movie)
                .theater(theater)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .maxSeats(50)
                .availableSeats(50)
                .build();

        booking = Booking.builder()
                .id(1L)
                .user(user)
                .showtime(showtime)
                .seatNumber(1)
                .price(new BigDecimal("10.00"))
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.CONFIRMED)
                .build();

        SeatRequest seatRequest = new SeatRequest();
        seatRequest.setSeatNumber(1);

        bookingRequest = new BookingRequest();
        bookingRequest.setShowtimeId(1L);
        bookingRequest.setSeats(List.of(seatRequest));

        bookingResponse = new BookingResponse();
        bookingResponse.setId(1L);
        bookingResponse.setSeatNumber(1);
        bookingResponse.setStatus(BookingStatus.CONFIRMED);
    }

    @Test
    void createBooking_Success() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(showtimeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(showtime));
        when(bookingRepository.findByShowtimeAndSeatNumber(any(), eq(1)))
                .thenReturn(Optional.empty());
        when(bookingRepository.saveAll(any())).thenReturn(List.of(booking));
        when(bookingMapper.toResponseList(any())).thenReturn(List.of(bookingResponse));

        // Act
        List<BookingResponse> result = bookingService.createBooking(username, bookingRequest);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(BookingStatus.CONFIRMED, result.get(0).getStatus());
        verify(showtimeRepository).save(any(Showtime.class));
    }

    @Test
    void createBooking_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.createBooking(username, bookingRequest));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createBooking_ShowtimeNotFound() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(showtimeRepository.findByIdWithLock(1L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.createBooking(username, bookingRequest));
        assertEquals(ErrorCode.SHOWTIME_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createBooking_InsufficientSeats() {
        // Arrange
        showtime.setAvailableSeats(0);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(showtimeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(showtime));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> bookingService.createBooking(username, bookingRequest));
        assertEquals(ErrorCode.INSUFFICIENT_SEATS, exception.getErrorCode());
    }

    @Test
    void createBooking_SeatAlreadyBooked() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(showtimeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(showtime));
        when(bookingRepository.findByShowtimeAndSeatNumber(any(), eq(1)))
                .thenReturn(Optional.of(booking));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> bookingService.createBooking(username, bookingRequest));
        assertEquals(ErrorCode.SEAT_ALREADY_BOOKED, exception.getErrorCode());
    }

    @Test
    void createBooking_InvalidSeatNumber() {
        // Arrange
        SeatRequest invalidSeat = new SeatRequest();
        invalidSeat.setSeatNumber(51); // Max seats is 50
        bookingRequest.setSeats(List.of(invalidSeat));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(showtimeRepository.findByIdWithLock(1L)).thenReturn(Optional.of(showtime));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(username, bookingRequest));
        assertEquals(ErrorCode.INVALID_SEAT_NUMBER, exception.getErrorCode());
    }

    @Test
    void getBooking_Success() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(bookingResponse);

        // Act
        BookingResponse result = bookingService.getBooking(1L);

        // Assert
        assertNotNull(result);
        assertEquals(bookingResponse.getId(), result.getId());
    }

    @Test
    void getBooking_NotFound() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.getBooking(1L));
        assertEquals(ErrorCode.BOOKING_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getUserBookings_Success() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(bookingRepository.findByUser(user)).thenReturn(List.of(booking));
        when(bookingMapper.toResponseList(any())).thenReturn(List.of(bookingResponse));

        // Act
        List<BookingResponse> results = bookingService.getUserBookings(username);

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    void getUserBookings_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> bookingService.getUserBookings(username));
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void isSeatAvailable_Available() {
        // Arrange
        when(bookingRepository.findByShowtimeAndSeatNumber(any(Showtime.class), eq(1)))
                .thenReturn(Optional.empty());

        // Act
        boolean result = bookingService.isSeatAvailable(1L, 1);

        // Assert
        assertTrue(result);
    }

    @Test
    void isSeatAvailable_NotAvailable() {
        // Arrange
        when(bookingRepository.findByShowtimeAndSeatNumber(any(Showtime.class), eq(1)))
                .thenReturn(Optional.of(booking));

        // Act
        boolean result = bookingService.isSeatAvailable(1L, 1);

        // Assert
        assertFalse(result);
    }
}