package guy.shalev.ATnT.Home.assignment.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "showtimes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Showtime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne
    @JoinColumn(name = "theater_id", nullable = false)
    private Theater theater;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Min(0)
    @Column(name = "available_seats", nullable = false)
    private Integer availableSeats;

    @Min(1)
    @Column(name = "max_seats", nullable = false)
    private Integer maxSeats;

    @OneToMany(mappedBy = "showtime", cascade = CascadeType.ALL)
    private List<Booking> bookings = new ArrayList<>();
}
