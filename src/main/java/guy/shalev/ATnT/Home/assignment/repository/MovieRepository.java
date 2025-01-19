package guy.shalev.ATnT.Home.assignment.repository;

import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    @Query("SELECT m FROM Movie m WHERE " +
            "(:title IS NULL OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
            "(:genre IS NULL OR LOWER(m.genre) = LOWER(:genre)) AND " +
            "(:duration IS NULL OR m.duration = :duration) AND " +
            "(:rating IS NULL OR LOWER(m.rating) = LOWER(:rating)) AND " +
            "(:releaseYear IS NULL OR m.releaseYear = :releaseYear)")
    List<Movie> searchMovies(
            @Param("title") String title,
            @Param("genre") String genre,
            @Param("duration") Integer duration,
            @Param("rating") String rating,
            @Param("releaseYear") Integer releaseYear
    );
}
