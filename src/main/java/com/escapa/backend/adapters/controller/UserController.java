package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CreateUserRequest;
import com.escapa.backend.adapters.dto.UserResponse;
import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.domain.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;

    public UserController(
            CreateUserUseCase createUserUseCase,
            ListUsersUseCase listUsersUseCase,
            GetUserByIdUseCase getUserByIdUseCase
    ) {
        this.createUserUseCase = createUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        final User user = createUserUseCase.execute(request.email(), request.password(), request.userType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(user), "User created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        final List<UserResponse> users = listUsersUseCase.execute().stream()
                .map(UserController::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable String id) {
        final User user = getUserByIdUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.success(toResponse(user)));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getUserType(), user.getCreatedAt());
    }
}
