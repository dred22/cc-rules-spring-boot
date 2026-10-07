package com.mk.ecommerce.service;

import com.mk.ecommerce.dto.CategoryRequest;
import com.mk.ecommerce.dto.CategoryResponse;
import com.mk.ecommerce.entity.Category;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.DuplicateResourceException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.repository.CategoryRepository;
import com.mk.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return CategoryResponse.from(getCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category name already exists: " + request.getName());
        }
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Category slug already exists: " + request.getSlug());
        }
        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        if (!category.getName().equals(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Category name already exists: " + request.getName());
        }
        if (!category.getSlug().equals(request.getSlug()) && categoryRepository.existsBySlug(request.getSlug())) {
            throw new DuplicateResourceException("Category slug already exists: " + request.getSlug());
        }
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setDescription(request.getDescription());
        return CategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("Cannot delete category with existing products: " + id);
        }
        categoryRepository.delete(category);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }
}
