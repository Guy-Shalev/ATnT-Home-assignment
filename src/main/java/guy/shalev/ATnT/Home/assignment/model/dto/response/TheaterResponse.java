package guy.shalev.ATnT.Home.assignment.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TheaterResponse {
    private Long id;
    private String name;
    private Integer capacity;
}
