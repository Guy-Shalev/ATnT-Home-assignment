package guy.shalev.ATnT.Home.assignment.service;

import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface ShowtimeService {
    ShowtimeResponse createShowtime(ShowtimeRequest request);

    @Transactional(readOnly = true)
    ShowtimeResponse getShowtime(Long id);

    @Transactional(readOnly = true)
    List<ShowtimeResponse> getShowtimesByMovie(Long movieId);

    @Transactional(readOnly = true)
    List<ShowtimeResponse> getShowtimesByTheater(Long theaterId);

    ShowtimeResponse updateShowtime(Long id, ShowtimeRequest request);

    void deleteShowtime(Long id);

    @Transactional(readOnly = true)
    boolean isShowtimeAvailable(Long theaterId, LocalDateTime startTime, LocalDateTime endTime);
}
