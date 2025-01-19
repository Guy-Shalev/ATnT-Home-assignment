package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.NotFoundException;
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
        // Fetch movie and theater
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + request.getMovieId()));

        Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(() -> new NotFoundException("Theater not found with id: " + request.getTheaterId()));

        // Validate maximum seats against theater capacity
        if (request.getMaxSeats() > theater.getCapacity()) {
            throw new BadRequestException("Max seats cannot exceed theater capacity: " + theater.getCapacity());
        }

        // Calculate end time based on movie duration
        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration());

        // Check for overlapping showtimes
        List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
                theater, request.getStartTime(), endTime);

        if (!overlappingShowtimes.isEmpty()) {
            throw new ConflictException("There is already a showtime scheduled in this theater at the requested time");
        }

        // Create and save the showtime
        Showtime showtime = showtimeMapper.toEntity(request);
        showtime.setEndTime(endTime);
        showtime.setAvailableSeats(request.getMaxSeats());

        Showtime savedShowtime = showtimeRepository.save(showtime);
        return showtimeMapper.toResponse(savedShowtime);
    }

    @Transactional(readOnly = true)
    @Override
    public ShowtimeResponse getShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Showtime not found with id: " + id));
        return showtimeMapper.toResponse(showtime);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ShowtimeResponse> getShowtimesByMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + movieId));

        List<Showtime> showtimes = showtimeRepository.findByMovie(movie);
        return showtimeMapper.toResponseList(showtimes);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ShowtimeResponse> getShowtimesByTheater(Long theaterId) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new NotFoundException("Theater not found with id: " + theaterId));

        List<Showtime> showtimes = showtimeRepository.findByTheater(theater);
        return showtimeMapper.toResponseList(showtimes);
    }

    @Override
    public ShowtimeResponse updateShowtime(Long id, ShowtimeRequest request) {
        Showtime existingShowtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Showtime not found with id: " + id));

        // If there are any bookings, prevent major changes
        if (!existingShowtime.getBookings().isEmpty()) {
            throw new BadRequestException("Cannot update showtime with existing bookings");
        }

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new NotFoundException("Movie not found with id: " + request.getMovieId()));

        Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(() -> new NotFoundException("Theater not found with id: " + request.getTheaterId()));

        // Calculate new end time
        LocalDateTime endTime = request.getStartTime().plusMinutes(movie.getDuration());

        // Check for overlapping showtimes (excluding this showtime)
        List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
                        theater, request.getStartTime(), endTime).stream()
                .filter(showtime -> !showtime.getId().equals(id))
                .toList();

        if (!overlappingShowtimes.isEmpty()) {
            throw new ConflictException("There is already another showtime scheduled in this theater at the requested time");
        }

        // Update the showtime
        existingShowtime.setMovie(movie);
        existingShowtime.setTheater(theater);
        existingShowtime.setStartTime(request.getStartTime());
        existingShowtime.setEndTime(endTime);
        existingShowtime.setMaxSeats(request.getMaxSeats());
        existingShowtime.setAvailableSeats(request.getMaxSeats());

        Showtime updatedShowtime = showtimeRepository.save(existingShowtime);
        return showtimeMapper.toResponse(updatedShowtime);
    }

    @Override
    public void deleteShowtime(Long id) {
        Showtime showtime = showtimeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Showtime not found with id: " + id));

        // Check if there are any bookings
        if (!showtime.getBookings().isEmpty()) {
            throw new BadRequestException("Cannot delete showtime with existing bookings");
        }

        showtimeRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isShowtimeAvailable(Long theaterId, LocalDateTime startTime, LocalDateTime endTime) {
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new NotFoundException("Theater not found with id: " + theaterId));

        List<Showtime> overlappingShowtimes = showtimeRepository.findOverlappingShowtimes(
                theater, startTime, endTime);

        return overlappingShowtimes.isEmpty();
    }
}
