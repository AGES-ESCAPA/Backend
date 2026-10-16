package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.LoginRequest;
import com.escapa.backend.adapters.dto.LoginResponse;
import com.escapa.backend.adapters.dto.RegisterStudentRequest;
import com.escapa.backend.application.usecase.LoginOutput;
import com.escapa.backend.application.usecase.LoginUseCase;
import com.escapa.backend.application.usecase.RegisterStudentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Login and public account registration")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterStudentUseCase registerStudentUseCase;

    public AuthController(LoginUseCase loginUseCase, RegisterStudentUseCase registerStudentUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerStudentUseCase = registerStudentUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        final LoginOutput output = loginUseCase.execute(request.email(), request.password());
        final LoginResponse response = LoginResponse.from(output);
        return ResponseEntity.ok(ApiResponse.success(response, "Operation completed successfully"));
    }

    @PostMapping("/register/student")
    @Operation(summary = "Register a student account")
    public ResponseEntity<ApiResponse<LoginResponse>> registerStudent(
            @Valid @RequestBody RegisterStudentRequest request
    ) {
        final LoginOutput output = registerStudentUseCase.execute(request.toCommand());
        final LoginResponse response = LoginResponse.from(output);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Student registered successfully"));
    }
}

