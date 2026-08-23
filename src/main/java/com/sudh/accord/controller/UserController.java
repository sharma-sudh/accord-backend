package com.sudh.accord.controller;

import com.sudh.accord.dto.UpdateBudgetRequest;
import com.sudh.accord.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping
    public ResponseEntity<Void> updateBudget(
            @AuthenticationPrincipal String userId,
            @RequestBody UpdateBudgetRequest req
    ) {
        userService.updateBudget(UUID.fromString(userId), req.budget());
        return ResponseEntity.noContent().build();
    }
}
