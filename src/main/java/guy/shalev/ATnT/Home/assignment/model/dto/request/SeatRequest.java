package guy.shalev.ATnT.Home.assignment.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatRequest {
    @NotNull(message = "Seat number is required")
    @Min(value = 1, message = "Seat number must be positive")
    private Integer seatNumber;
}