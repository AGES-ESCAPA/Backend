package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.LoginRequest;
import com.escapa.backend.adapters.dto.LoginResponse;
import com.escapa.backend.adapters.dto.LoginUserResponse;
import com.escapa.backend.adapters.dto.RegisterCompanyRequest;
import com.escapa.backend.adapters.dto.RegisterStudentRequest;
import com.escapa.backend.adapters.exception.ApiError;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.LoginOutput;
import com.escapa.backend.application.usecase.LoginUseCase;
import com.escapa.backend.application.usecase.RegisterCompanyUseCase;
import com.escapa.backend.application.usecase.RegisterStudentUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Login and public account registration")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterCompanyUseCase registerCompanyUseCase;
    private final RegisterStudentUseCase registerStudentUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;

    public AuthController(
            LoginUseCase loginUseCase,
            RegisterCompanyUseCase registerCompanyUseCase,
            RegisterStudentUseCase registerStudentUseCase,
            GetUserByIdUseCase getUserByIdUseCase
    ) {
        this.loginUseCase = loginUseCase;
        this.registerCompanyUseCase = registerCompanyUseCase;
        this.registerStudentUseCase = registerStudentUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        final LoginOutput output = loginUseCase.execute(request.email(), request.password());
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(output), "Operation completed successfully"));
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
                .body(ApiResponse.success(LoginResponse.from(output), "Company registered successfully"));
    }

    @PostMapping("/register/student")
    @Operation(summary = "Register a student account")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "Student created with an access token", useReturnTypeSchema = true)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "email or cpf already registered",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<ApiResponse<LoginResponse>> registerStudent(
            @Valid @RequestBody RegisterStudentRequest request
    ) {
        final LoginOutput output = registerStudentUseCase.execute(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(LoginResponse.from(output), "Student registered successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginUserResponse>> me() {
        final var authenticatedUser = AuthenticationContext.requireUser();
        final var user = getUserByIdUseCase.execute(authenticatedUser.id());
        return ResponseEntity.ok(ApiResponse.success(LoginUserResponse.from(user, authenticatedUser.profile())));
    }
}
