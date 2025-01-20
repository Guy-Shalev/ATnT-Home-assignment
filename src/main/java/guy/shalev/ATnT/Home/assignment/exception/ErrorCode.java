package guy.shalev.ATnT.Home.assignment.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Authentication & Authorization (1xxx)
    AUTHENTICATION_FAILED(1001, HttpStatus.UNAUTHORIZED, "Authentication failed"),
    INVALID_CREDENTIALS(1002, HttpStatus.UNAUTHORIZED, "Invalid username or password"),
    ACCESS_DENIED(1003, HttpStatus.FORBIDDEN, "Access denied"),
    TOKEN_EXPIRED(1004, HttpStatus.UNAUTHORIZED, "Token has expired"),
    INVALID_TOKEN(1005, HttpStatus.UNAUTHORIZED, "Invalid token"),

    // Validation Errors (2xxx)
    VALIDATION_FAILED(2001, HttpStatus.BAD_REQUEST, "Validation failed"),
    INVALID_REQUEST_PARAMETER(2002, HttpStatus.BAD_REQUEST, "Invalid request parameter"),
    MISSING_REQUIRED_FIELD(2003, HttpStatus.BAD_REQUEST, "Required field is missing"),
    INVALID_DATE_FORMAT(2004, HttpStatus.BAD_REQUEST, "Invalid date format"),
    INVALID_EMAIL_FORMAT(2005, HttpStatus.BAD_REQUEST, "Invalid email format"),

    // Resource Errors (3xxx)
    RESOURCE_NOT_FOUND(3001, HttpStatus.NOT_FOUND, "Resource not found"),
    MOVIE_NOT_FOUND(3002, HttpStatus.NOT_FOUND, "Movie not found"),
    THEATER_NOT_FOUND(3003, HttpStatus.NOT_FOUND, "Theater not found"),
    SHOWTIME_NOT_FOUND(3004, HttpStatus.NOT_FOUND, "Showtime not found"),
    USER_NOT_FOUND(3005, HttpStatus.NOT_FOUND, "User not found"),
    BOOKING_NOT_FOUND(3006, HttpStatus.NOT_FOUND, "Booking not found"),

    // Conflict Errors (4xxx)
    RESOURCE_ALREADY_EXISTS(4001, HttpStatus.CONFLICT, "Resource already exists"),
    USERNAME_ALREADY_EXISTS(4002, HttpStatus.CONFLICT, "Username already exists"),
    EMAIL_ALREADY_EXISTS(4003, HttpStatus.CONFLICT, "Email already exists"),
    THEATER_NAME_EXISTS(4004, HttpStatus.CONFLICT, "Theater with this name already exists"),
    SHOWTIME_OVERLAP(4005, HttpStatus.CONFLICT, "Showtime overlaps with existing showtime"),
    SEAT_ALREADY_BOOKED(4006, HttpStatus.CONFLICT, "Seat is already booked"),

    // Business Logic Errors (5xxx)
    INSUFFICIENT_SEATS(5001, HttpStatus.BAD_REQUEST, "Insufficient available seats"),
    INVALID_SEAT_NUMBER(5002, HttpStatus.BAD_REQUEST, "Invalid seat number"),
    PAST_SHOWTIME(5003, HttpStatus.BAD_REQUEST, "Cannot book showtime in the past"),
    THEATER_IN_USE(5004, HttpStatus.BAD_REQUEST, "Cannot delete theater with scheduled showtimes"),
    SHOWTIME_HAS_BOOKINGS(5005, HttpStatus.BAD_REQUEST, "Cannot modify showtime with existing bookings"),
    BOOKING_CLOSED(5006, HttpStatus.BAD_REQUEST, "Booking is closed for this showtime");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String getFormattedMessage(Object... args) {
        return String.format(message, args);
    }
}
