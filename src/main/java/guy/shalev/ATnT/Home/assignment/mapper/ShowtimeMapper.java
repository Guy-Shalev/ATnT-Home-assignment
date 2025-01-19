package guy.shalev.ATnT.Home.assignment.mapper;

import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Showtime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MovieMapper.class, TheaterMapper.class})
public interface ShowtimeMapper {
    @Mapping(source = "movie", target = "movie")
    @Mapping(source = "theater", target = "theater")
    ShowtimeResponse toResponse(Showtime showtime);

    List<ShowtimeResponse> toResponseList(List<Showtime> showtimes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "movie.id", source = "movieId")
    @Mapping(target = "theater.id", source = "theaterId")
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "availableSeats", expression = "java(request.getMaxSeats())")
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "version", ignore = true)
    Showtime toEntity(ShowtimeRequest request);
}
