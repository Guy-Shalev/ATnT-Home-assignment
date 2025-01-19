package guy.shalev.ATnT.Home.assignment.service;

import guy.shalev.ATnT.Home.assignment.model.dto.request.BookingRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.BookingResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BookingService {
    BookingResponse createBooking(Long userId, BookingRequest request);

    @Transactional(readOnly = true)
    BookingResponse getBooking(Long id);

    @Transactional(readOnly = true)
    List<BookingResponse> getUserBookings(Long userId);

    @Transactional(readOnly = true)
    boolean isSeatAvailable(Long showtimeId, Integer seatNumber);
}
