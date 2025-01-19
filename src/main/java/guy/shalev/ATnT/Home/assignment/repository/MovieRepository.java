package guy.shalev.ATnT.Home.assignment.repository;

import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByGenre(String genre);

    Optional<Movie> findByTitle(String title);

    List<Movie> findByReleaseYear(Integer year);
}
