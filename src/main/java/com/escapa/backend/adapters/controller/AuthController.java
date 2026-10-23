package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.LoginRequest;
import com.escapa.backend.adapters.dto.LoginResponse;
import com.escapa.backend.adapters.dto.LoginUserResponse;
import com.escapa.backend.adapters.dto.RegisterCompanyRequest;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.LoginOutput;
import com.escapa.backend.application.usecase.LoginUseCase;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.RegisterCompanyUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterCompanyUseCase registerCompanyUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;

    public AuthController(
            LoginUseCase loginUseCase,
            RegisterCompanyUseCase registerCompanyUseCase,
            GetUserByIdUseCase getUserByIdUseCase
    ) {
        this.loginUseCase = loginUseCase;
        this.registerCompanyUseCase = registerCompanyUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
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

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginUserResponse>> me() {
        final var authenticatedUser = AuthenticationContext.requireUser();
        final var user = getUserByIdUseCase.execute(authenticatedUser.id());
        return ResponseEntity.ok(ApiResponse.success(LoginUserResponse.from(user, authenticatedUser.profile())));
    }

    private static LoginResponse toResponse(LoginOutput output) {
        final LoginUserResponse userResponse = LoginUserResponse.from(output.user(), output.profile());
        return new LoginResponse(
                output.accessToken(),
                output.tokenType(),
                output.expiresIn(),
                userResponse
        );
    }
}
