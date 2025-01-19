package guy.shalev.ATnT.Home.assignment.service;

import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MovieService {
    MovieResponse createMovie(MovieRequest request);

    @Transactional(readOnly = true)
    MovieResponse getMovie(Long id);

    @Transactional(readOnly = true)
    List<MovieResponse> getAllMovies();

    MovieResponse updateMovie(Long id, MovieRequest request);

    void deleteMovie(Long id);

    @Transactional(readOnly = true)
    List<MovieResponse> searchMovies(String title, String genre, Integer duration, String rating, Integer releaseYear);

}
