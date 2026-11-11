package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.AccountInviteResponse;
import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CreateInvitePasswordRequest;
import com.escapa.backend.adapters.dto.LoginResponse;
import com.escapa.backend.application.usecase.CreateInvitePasswordUseCase;
import com.escapa.backend.application.usecase.GetAccountInviteUseCase;
import com.escapa.backend.application.usecase.LoginOutput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/invites")
@Tag(name = "Authentication - Invites", description = "Password creation by the employee invite link")
public class AccountInviteController {

    private final GetAccountInviteUseCase getAccountInviteUseCase;
    private final CreateInvitePasswordUseCase createInvitePasswordUseCase;

    public AccountInviteController(
            GetAccountInviteUseCase getAccountInviteUseCase,
            CreateInvitePasswordUseCase createInvitePasswordUseCase
    ) {
        this.getAccountInviteUseCase = getAccountInviteUseCase;
        this.createInvitePasswordUseCase = createInvitePasswordUseCase;
    }

    @GetMapping("/{token}")
    @Operation(
            summary = "Get the invite of the employee",
            description = "Unknown or used token returns 404; expired token returns 410."
    )
    public ResponseEntity<ApiResponse<AccountInviteResponse>> getInvite(@PathVariable String token) {
        final AccountInviteResponse response = AccountInviteResponse.from(getAccountInviteUseCase.execute(token));
        return ResponseEntity.ok(ApiResponse.success(response, "Operation completed successfully"));
    }

    @PostMapping("/{token}/password")
    @Operation(
            summary = "Create the password of the invited employee",
            description = "Activates the account, invalidates the invite and returns the access token, "
                    + "in the same format as the login. Unknown or used token returns 404; expired token returns 410."
    )
    public ResponseEntity<ApiResponse<LoginResponse>> createPassword(
            @PathVariable String token,
            @Valid @RequestBody CreateInvitePasswordRequest request
    ) {
        final LoginOutput output = createInvitePasswordUseCase.execute(token, request.password());
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(output), "Operation completed successfully"));
    }
}
