package guy.shalev.ATnT.Home.assignment.mapper;

import guy.shalev.ATnT.Home.assignment.model.dto.request.TheaterRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.TheaterResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Theater;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TheaterMapper {
    TheaterResponse toResponse(Theater theater);

    List<TheaterResponse> toResponseList(List<Theater> theaters);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "showtimes", ignore = true)
    Theater toEntity(TheaterRequest request);
}