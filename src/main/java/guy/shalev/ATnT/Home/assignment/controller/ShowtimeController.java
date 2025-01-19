package guy.shalev.ATnT.Home.assignment.controller;

import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
import guy.shalev.ATnT.Home.assignment.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShowtimeResponse createShowtime(@RequestBody @Valid ShowtimeRequest request) {
        return showtimeService.createShowtime(request);
    }

    @GetMapping("/{id}")
    public ShowtimeResponse getShowtime(@PathVariable Long id) {
        return showtimeService.getShowtime(id);
    }

    @GetMapping("/movie/{movieId}")
    public List<ShowtimeResponse> getShowtimesByMovie(@PathVariable Long movieId) {
        return showtimeService.getShowtimesByMovie(movieId);
    }

    @GetMapping("/theater/{theaterId}")
    public List<ShowtimeResponse> getShowtimesByTheater(@PathVariable Long theaterId) {
        return showtimeService.getShowtimesByTheater(theaterId);
    }

    @PutMapping("/{id}")
    public ShowtimeResponse updateShowtime(@PathVariable Long id, @RequestBody @Valid ShowtimeRequest request) {
        return showtimeService.updateShowtime(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShowtime(@PathVariable Long id) {
        showtimeService.deleteShowtime(id);
    }

    @GetMapping("/available")
    public boolean isShowtimeAvailable(
            @RequestParam Long theaterId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return showtimeService.isShowtimeAvailable(theaterId, startTime, endTime);
    }

}
