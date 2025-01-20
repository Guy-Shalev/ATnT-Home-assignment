package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.ErrorCode;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.mapper.BookingMapper;
import guy.shalev.ATnT.Home.assignment.model.dto.request.BookingRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.request.SeatRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(transactionManager = "transactionManager")
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    @Value("${app.booking.ticket.price}")
    private BigDecimal ticketPrice;

    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    public List<BookingResponse> createBooking(String username, BookingRequest request) {
        User user = getUserByUsername(username);
        Showtime showtime = getShowtimeWithLock(request.getShowtimeId());

        validateBookingRequest(showtime, request);
        List<Booking> bookings = createBookings(user, showtime, request.getSeats());
        updateShowtimeSeats(showtime, request.getSeats().size());

        List<Booking> savedBookings = bookingRepository.saveAll(bookings);
        return bookingMapper.toResponseList(savedBookings);
    }

    private Showtime getShowtimeWithLock(Long showtimeId) {
        return showtimeRepository.findByIdWithLock(showtimeId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHOWTIME_NOT_FOUND, "Showtime not found with id: " + showtimeId));
    }

    private void validateBookingRequest(Showtime showtime, BookingRequest request) {
        validateAvailableSeats(showtime, request.getSeats().size());
        validateSeatNumbers(showtime, request.getSeats());
        validateSeatsNotBooked(showtime, request.getSeats());
    }

    private void validateAvailableSeats(Showtime showtime, int requestedSeats) {
        if (showtime.getAvailableSeats() < requestedSeats) {
            throw new ConflictException(ErrorCode.INSUFFICIENT_SEATS, String.format(
                    "Not enough seats available. Requested: %d, Available: %d",
                    requestedSeats, showtime.getAvailableSeats()));
        }
    }

    private void validateSeatNumbers(Showtime showtime, List<SeatRequest> seats) {
        for (SeatRequest seat : seats) {
            if (seat.getSeatNumber() > showtime.getMaxSeats()) {
                throw new BadRequestException(ErrorCode.INVALID_SEAT_NUMBER,
                        "Invalid seat number " + seat.getSeatNumber() +
                                ". Maximum seat number is: " + showtime.getMaxSeats());
            }
        }
    }

    private List<Booking> createBookings(User user, Showtime showtime, List<SeatRequest> seatRequests) {
        return seatRequests.stream()
                .map(seatRequest -> createSingleBooking(user, showtime, seatRequest))
                .toList();
    }

    private Booking createSingleBooking(User user, Showtime showtime, SeatRequest seatRequest) {
        return Booking.builder()
                .user(user)
                .showtime(showtime)
                .seatNumber(seatRequest.getSeatNumber())
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.CONFIRMED)
                .price(ticketPrice)
                .build();
    }

    private void validateSeatsNotBooked(Showtime showtime, List<SeatRequest> seats) {
        for (SeatRequest seat : seats) {
            if (bookingRepository.findByShowtimeAndSeatNumber(showtime, seat.getSeatNumber()).isPresent()) {
                throw new ConflictException(ErrorCode.SEAT_ALREADY_BOOKED, "Seat " + seat.getSeatNumber() + " is already booked");
            }
        }
    }

    private void updateShowtimeSeats(Showtime showtime, int bookedSeats) {
        showtime.setAvailableSeats(showtime.getAvailableSeats() - bookedSeats);
        showtimeRepository.save(showtime);
    }

    @Transactional(readOnly = true)
    @Override
    public BookingResponse getBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.BOOKING_NOT_FOUND, "Booking not found with id: " + id));
        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    @Override
    public List<BookingResponse> getUserBookings(String username) {
        User user = getUserByUsername(username);
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

    private User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found with username: " + username));
    }
}
