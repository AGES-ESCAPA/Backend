package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.UserResponse;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.domain.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class MeController {

    private final GetUserByIdUseCase getUserByIdUseCase;

    public MeController(GetUserByIdUseCase getUserByIdUseCase) {
        this.getUserByIdUseCase = getUserByIdUseCase;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@RequestHeader("X-User-Id") UUID userId) {
        final User user = getUserByIdUseCase.execute(userId);
        
        final UserResponse response = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getUserType(),
                user.getCreatedAt()
        );
        
        return ResponseEntity.ok(ApiResponse.success(response, "User profile retrieved successfully"));
    }
}
