package com.escapa.backend.adapters.dto;

import com.escapa.backend.application.model.CompanySeatsSummary;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Seats contracted and in use by the company. Every linked employee takes a seat, "
        + "whatever the status; only unlinking the employee frees it.")
public record CompanySeatsResponse(
        @Schema(description = "Seats contracted by the company", example = "10")
        int seatsTotal,
        @Schema(description = "Seats taken, one per linked employee", example = "9")
        int seatsUsed,
        @Schema(description = "Seats still free; never negative", example = "1")
        int seatsAvailable,
        @Schema(description = "Linked employees of any status", example = "9")
        int employeesTotal,
        @Schema(description = "Linked employees whose status is ACTIVE", example = "6")
        int employeesActive
) {

    public static CompanySeatsResponse from(CompanySeatsSummary summary) {
        return new CompanySeatsResponse(
                summary.seatsTotal(),
                summary.seatsUsed(),
                summary.seatsAvailable(),
                summary.employeesTotal(),
                summary.employeesActive());
    }
}
