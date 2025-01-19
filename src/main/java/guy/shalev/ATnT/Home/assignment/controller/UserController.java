package guy.shalev.ATnT.Home.assignment.controller;

import guy.shalev.ATnT.Home.assignment.model.dto.request.UserRequest;
import guy.shalev.ATnT.Home.assignment.model.dto.response.UserResponse;
import guy.shalev.ATnT.Home.assignment.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerUser(@RequestBody @Valid UserRequest request) {
        userService.registerUser(request);
    }

    @GetMapping("/current")
    public UserResponse getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        return userService.getCurrentUser(userDetails.getUsername());
    }

}
