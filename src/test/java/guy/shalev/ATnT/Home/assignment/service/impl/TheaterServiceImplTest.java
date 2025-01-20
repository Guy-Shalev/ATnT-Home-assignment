package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.ErrorCode;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.mapper.TheaterMapper;
import guy.shalev.ATnT.Home.assignment.model.dto.request.TheaterRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import guy.shalev.ATnT.Home.assignment.model.entities.Showtime;
import guy.shalev.ATnT.Home.assignment.model.entities.Theater;
import guy.shalev.ATnT.Home.assignment.repository.TheaterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TheaterServiceImplTest {

    @Mock
    private TheaterRepository theaterRepository;

    @Mock
    private TheaterMapper theaterMapper;

    @InjectMocks
    private TheaterServiceImpl theaterService;

    private Theater theater;
    private TheaterRequest theaterRequest;
    private TheaterResponse theaterResponse;

    @BeforeEach
    void setUp() {
        // Initialize test data
        theater = new Theater();
        theater.setId(1L);
        theater.setName("Test Theater");
        theater.setCapacity(100);
        theater.setShowtimes(new ArrayList<>());

        theaterRequest = new TheaterRequest();
        theaterRequest.setName("Test Theater");
        theaterRequest.setCapacity(100);

        theaterResponse = new TheaterResponse();
        theaterResponse.setId(1L);
        theaterResponse.setName("Test Theater");
        theaterResponse.setCapacity(100);
    }

    @Test
    void createTheater_Success() {
        // Arrange
        when(theaterRepository.findByName("Test Theater")).thenReturn(Optional.empty());
        when(theaterMapper.toEntity(any(TheaterRequest.class))).thenReturn(theater);
        when(theaterRepository.save(any(Theater.class))).thenReturn(theater);
        when(theaterMapper.toResponse(any(Theater.class))).thenReturn(theaterResponse);

        // Act
        TheaterResponse result = theaterService.createTheater(theaterRequest);

        // Assert
        assertNotNull(result);
        assertEquals(theaterResponse.getId(), result.getId());
        assertEquals(theaterResponse.getName(), result.getName());
        assertEquals(theaterResponse.getCapacity(), result.getCapacity());
        verify(theaterRepository).save(any(Theater.class));
    }

    @Test
    void createTheater_NameExists() {
        // Arrange
        when(theaterRepository.findByName("Test Theater")).thenReturn(Optional.of(theater));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> theaterService.createTheater(theaterRequest));
        assertEquals(ErrorCode.THEATER_NAME_EXISTS, exception.getErrorCode());
        verify(theaterRepository, never()).save(any(Theater.class));
    }

    @Test
    void getTheater_Success() {
        // Arrange
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(theaterMapper.toResponse(theater)).thenReturn(theaterResponse);

        // Act
        TheaterResponse result = theaterService.getTheater(1L);

        // Assert
        assertNotNull(result);
        assertEquals(theaterResponse.getId(), result.getId());
        assertEquals(theaterResponse.getName(), result.getName());
    }

    @Test
    void getTheater_NotFound() {
        // Arrange
        when(theaterRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> theaterService.getTheater(1L));
        assertEquals(ErrorCode.THEATER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getAllTheaters_Success() {
        // Arrange
        List<Theater> theaters = Collections.singletonList(theater);
        List<TheaterResponse> theaterResponses = Collections.singletonList(theaterResponse);

        when(theaterRepository.findAll()).thenReturn(theaters);
        when(theaterMapper.toResponseList(theaters)).thenReturn(theaterResponses);

        // Act
        List<TheaterResponse> results = theaterService.getAllTheaters();

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(theaterResponse.getId(), results.get(0).getId());
    }

    @Test
    void updateTheater_Success() {
        // Arrange
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(theaterRepository.findByName("Test Theater"))
                .thenReturn(Optional.of(theater)); // Same theater, so it's OK
        when(theaterRepository.save(any(Theater.class))).thenReturn(theater);
        when(theaterMapper.toResponse(theater)).thenReturn(theaterResponse);

        // Act
        TheaterResponse result = theaterService.updateTheater(1L, theaterRequest);

        // Assert
        assertNotNull(result);
        assertEquals(theaterResponse.getId(), result.getId());
        assertEquals(theaterResponse.getName(), result.getName());
        verify(theaterRepository).save(any(Theater.class));
    }

    @Test
    void updateTheater_NotFound() {
        // Arrange
        when(theaterRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> theaterService.updateTheater(1L, theaterRequest));
        assertEquals(ErrorCode.THEATER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateTheater_NameConflict() {
        // Arrange
        Theater existingTheater = new Theater();
        existingTheater.setId(2L);
        existingTheater.setName("Test Theater");

        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));
        when(theaterRepository.findByName("Test Theater")).thenReturn(Optional.of(existingTheater));

        // Act & Assert
        ConflictException exception = assertThrows(ConflictException.class,
                () -> theaterService.updateTheater(1L, theaterRequest));
        assertEquals(ErrorCode.THEATER_NAME_EXISTS, exception.getErrorCode());
    }

    @Test
    void deleteTheater_Success() {
        // Arrange
        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));

        // Act
        theaterService.deleteTheater(1L);

        // Assert
        verify(theaterRepository).deleteById(1L);
    }

    @Test
    void deleteTheater_NotFound() {
        // Arrange
        when(theaterRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> theaterService.deleteTheater(1L));
        assertEquals(ErrorCode.THEATER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void deleteTheater_WithScheduledShowtimes() {
        // Arrange
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Test Movie");

        Showtime showtime = Showtime.builder()
                .id(1L)
                .movie(movie)
                .theater(theater)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .maxSeats(50)
                .availableSeats(50)
                .build();

        theater.setShowtimes(List.of(showtime));

        when(theaterRepository.findById(1L)).thenReturn(Optional.of(theater));

        // Act & Assert
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> theaterService.deleteTheater(1L));
        assertEquals(ErrorCode.THEATER_IN_USE, exception.getErrorCode());
        verify(theaterRepository, never()).deleteById(any());
    }
}