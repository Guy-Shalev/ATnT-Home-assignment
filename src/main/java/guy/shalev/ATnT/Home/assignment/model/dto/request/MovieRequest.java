package guy.shalev.ATnT.Home.assignment.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Genre is required")
    private String genre;

    @Min(value = 1, message = "Duration must be positive")
    private Integer duration;

    @NotBlank(message = "Rating is required")
    private String rating;

    @Min(value = 1900, message = "Release year must be valid")
    private Integer releaseYear;
}
