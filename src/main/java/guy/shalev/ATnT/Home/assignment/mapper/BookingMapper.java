package guy.shalev.ATnT.Home.assignment.mapper;

import guy.shalev.ATnT.Home.assignment.model.dto.request.BookingRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.BookingResponse;
import guy.shalev.ATnT.Home.assignment.model.entities.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring", uses = {ShowtimeMapper.class}, imports = {LocalDateTime.class})
public interface BookingMapper {
    @Mapping(source = "showtime", target = "showtime")
    @Mapping(source = "seatNumber", target = "seatNumber")
    @Mapping(source = "price", target = "price")
    @Mapping(source = "bookingTime", target = "bookingTime")
    @Mapping(source = "status", target = "status")
    BookingResponse toResponse(Booking booking);

    List<BookingResponse> toResponseList(List<Booking> bookings);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "showtime.id", source = "showtimeId")
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "bookingTime", expression = "java(LocalDateTime.now())")
    @Mapping(target = "status", constant = "PENDING")
    Booking toEntity(BookingRequest request);
}