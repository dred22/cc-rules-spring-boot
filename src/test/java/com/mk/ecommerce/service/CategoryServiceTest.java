package com.mk.ecommerce.service;

import com.mk.ecommerce.dto.CategoryRequest;
import com.mk.ecommerce.dto.CategoryResponse;
import com.mk.ecommerce.entity.Category;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.DuplicateResourceException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.repository.CategoryRepository;
import com.mk.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void should_return_all_categories_when_categories_exist() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(List.of(category(1L, "Books", "books")));

        // Act
        List<CategoryResponse> result = categoryService.findAll();

        // Assert
        assertThat(result).extracting(CategoryResponse::getSlug).containsExactly("books");
    }

    @Test
    void should_throw_not_found_when_category_id_does_not_exist() {
        // Arrange
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void should_create_category_when_name_and_slug_are_unique() {
        // Arrange
        CategoryRequest request = request("Books", "books");
        when(categoryRepository.existsByName("Books")).thenReturn(false);
        when(categoryRepository.existsBySlug("books")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CategoryResponse result = categoryService.create(request);

        // Assert
        assertThat(result.getName()).isEqualTo("Books");
    }

    @Test
    void should_throw_duplicate_when_category_name_already_exists() {
        // Arrange
        when(categoryRepository.existsByName("Books")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.create(request("Books", "books")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("name");
    }

    @Test
    void should_throw_duplicate_when_category_slug_already_exists() {
        // Arrange
        when(categoryRepository.existsByName("Books")).thenReturn(false);
        when(categoryRepository.existsBySlug("books")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.create(request("Books", "books")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("slug");
    }

    @Test
    void should_update_category_when_new_values_are_unique() {
        // Arrange
        Category existing = category(1L, "Books", "books");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.existsByName("Novels")).thenReturn(false);
        when(categoryRepository.existsBySlug("novels")).thenReturn(false);

        // Act
        CategoryResponse result = categoryService.update(1L, request("Novels", "novels"));

        // Assert
        assertThat(result.getName()).isEqualTo("Novels");
    }

    @Test
    void should_throw_duplicate_when_updating_to_name_used_by_another_category() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Books", "books")));
        when(categoryRepository.existsByName("Clothing")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.update(1L, request("Clothing", "books")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void should_throw_business_rule_when_deleting_category_with_products() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Books", "books")));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_delete_category_when_it_has_no_products() {
        // Arrange
        Category existing = category(1L, "Books", "books");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        // Act
        categoryService.delete(1L);

        // Assert
        verify(categoryRepository).delete(existing);
    }

    private Category category(Long id, String name, String slug) {
        return Category.builder().id(id).name(name).slug(slug).build();
    }

    private CategoryRequest request(String name, String slug) {
        return CategoryRequest.builder().name(name).slug(slug).build();
    }
}
