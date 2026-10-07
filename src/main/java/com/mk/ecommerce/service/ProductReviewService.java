package com.mk.ecommerce.service;

import com.mk.ecommerce.dto.ReviewRequest;
import com.mk.ecommerce.dto.ReviewResponse;
import com.mk.ecommerce.entity.Customer;
import com.mk.ecommerce.entity.Product;
import com.mk.ecommerce.entity.ProductReview;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.repository.CustomerRepository;
import com.mk.ecommerce.repository.ProductRepository;
import com.mk.ecommerce.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public ReviewResponse createReview(Long productId, ReviewRequest request) {
        Product product = getProduct(productId);
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));

        ProductReview review = ProductReview.builder()
                .product(product)
                .customer(customer)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> listReviews(Long productId) {
        getProduct(productId);
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }
}
