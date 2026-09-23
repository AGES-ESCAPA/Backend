package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.LoginRequest;
import com.escapa.backend.adapters.dto.LoginResponse;
import com.escapa.backend.application.usecase.AuthenticateUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        final String token = authenticateUserUseCase.execute(request.email(), request.password());
        
        final LoginResponse response = new LoginResponse(token, "Bearer");
        
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }
}
