package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.ErrorCode;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import guy.shalev.ATnT.Home.assignment.mapper.MovieMapper;
import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import guy.shalev.ATnT.Home.assignment.repository.MovieRepository;
import guy.shalev.ATnT.Home.assignment.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    @Override
    public MovieResponse createMovie(MovieRequest request) {
        Movie movie = movieMapper.toEntity(request);
        Movie savedMovie = movieRepository.save(movie);
        return movieMapper.toResponse(savedMovie);
    }

    @Transactional(readOnly = true)
    @Override
    public MovieResponse getMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found with id: " + id));
        return movieMapper.toResponse(movie);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MovieResponse> getAllMovies() {
        List<Movie> movies = movieRepository.findAll();
        return movieMapper.toResponseList(movies);
    }

    @Override
    public MovieResponse updateMovie(Long id, MovieRequest request) {
        Movie existingMovie = movieRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found with id: " + id));

        // Update the existing movie with the new data
        existingMovie.setTitle(request.getTitle());
        existingMovie.setGenre(request.getGenre());
        existingMovie.setDuration(request.getDuration());
        existingMovie.setRating(request.getRating());
        existingMovie.setReleaseYear(request.getReleaseYear());

        Movie updatedMovie = movieRepository.save(existingMovie);
        return movieMapper.toResponse(updatedMovie);
    }

    @Override
    public void deleteMovie(Long id) {
        if (!movieRepository.existsById(id)) {
            throw new NotFoundException(ErrorCode.MOVIE_NOT_FOUND, "Movie not found with id: " + id);
        }
        movieRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MovieResponse> searchMovies(String title, String genre, Integer duration, String rating, Integer releaseYear) {
        List<Movie> movies = movieRepository.searchMovies(title, genre, duration, rating, releaseYear);
        return movieMapper.toResponseList(movies);
    }
}
