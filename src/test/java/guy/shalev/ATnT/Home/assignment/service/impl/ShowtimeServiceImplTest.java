package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.ErrorCode;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.mapper.ShowtimeMapper;
import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.*;
import guy.shalev.ATnT.Home.assignment.model.enums.BookingStatus;
import guy.shalev.ATnT.Home.assignment.repository.MovieRepository;
import guy.shalev.ATnT.Home.assignment.repository.ShowtimeRepository;
import guy.shalev.ATnT.Home.assignment.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowtimeServiceImplTest {
    @Mock
    private ShowtimeRepository showtimeRepository;

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private TheaterRepository theaterRepository;

    @Mock
    private ShowtimeMapper showtimeMapper;

    @InjectMocks
    private ShowtimeServiceImpl showtimeService;

    private Movie movie;
    private Theater theater;
    private Showtime showtime;
    private ShowtimeRequest request;
    private ShowtimeResponse response;
    private LocalDateTime startTime;

    @BeforeEach
    void setUp() {
        // Initialize test data
        movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Test Movie");
        movie.setDuration(120); // 2 hours

        theater = new Theater();
        theater.setId(1L);
        theater.setName("Test Theater");
        theater.setCapacity(100);

        startTime = LocalDateTime.now().plusDays(1);

        request = new ShowtimeRequest();
        request.setMovieId(1L);
        request.setTheaterId(1L);
        request.setStartTime(startTime);
        request.setMaxSeats(50);

        showtime = new Showtime();
        showtime.setId(1L);
        showtime.setMovie(movie);
        showtime.setTheater(theater);
        showtime.setStartTime(startTime);
        showtime.setEndTime(startTime.plusMinutes(120));
        showtime.setMaxSeats(50);
        showtime.setAvailableSeats(50);
        showtime.setBookings(new ArrayList<>());

        response = new ShowtimeResponse();
        response.setId(1L);
    }

    @Test
    void createShowtime_Success() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(showtimeRepository.findOverlappingShowtimes(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(showtimeRepository.save(any(Showtime.class))).thenReturn(showtime);
        when(showtimeRepository.findById(any())).thenReturn(Optional.of(showtime));
        when(showtimeMapper.toResponse(any(Showtime.class))).thenReturn(response);
        when(showtimeMapper.toEntity(any(), any(), any(), any())).thenReturn(showtime);

        // Act
        ShowtimeResponse result = showtimeService.createShowtime(request);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(showtimeRepository).save(any(Showtime.class));
    }

    @Test
    void createShowtime_MovieNotFound() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> showtimeService.createShowtime(request));
        assertEquals(ErrorCode.MOVIE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createShowtime_TheaterNotFound() {
        // Arrange
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(theaterRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> showtimeService.createShowtime(request));
        assertEquals(ErrorCode.THEATER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void createShowtime_ExceedsTheaterCapacity() {
        // Arrange
        request.setMaxSeats(150); // Theater capacity is 100
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> showtimeService.createShowtime(request));
        assertEquals(ErrorCode.INSUFFICIENT_SEATS, exception.getErrorCode());
    }

    @Test
    void createShowtime_OverlappingShowtime() {
        // Arrange
        List<Showtime> overlappingShowtimes = Collections.singletonList(showtime);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(showtimeRepository.findOverlappingShowtimes(any(), any(), any()))
                .thenReturn(overlappingShowtimes);

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> showtimeService.createShowtime(request));
        assertEquals(ErrorCode.SHOWTIME_OVERLAP, exception.getErrorCode());
    }

    @Test
    void updateShowtime_Success() {
        // Arrange
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie));
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(showtimeRepository.findOverlappingShowtimes(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(showtimeRepository.save(any(Showtime.class))).thenReturn(showtime);
        when(showtimeMapper.toResponse(any(Showtime.class))).thenReturn(response);

        // Act
        ShowtimeResponse result = showtimeService.updateShowtime(1L, request);

        // Assert
        assertNotNull(result);
        verify(showtimeRepository).save(any(Showtime.class));
    }

    @Test
    void updateShowtime_WithExistingBookings() {
        // Arrange
        Booking booking = Booking.builder()
                .id(1L)
                .user(User.builder().id(1L).username("testUser").build())
                .showtime(showtime)
                .seatNumber(1)
                .price(new BigDecimal("10.00"))
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.CONFIRMED)
                .build();
        showtime.setBookings(List.of(booking));
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> showtimeService.updateShowtime(1L, request));
        assertEquals(ErrorCode.SHOWTIME_HAS_BOOKINGS, exception.getErrorCode());
    }

    @Test
    void deleteShowtime_Success() {
        // Arrange
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));

        // Act
        showtimeService.deleteShowtime(1L);

        // Assert
        verify(showtimeRepository).deleteById(1L);
    }

    @Test
    void deleteShowtime_WithExistingBookings() {
        // Arrange
        Booking booking = Booking.builder()
                .id(1L)
                .user(User.builder().id(1L).username("testUser").build())
                .showtime(showtime)
                .seatNumber(1)
                .price(new BigDecimal("10.00"))
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.CONFIRMED)
                .build();
        showtime.setBookings(List.of(booking));
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> showtimeService.deleteShowtime(1L));
        assertEquals(ErrorCode.SHOWTIME_HAS_BOOKINGS, exception.getErrorCode());
    }
}