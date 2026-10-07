package com.mk.ecommerce.controller;

import com.mk.ecommerce.dto.OrderRequest;
import com.mk.ecommerce.dto.OrderResponse;
import com.mk.ecommerce.dto.OrderStatusUpdateRequest;
import com.mk.ecommerce.entity.OrderStatus;
import com.mk.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public List<OrderResponse> findAll(@RequestParam(required = false) Long customerId,
                                       @RequestParam(required = false) OrderStatus status) {
        return orderService.findAll(customerId, status);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse placeOrder(@Valid @RequestBody OrderRequest request) {
        return orderService.placeOrder(request);
    }

    @PutMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateStatus(id, request.getStatus());
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }
}
