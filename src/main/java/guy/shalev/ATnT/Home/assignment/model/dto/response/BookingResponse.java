package guy.shalev.ATnT.Home.assignment.model.dto.response;

import guy.shalev.ATnT.Home.assignment.model.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private Long id;
    private ShowtimeResponse showtime;
    private Integer seatNumber;
    private BigDecimal price;
    private LocalDateTime bookingTime;
    private BookingStatus status;
}