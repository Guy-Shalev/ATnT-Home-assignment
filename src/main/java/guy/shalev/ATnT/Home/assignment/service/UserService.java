package guy.shalev.ATnT.Home.assignment.service;

import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.UserResponse;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {

    @Transactional
    void registerUser(UserRequest request);

    UserResponse getCurrentUser(String username);
}
