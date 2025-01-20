package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.ErrorCode;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.mapper.ShowtimeMapper;
import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import guy.shalev.ATnT.Home.assignment.model.entities.Showtime;
import guy.shalev.ATnT.Home.assignment.model.entities.Theater;
import guy.shalev.ATnT.Home.assignment.repository.MovieRepository;
import guy.shalev.ATnT.Home.assignment.repository.ShowtimeRepository;
import guy.shalev.ATnT.Home.assignment.repository.TheaterRepository;
import guy.shalev.ATnT.Home.assignment.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final ShowtimeMapper showtimeMapper;

    @Override
    public ShowtimeResponse createShowtime(ShowtimeRequest request) {
        Movie movie = getMovie(request.getMovieId());
        Theater theater = getTheater(request.getTheaterId());

        validateTheaterCapacity(theater, request.getMaxSeats());

        LocalDateTime endTime = calculateEndTime(movie, request.getStartTime());
        validateNoOverlappingShowtimes(theater, request.getStartTime(), endTime);

        Showtime showtime = showtimeMapper.toEntity(movie, theater, request, endTime);
        return showtimeMapper.toResponse(saveAndRefresh(showtime));
    }

    private Movie getMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found with id: " + movieId));
    }

    private Theater getTheater(Long theaterId) {
        return theaterRepository.findById(theaterId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.THEATER_NOT_FOUND, "Theater not found with id: " + theaterId));
    }

    private void validateTheaterCapacity(Theater theater, int requestedSeats) {
        if (requestedSeats > theater.getCapacity()) {
            throw new BadRequestException(ErrorCode.INSUFFICIENT_SEATS, "Max seats cannot exceed theater capacity: " + theater.getCapacity());
        }
    }

    private LocalDateTime calculateEndTime(Movie movie, LocalDateTime startTime) {
        return startTime.plusMinutes(movie.getDuration());
    }

    private void validateNoOverlappingShowtimes(Theater theater, LocalDateTime startTime, LocalDateTime endTime) {
        List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
                theater, startTime, endTime);

        if (!overlappingShowtimes.isEmpty()) {
            throw new ConflictException(ErrorCode.SHOWTIME_OVERLAP, "There is already a showtime scheduled in this theater at the requested time");
        }
    }

    private Showtime saveAndRefresh(Showtime showtime) {
        Showtime savedShowtime = showtimeRepository.save(showtime);
        return showtimeRepository.findById(savedShowtime.getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHOWTIME_NOT_FOUND, "Showtime not found after saving"));
    }

    @Transactional(readOnly = true)
    @Override
    public ShowtimeResponse getShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHOWTIME_NOT_FOUND, "Showtime not found with id: " + id));
        return showtimeMapper.toResponse(showtime);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ShowtimeResponse> getShowtimesByMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found with id: " + movieId));

        List<Showtime> showtimes = showtimeRepository.findByMovie(movie);
        return showtimeMapper.toResponseList(showtimes);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ShowtimeResponse> getShowtimesByTheater(Long theaterId) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.THEATER_NOT_FOUND, "Theater not found with id: " + theaterId));

        List<Showtime> showtimes = showtimeRepository.findByTheater(theater);
        return showtimeMapper.toResponseList(showtimes);
    }

    @Override
    public ShowtimeResponse updateShowtime(Long id, ShowtimeRequest request) {
        Showtime existingShowtime = findExistingShowtime(id);
        validateNoBookings(existingShowtime);

        Movie movie = findMovie(request.getMovieId());
        Theater theater = findTheater(request.getTheaterId());

        LocalDateTime endTime = calculateEndTime(request.getStartTime(), movie);
        checkOverlappingShowtimes(theater, request.getStartTime(), endTime, id);

        Showtime updatedShowtime = updateShowtimeEntity(existingShowtime, movie, theater, request);
        updatedShowtime = saveShowtime(updatedShowtime);

        return showtimeMapper.toResponse(updatedShowtime);
    }

    private Showtime findExistingShowtime(Long id) {
        return showtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHOWTIME_NOT_FOUND,
                        "Showtime not found with id: " + id));
    }

    private void validateNoBookings(Showtime showtime) {
        if (!showtime.getBookings().isEmpty()) {
            throw new BadRequestException(ErrorCode.SHOWTIME_HAS_BOOKINGS,
                    "Cannot update showtime with existing bookings");
        }
    }

    private Movie findMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MOVIE_NOT_FOUND,
                        "Movie not found with id: " + movieId));
    }

    private Theater findTheater(Long theaterId) {
        return theaterRepository.findById(theaterId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.THEATER_NOT_FOUND,
                        "Theater not found with id: " + theaterId));
    }

    private LocalDateTime calculateEndTime(LocalDateTime startTime, Movie movie) {
        return startTime.plusMinutes(movie.getDuration());
    }

    private void checkOverlappingShowtimes(Theater theater, LocalDateTime startTime,
                                           LocalDateTime endTime, Long showtimeId) {
        List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
                        theater, startTime, endTime).stream()
                .filter(showtime -> !showtime.getId().equals(showtimeId))
                .toList();

        if (!overlappingShowtimes.isEmpty()) {
            throw new ConflictException(ErrorCode.SHOWTIME_OVERLAP,
                    "There is already a showtime scheduled in this theater at the requested time");
        }
    }

    private Showtime updateShowtimeEntity(Showtime existingShowtime, Movie movie,
                                          Theater theater, ShowtimeRequest request) {
        existingShowtime.setMovie(movie);
        existingShowtime.setTheater(theater);
        existingShowtime.setStartTime(request.getStartTime());
        existingShowtime.setEndTime(calculateEndTime(request.getStartTime(), movie));
        existingShowtime.setMaxSeats(request.getMaxSeats());
        existingShowtime.setAvailableSeats(request.getMaxSeats());
        return existingShowtime;
    }

    private Showtime saveShowtime(Showtime showtime) {
        return showtimeRepository.save(showtime);
    }

    @Override
    public void deleteShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHOWTIME_NOT_FOUND, "Showtime not found with id: " + id));

        // Check if there are any bookings
        if (!showtime.getBookings().isEmpty()) {
            throw new BadRequestException(ErrorCode.SHOWTIME_HAS_BOOKINGS, "Cannot delete showtime with existing bookings");
        }

        showtimeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isShowtimeAvailable(Long theaterId, LocalDateTime startTime, LocalDateTime endTime) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.THEATER_NOT_FOUND, "Theater not found with id: " + theaterId));

        List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
                theater, startTime, endTime);

        return overlappingShowtimes.isEmpty();
    }
}
