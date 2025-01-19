package guy.shalev.ATnT.Home.assignment.service;

import guy.shalev.ATnT.Home.assignment.model.dto.request.TheaterRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TheaterService {
    TheaterResponse createTheater(TheaterRequest request);

    @Transactional(readOnly = true)
    TheaterResponse getTheater(Long id);

    @Transactional(readOnly = true)
    List<TheaterResponse> getAllTheaters();

    TheaterResponse updateTheater(Long id, TheaterRequest request);

    void deleteTheater(Long id);
}
