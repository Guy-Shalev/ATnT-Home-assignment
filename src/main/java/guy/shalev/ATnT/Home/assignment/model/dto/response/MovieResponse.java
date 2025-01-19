package guy.shalev.ATnT.Home.assignment.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {
    private Long id;
    private String title;
    private String genre;
    private Integer duration;
    private String rating;
    private Integer releaseYear;
}
