package guy.shalev.ATnT.Home.assignment.mapper;

import guy.shalev.ATnT.Home.assignment.model.dto.request.MovieRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.MovieResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MovieMapper {
    MovieResponse toResponse(Movie movie);

    List<MovieResponse> toResponseList(List<Movie> movies);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "showtimes", ignore = true)
    Movie toEntity(MovieRequest request);
}
