package guy.shalev.ATnT.Home.assignment.exception.errorResponse;

import java.time.LocalDateTime;

public record ErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp,
        String path
) {
}
