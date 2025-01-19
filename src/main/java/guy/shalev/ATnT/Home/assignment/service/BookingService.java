package guy.shalev.ATnT.Home.assignment.service;

import guy.shalev.ATnT.Home.assignment.model.dto.request.BookingRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.BookingResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BookingService {
    BookingResponse createBooking(String username, BookingRequest request);

    @Transactional(readOnly = true)
    BookingResponse getBooking(Long id);

    @Transactional(readOnly = true)
    List<BookingResponse> getUserBookings(String username);

    @Transactional(readOnly = true)
    boolean isSeatAvailable(Long showtimeId, Integer seatNumber);
}
