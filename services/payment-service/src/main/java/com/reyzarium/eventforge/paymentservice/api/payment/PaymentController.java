package com.reyzarium.eventforge.paymentservice.api.payment;

import com.reyzarium.eventforge.paymentservice.api.payment.dto.CreatePaymentRequest;
import com.reyzarium.eventforge.paymentservice.api.payment.dto.CreatePaymentResponse;
import com.reyzarium.eventforge.paymentservice.api.payment.dto.PaymentResponse;
import com.reyzarium.eventforge.paymentservice.application.service.PaymentCommandService;
import com.reyzarium.eventforge.paymentservice.application.service.PaymentQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentCommandService paymentCommandService;
    private final PaymentQueryService paymentQueryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse createPayment(@RequestHeader("X-User-Id") UUID userId,
                                               @RequestHeader("Idempotency-Key") String idempotencyKey,
                                               @Valid @RequestBody CreatePaymentRequest request) {
        return paymentCommandService.createPayment(userId, idempotencyKey, request);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@RequestHeader("X-User-Id") UUID userId,
                                      @PathVariable UUID paymentId) {
        return paymentQueryService.getPayment(userId, paymentId);
    }
}
