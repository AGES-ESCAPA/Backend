package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.LoginRequest;
import com.escapa.backend.adapters.dto.LoginResponse;
import com.escapa.backend.adapters.dto.LoginUserResponse;
import com.escapa.backend.adapters.dto.RegisterCompanyRequest;
import com.escapa.backend.application.usecase.LoginOutput;
import com.escapa.backend.application.usecase.LoginUseCase;
import com.escapa.backend.application.usecase.RegisterCompanyUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterCompanyUseCase registerCompanyUseCase;

    public AuthController(LoginUseCase loginUseCase, RegisterCompanyUseCase registerCompanyUseCase) {
        this.loginUseCase = loginUseCase;
        this.registerCompanyUseCase = registerCompanyUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        final LoginOutput output = loginUseCase.execute(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.success(toResponse(output), "Operation completed successfully"));
    }

    @PostMapping("/register/company")
    public ResponseEntity<ApiResponse<LoginResponse>> registerCompany(
            @Valid @RequestBody RegisterCompanyRequest request
    ) {
        final LoginOutput output = registerCompanyUseCase.execute(
                request.responsibleName(),
                request.cnpj(),
                request.companyName(),
                request.email(),
                request.phone(),
                request.password()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(toResponse(output), "Company registered successfully"));
    }

    private static LoginResponse toResponse(LoginOutput output) {
        final LoginUserResponse userResponse = new LoginUserResponse(
                output.user().getId(),
                output.user().getName(),
                output.user().getEmail(),
                output.profile()
        );
        return new LoginResponse(
                output.accessToken(),
                output.tokenType(),
                output.expiresIn(),
                userResponse
        );
    }
}