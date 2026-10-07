package com.mk.ecommerce.service;

import com.mk.ecommerce.dto.ProductRequest;
import com.mk.ecommerce.dto.ProductResponse;
import com.mk.ecommerce.entity.Category;
import com.mk.ecommerce.entity.Product;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.DuplicateResourceException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.repository.CategoryRepository;
import com.mk.ecommerce.repository.OrderItemRepository;
import com.mk.ecommerce.repository.ProductRepository;
import com.mk.ecommerce.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(Long categoryId) {
        List<Product> products = categoryId == null
                ? productRepository.findAll()
                : productRepository.findByCategoryId(categoryId, Pageable.unpaged()).getContent();
        return products.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return ProductResponse.from(getProduct(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product SKU already exists: " + request.getSku());
        }
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .sku(request.getSku())
                .stockQuantity(request.getStockQuantity())
                .active(request.getActive() == null || request.getActive())
                .category(getCategory(request.getCategoryId()))
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product SKU already exists: " + request.getSku());
        }
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setSku(request.getSku());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }
        product.setCategory(getCategory(request.getCategoryId()));
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = getProduct(id);
        if (orderItemRepository.existsByProductId(id)) {
            throw new BusinessRuleException("Cannot delete product referenced by orders: " + id);
        }
        reviewRepository.deleteByProductId(id);
        productRepository.delete(product);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
