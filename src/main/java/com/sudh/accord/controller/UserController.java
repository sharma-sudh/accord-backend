package com.sudh.accord.controller;

import com.sudh.accord.entity.User;
import com.sudh.accord.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping
    public User updateBudget(@RequestBody BigDecimal budget, @AuthenticationPrincipal String userId) {
        return userService.updateBudget(UUID.fromString(userId), budget);
    }
}
