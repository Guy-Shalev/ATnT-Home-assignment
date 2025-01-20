package guy.shalev.ATnT.Home.assignment.exception;

import guy.shalev.ATnT.Home.assignment.exception.errorResponse.ErrorResponse;
import guy.shalev.ATnT.Home.assignment.exception.errorResponse.ValidationErrorResponse;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.BadRequestException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.ConflictException;
import guy.shalev.ATnT.Home.assignment.exception.exceptions.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handle all custom business exceptions
    @ExceptionHandler({NotFoundException.class, BadRequestException.class, ConflictException.class})
    public ErrorResponse handleCustomExceptions(RuntimeException ex, HttpServletRequest request) {
        HttpStatus status = determineHttpStatus(ex);

        return new ErrorResponse(
                status.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (error1, error2) -> error1
                ));

        return new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                LocalDateTime.now(),
                request.getRequestURI(),
                errors
        );
    }

    // Handle security exceptions
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public ErrorResponse handleSecurityException(Exception ex, HttpServletRequest request) {
        HttpStatus status = ex instanceof AccessDeniedException ?
                HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;

        return new ErrorResponse(
                status.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    // Handle all other unexpected exceptions
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllUncaughtException(Exception ex, HttpServletRequest request) {
        return new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                LocalDateTime.now(),
                request.getRequestURI()
        );
    }

    private HttpStatus determineHttpStatus(RuntimeException ex) {
        if (ex instanceof NotFoundException) return HttpStatus.NOT_FOUND;
        if (ex instanceof BadRequestException) return HttpStatus.BAD_REQUEST;
        if (ex instanceof ConflictException) return HttpStatus.CONFLICT;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
