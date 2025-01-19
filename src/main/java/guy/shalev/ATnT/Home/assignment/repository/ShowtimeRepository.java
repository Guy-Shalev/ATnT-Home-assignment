package guy.shalev.ATnT.Home.assignment.repository;

import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import guy.shalev.ATnT.Home.assignment.model.entities.Showtime;
import guy.shalev.ATnT.Home.assignment.model.entities.Theater;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    List<Showtime> findByMovie(Movie movie);

    List<Showtime> findByTheater(Theater theater);

    List<Showtime> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    // Find overlapping showtimes for a theater
    @Query("SELECT s FROM Showtime s WHERE s.theater = :theater " +
            "AND ((s.startTime BETWEEN :start AND :end) OR " +
            "(s.endTime BETWEEN :start AND :end) OR " +
            "(s.startTime <= :start AND s.endTime >= :end))")
    List<Showtime> findOverlappingShowtimes(
            @Param("theater") Theater theater,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Showtime s WHERE s.id = :id")
    Optional<Showtime> findByIdWithLock(@Param("id") Long id);
}
