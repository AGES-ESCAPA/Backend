package com.escapa.backend.adapters.controller;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CreateUserRequest;
import com.escapa.backend.adapters.dto.ProfileUpdateRequest;
import com.escapa.backend.adapters.dto.RequestEmailChangeRequest;
import com.escapa.backend.adapters.dto.UserResponse;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.model.UserProfile;
import com.escapa.backend.application.port.EmailChangeTokenRepositoryPort;
import com.escapa.backend.application.usecase.AdminDeactivateUserUseCase;
import com.escapa.backend.application.usecase.AdminUpdateUserUseCase;
import com.escapa.backend.application.usecase.CreateUserUseCase;
import com.escapa.backend.application.usecase.EmailChangeMailer;
import com.escapa.backend.application.usecase.GetProfileUseCase;
import com.escapa.backend.application.usecase.GetUserByIdUseCase;
import com.escapa.backend.application.usecase.ListUsersUseCase;
import com.escapa.backend.application.usecase.RequestAdminEmailChangeUseCase;
import com.escapa.backend.domain.entity.User;

@RestController
@Tag(name = "Users", description = "User registration and lookup")
@RequestMapping("/api/v1/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;
    private final GetProfileUseCase getProfileUseCase;
    private final AdminUpdateUserUseCase adminUpdateUserUseCase;
    private final AdminDeactivateUserUseCase adminDeactivateUserUseCase;
    private final RequestAdminEmailChangeUseCase requestAdminEmailChangeUseCase;
    private final EmailChangeMailer emailChangeMailer;
    private final EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort;
    private final Clock clock;

    public UserController(
            CreateUserUseCase createUserUseCase,
            ListUsersUseCase listUsersUseCase,
            GetUserByIdUseCase getUserByIdUseCase,
            GetProfileUseCase getProfileUseCase,
            AdminUpdateUserUseCase adminUpdateUserUseCase,
            AdminDeactivateUserUseCase adminDeactivateUserUseCase,
            RequestAdminEmailChangeUseCase requestAdminEmailChangeUseCase,
            EmailChangeMailer emailChangeMailer,
            EmailChangeTokenRepositoryPort emailChangeTokenRepositoryPort,
            Clock clock
    ) {
        this.createUserUseCase = createUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.getProfileUseCase = getProfileUseCase;
        this.adminUpdateUserUseCase = adminUpdateUserUseCase;
        this.adminDeactivateUserUseCase = adminDeactivateUserUseCase;
        this.requestAdminEmailChangeUseCase = requestAdminEmailChangeUseCase;
        this.emailChangeMailer = emailChangeMailer;
        this.emailChangeTokenRepositoryPort = emailChangeTokenRepositoryPort;
        this.clock = clock;
    }

    @PostMapping
    @Operation(summary = "Register a user")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        final User user = createUserUseCase.execute(
                request.name(), request.email(), request.password(), request.userType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(UserResponse.from(user), "User created successfully"));
    }

    @GetMapping
    @Operation(summary = "List users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        final List<UserResponse> users = listUsersUseCase.execute().stream()
                .map(user -> UserResponse.from(user, listUsersUseCase.resolveProfile(user)))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by id, including registration fields for that profile")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(detailResponse(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a user's registration data (admin)")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProfileUpdateRequest request) {
        final AuthenticatedUser actor = AuthenticationContext.requireUser();
        adminUpdateUserUseCase.execute(
                actor.id(), id, request.name(), request.phone(), request.companyName());
        return ResponseEntity.ok(ApiResponse.success(
                detailResponse(id), "User updated successfully"));
    }

    @PostMapping("/{id}/email-change")
    @Operation(
            summary = "Request an email change for a user (admin)",
            description = "Sends a confirmation link to the new e-mail. The current e-mail stays "
                    + "active until the link is confirmed."
    )
    public ResponseEntity<ApiResponse<UserResponse>> requestEmailChange(
            @PathVariable UUID id,
            @Valid @RequestBody RequestEmailChangeRequest request) {
        final AuthenticatedUser actor = AuthenticationContext.requireUser();
        final var command = requestAdminEmailChangeUseCase.execute(actor.id(), id, request.email());
        emailChangeMailer.send(command.name(), command.newEmail(), command.token());
        return ResponseEntity.ok(ApiResponse.success(
                detailResponse(id),
                "Confirmation e-mail sent. The address will change after confirmation."));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Block a user by setting status to INACTIVE (admin)")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(@PathVariable UUID id) {
        final AuthenticatedUser actor = AuthenticationContext.requireUser();
        adminDeactivateUserUseCase.execute(actor.id(), id);
        return ResponseEntity.ok(ApiResponse.success(
                detailResponse(id), "User deactivated successfully"));
    }

    private UserResponse detailResponse(UUID id) {
        final User user = getUserByIdUseCase.execute(id);
        final UserProfile profile = getProfileUseCase.execute(id);
        final String pendingEmail = emailChangeTokenRepositoryPort
                .findOpenByUserId(id, LocalDateTime.now(clock))
                .map(token -> token.getNewEmail())
                .orElse(null);
        return UserResponse.from(user, profile, pendingEmail);
    }
}
