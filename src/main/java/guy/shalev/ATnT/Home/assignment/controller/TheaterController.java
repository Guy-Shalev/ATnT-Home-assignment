package guy.shalev.ATnT.Home.assignment.controller;

import guy.shalev.ATnT.Home.assignment.model.dto.request.TheaterRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import guy.shalev.ATnT.Home.assignment.service.TheaterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/theaters")
@RequiredArgsConstructor
public class TheaterController {

    private final TheaterService theaterService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TheaterResponse createTheater(@RequestBody @Valid TheaterRequest request) {
        return theaterService.createTheater(request);
    }

    @GetMapping("/{id}")
    public TheaterResponse getTheater(@PathVariable Long id) {
        return theaterService.getTheater(id);
    }

    @GetMapping
    public List<TheaterResponse> getAllTheaters() {
        return theaterService.getAllTheaters();
    }

    @PutMapping("/{id}")
    public TheaterResponse updateTheater(@PathVariable Long id, @RequestBody @Valid TheaterRequest request) {
        return theaterService.updateTheater(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTheater(@PathVariable Long id) {
        theaterService.deleteTheater(id);
    }

}
