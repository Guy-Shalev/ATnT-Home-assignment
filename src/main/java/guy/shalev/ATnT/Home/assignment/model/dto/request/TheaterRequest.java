package guy.shalev.ATnT.Home.assignment.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheaterRequest {
    @NotBlank(message = "Theater name is required")
    private String name;

    @Min(value = 1, message = "Capacity must be positive")
    private Integer capacity;
}
