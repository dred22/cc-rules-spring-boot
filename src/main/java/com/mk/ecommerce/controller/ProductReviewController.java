package com.mk.ecommerce.controller;

import com.mk.ecommerce.dto.ReviewRequest;
import com.mk.ecommerce.dto.ReviewResponse;
import com.mk.ecommerce.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;

    @GetMapping
    public List<ReviewResponse> listReviews(@PathVariable Long productId) {
        return reviewService.listReviews(productId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@PathVariable Long productId, @Valid @RequestBody ReviewRequest request) {
        return reviewService.createReview(productId, request);
    }
}
