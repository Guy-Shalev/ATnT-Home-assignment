package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.NotFoundException;
import guy.shalev.ATnT.Home.assignment.mapper.BookingMapper;
import guy.shalev.ATnT.Home.assignment.model.dto.request.BookingRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.BookingResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Booking;
import guy.shalev.ATnT.Home.assignment.model.entities.Showtime;
import guy.shalev.ATnT.Home.assignment.model.entities.User;
import guy.shalev.ATnT.Home.assignment.model.enums.BookingStatus;
import guy.shalev.ATnT.Home.assignment.repository.BookingRepository;
import guy.shalev.ATnT.Home.assignment.repository.ShowtimeRepository;
import guy.shalev.ATnT.Home.assignment.repository.UserRepository;
import guy.shalev.ATnT.Home.assignment.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(transactionManager = "transactionManager")
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    public BookingResponse createBooking(Long userId, BookingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Showtime showtime = showtimeRepository.findByIdWithLock(request.getShowtimeId())
                .orElseThrow(() -> new NotFoundException("Showtime not found with id: " + request.getShowtimeId()));

        if (showtime.getAvailableSeats() <= 0) {
            throw new ConflictException("No seats available for this showtime");
        }

        if (request.getSeatNumber() > showtime.getMaxSeats()) {
            throw new BadRequestException("Invalid seat number. Maximum seat number is: " + showtime.getMaxSeats());
        }

        if (bookingRepository.findByShowtimeAndSeatNumber(showtime, request.getSeatNumber()).isPresent()) {
            throw new ConflictException("Seat " + request.getSeatNumber() + " is already booked");
        }

        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .seatNumber(request.getSeatNumber())
                .price(BigDecimal.TEN)
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.CONFIRMED)
                .build();

        showtime.setAvailableSeats(showtime.getAvailableSeats() - 1);
        showtimeRepository.save(showtime);

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    @Override
    public BookingResponse getBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));
        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    @Override
    public List<BookingResponse> getUserBookings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        return bookingMapper.toResponseList(bookingRepository.findByUser(user));
    }

    @Transactional(readOnly = true)
    @Override
    public boolean isSeatAvailable(Long showtimeId, Integer seatNumber) {
        return bookingRepository.findByShowtimeAndSeatNumber(
                        Showtime.builder().id(showtimeId).build(),
                        seatNumber)
                .isEmpty();
    }
}
