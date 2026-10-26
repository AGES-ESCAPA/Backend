package com.escapa.backend.adapters.controller;

import com.escapa.backend.adapters.dto.ApiResponse;
import com.escapa.backend.adapters.dto.CreateOrderRequest;
import com.escapa.backend.adapters.dto.OrderResponse;
import com.escapa.backend.adapters.dto.SeatPriceResponse;
import com.escapa.backend.adapters.security.AuthenticatedUser;
import com.escapa.backend.adapters.security.AuthenticationContext;
import com.escapa.backend.application.model.CreateOrderCommand;
import com.escapa.backend.application.usecase.CreateOrderUseCase;
import com.escapa.backend.application.usecase.GetSeatPriceUseCase;
import com.escapa.backend.domain.entity.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Orders", description = "Purchases of courses and seats by students and companies")
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetSeatPriceUseCase getSeatPriceUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase, GetSeatPriceUseCase getSeatPriceUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getSeatPriceUseCase = getSeatPriceUseCase;
    }

    @GetMapping("/seat-price")
    @Operation(
            summary = "Get the price of one corporate seat",
            description = "Lets the checkout show the total of a seats package before the purchase. The order "
                    + "itself is still calculated by the server with this same value.")
    public ResponseEntity<ApiResponse<SeatPriceResponse>> getSeatPrice() {
        return ResponseEntity.ok(ApiResponse.success(SeatPriceResponse.from(getSeatPriceUseCase.execute())));
    }

    @PostMapping
    @Operation(
            summary = "Place a purchase and release the access",
            description = "Registers the simulated purchase as APPROVED and releases the access in the same "
                    + "transaction. A student buys a course; a company buys a course or seats. Only CARD is "
                    + "accepted for now. The amount is calculated by the server.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order approved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Invalid request, payment method other than CARD, course not published "
                            + "or a profile that cannot buy the item"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "The buyer already has active access to the course")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        final AuthenticatedUser user = AuthenticationContext.requireUser();

        final Order order = createOrderUseCase.execute(new CreateOrderCommand(
                user.id(),
                user.role(),
                request.itemType(),
                request.courseId(),
                request.seatsQuantity(),
                request.paymentMethod(),
                request.installments()));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(OrderResponse.from(order)));
    }
}
