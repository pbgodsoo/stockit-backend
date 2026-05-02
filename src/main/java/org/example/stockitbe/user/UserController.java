package org.example.stockitbe.user;

import lombok.RequiredArgsConstructor;
import org.example.stockitbe.common.model.BaseResponse;
import org.example.stockitbe.user.model.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/user")
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<BaseResponse<UserDto.SignupRes>> signup(
            @RequestBody UserDto.SignupReq req) {
        UserDto.SignupRes result = userService.signup(req);
        return ResponseEntity.ok(BaseResponse.success(result));

    }
}



