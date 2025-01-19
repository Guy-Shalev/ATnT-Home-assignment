package guy.shalev.ATnT.Home.assignment.mapper;

import guy.shalev.ATnT.Home.assignment.model.dto.request.ShowtimeRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.ShowtimeResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import guy.shalev.ATnT.Home.assignment.model.entities.Showtime;
import guy.shalev.ATnT.Home.assignment.model.entities.Theater;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring", uses = {MovieMapper.class, TheaterMapper.class})
public interface ShowtimeMapper {
    @Mapping(source = "movie", target = "movie")
    @Mapping(source = "theater", target = "theater")
    ShowtimeResponse toResponse(Showtime showtime);

    List<ShowtimeResponse> toResponseList(List<Showtime> showtimes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "movie", source = "movie")
    @Mapping(target = "theater", source = "theater")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "availableSeats", source = "request.maxSeats")
    @Mapping(target = "maxSeats", source = "request.maxSeats")
    @Mapping(target = "bookings", ignore = true)
    @Mapping(target = "version", ignore = true)
    Showtime toEntity(Movie movie, Theater theater, ShowtimeRequest request, LocalDateTime endTime);
}
