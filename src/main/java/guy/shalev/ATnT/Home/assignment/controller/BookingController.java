package guy.shalev.ATnT.Home.assignment.controller;

import guy.shalev.ATnT.Home.assignment.model.dto.request.BookingRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.BookingResponse;
import guy.shalev.ATnT.Home.assignment.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public List<BookingResponse> createBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid BookingRequest request) {
        return bookingService.createBooking(userDetails.getUsername(), request);
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(@AuthenticationPrincipal UserDetails userDetails,
                                      @PathVariable Long id) {
        return bookingService.getBooking(id);
    }

    @GetMapping("/user")
    public List<BookingResponse> getUserBookings(@AuthenticationPrincipal UserDetails userDetails) {
        return bookingService.getUserBookings(userDetails.getUsername());
    }

    @GetMapping("/seat-available")
    public boolean isSeatAvailable(@RequestParam Long showtimeId, @RequestParam Integer seatNumber) {
        return bookingService.isSeatAvailable(showtimeId, seatNumber);
    }
}
