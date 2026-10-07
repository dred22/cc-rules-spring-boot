package com.mk.ecommerce.repository;

import com.mk.ecommerce.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);

    void deleteByProductId(Long productId);

    void deleteByCustomerId(Long customerId);
}
