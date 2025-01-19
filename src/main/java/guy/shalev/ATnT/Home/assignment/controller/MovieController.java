package guy.shalev.ATnT.Home.assignment.controller;

import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.service.MovieService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponse createMovie(@RequestBody @Valid MovieRequest request) {
        return movieService.createMovie(request);
    }

    @GetMapping("/{id}")
    public MovieResponse getMovie(@PathVariable Long id) {
        return movieService.getMovie(id);
    }

    @GetMapping
    public List<MovieResponse> getAllMovies() {
        return movieService.getAllMovies();
    }

    @PutMapping("/{id}")
    public MovieResponse updateMovie(@PathVariable Long id, @RequestBody @Valid MovieRequest request) {
        return movieService.updateMovie(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
    }

    @GetMapping("/search")
    public List<MovieResponse> searchMovies(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer duration,
            @RequestParam(required = false) String rating,
            @RequestParam(required = false) Integer releaseYear) {
        return movieService.searchMovies(title, genre, duration, rating, releaseYear);
    }
}
