package guy.shalev.ATnT.Home.assignment.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowtimeResponse {
    private Long id;
    private MovieResponse movie;
    private TheaterResponse theater;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer availableSeats;
    private Integer maxSeats;
}