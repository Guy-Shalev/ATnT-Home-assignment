package guy.shalev.ATnT.Home.assignment.service.impl;

import guy.shalev.ATnT.Home.assignment.exception.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.NotFoundException;
import guy.shalev.ATnT.Home.assignment.mapper.TheaterMapper;
import guy.shalev.ATnT.Home.assignment.model.dto.request.TheaterRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Theater;
import guy.shalev.ATnT.Home.assignment.repository.TheaterRepository;
import guy.shalev.ATnT.Home.assignment.service.TheaterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TheaterServiceImpl implements TheaterService {

    private final TheaterRepository theaterRepository;
    private final TheaterMapper theaterMapper;

    @Override
    public TheaterResponse createTheater(TheaterRequest request) {
        // Check if theater with same name already exists
        theaterRepository.findByName(request.getName())
                .ifPresent(theater -> {
                    throw new ConflictException("Theater already exists with name: " + request.getName());
                });

        Theater theater = theaterMapper.toEntity(request);
        Theater savedTheater = theaterRepository.save(theater);
        return theaterMapper.toResponse(savedTheater);
    }

    @Transactional(readOnly = true)
    @Override
    public TheaterResponse getTheater(Long id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Theater not found with id: " + id));
        return theaterMapper.toResponse(theater);
    }

    @Transactional(readOnly = true)
    @Override
    public List<TheaterResponse> getAllTheaters() {
        List<Theater> theaters = theaterRepository.findAll();
        return theaterMapper.toResponseList(theaters);
    }

    @Override
    public TheaterResponse updateTheater(Long id, TheaterRequest request) {
        Theater existingTheater = theaterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Theater not found with id: " + id));

        // Check if new name conflicts with another theater
        theaterRepository.findByName(request.getName())
                .ifPresent(theater -> {
                    if (!theater.getId().equals(id)) {
                        throw new ConflictException("Theater already exists with name: " + request.getName());
                    }
                });

        existingTheater.setName(request.getName());
        existingTheater.setCapacity(request.getCapacity());

        Theater updatedTheater = theaterRepository.save(existingTheater);
        return theaterMapper.toResponse(updatedTheater);
    }

    @Override
    public void deleteTheater(Long id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Theater not found with id: " + id));

        // Check if theater has any scheduled showtimes
        if (!theater.getShowtimes().isEmpty()) {
            throw new BadRequestException("Cannot delete theater with scheduled showtimes");
        }

        theaterRepository.deleteById(id);
    }
}
