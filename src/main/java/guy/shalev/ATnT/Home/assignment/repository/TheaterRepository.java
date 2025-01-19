package guy.shalev.ATnT.Home.assignment.repository;

import guy.shalev.ATnT.Home.assignment.model.entities.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {
    Optional<Theater> findByName(String name);
}
