package guy.shalev.ATnT.Home.assignment.repository;

import guy.shalev.ATnT.Home.assignment.model.entities.Booking;
import guy.shalev.ATnT.Home.assignment.model.entities.Showtime;
import guy.shalev.ATnT.Home.assignment.model.entities.User;
import guy.shalev.ATnT.Home.assignment.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser(User user);

    List<Booking> findByShowtime(Showtime showtime);

    List<Booking> findByStatus(BookingStatus status);

    // Find existing booking for a specific seat in a showtime
    Optional<Booking> findByShowtimeAndSeatNumber(Showtime showtime, Integer seatNumber);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.showtime = :showtime AND b.status = 'CONFIRMED'")
    long countConfirmedBookingsByShowtime(@Param("showtime") Showtime showtime);
}
