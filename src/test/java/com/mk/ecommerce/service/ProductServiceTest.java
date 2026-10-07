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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void should_return_all_products_when_no_category_filter_given() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of(product(1L, "SKU-1")));

        // Act
        List<ProductResponse> result = productService.findAll(null);

        // Assert
        assertThat(result).extracting(ProductResponse::getSku).containsExactly("SKU-1");
    }

    @Test
    void should_throw_not_found_when_product_id_does_not_exist() {
        // Arrange
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void should_create_product_when_sku_is_unique_and_category_exists() {
        // Arrange
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductResponse result = productService.create(request("SKU-1", null));

        // Assert
        assertThat(result.getSku()).isEqualTo("SKU-1");
    }

    @Test
    void should_default_active_to_true_when_active_is_omitted_on_create() {
        // Arrange
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category()));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ProductResponse result = productService.create(request("SKU-1", null));

        // Assert
        assertThat(result.getActive()).isTrue();
    }

    @Test
    void should_throw_duplicate_when_product_sku_already_exists() {
        // Arrange
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.create(request("SKU-1", true)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU-1");
    }

    @Test
    void should_throw_not_found_when_creating_product_for_missing_category() {
        // Arrange
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.create(request("SKU-1", true)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    void should_update_product_when_values_are_valid() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "SKU-1")));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category()));

        // Act
        ProductResponse result = productService.update(1L, request("SKU-1", false));

        // Assert
        assertThat(result.getActive()).isFalse();
    }

    @Test
    void should_throw_duplicate_when_updating_to_sku_used_by_another_product() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "SKU-1")));
        when(productRepository.existsBySku("SKU-2")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.update(1L, request("SKU-2", true)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void should_throw_business_rule_when_deleting_product_referenced_by_orders() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product(1L, "SKU-1")));
        when(orderItemRepository.existsByProductId(1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.delete(1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_delete_product_when_not_referenced_by_orders() {
        // Arrange
        Product existing = product(1L, "SKU-1");
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderItemRepository.existsByProductId(1L)).thenReturn(false);

        // Act
        productService.delete(1L);

        // Assert
        verify(productRepository).delete(existing);
    }

    private Category category() {
        return Category.builder().id(1L).name("Books").slug("books").build();
    }

    private Product product(Long id, String sku) {
        return Product.builder()
                .id(id)
                .name("Product " + sku)
                .sku(sku)
                .price(new BigDecimal("10.00"))
                .stockQuantity(5)
                .active(true)
                .category(category())
                .build();
    }

    private ProductRequest request(String sku, Boolean active) {
        return ProductRequest.builder()
                .name("Product " + sku)
                .price(new BigDecimal("10.00"))
                .sku(sku)
                .stockQuantity(5)
                .active(active)
                .categoryId(1L)
                .build();
    }
}
