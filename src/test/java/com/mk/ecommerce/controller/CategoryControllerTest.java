package com.mk.ecommerce.controller;

import com.mk.ecommerce.dto.CategoryRequest;
import com.mk.ecommerce.dto.CategoryResponse;
import com.mk.ecommerce.exception.AccessDeniedException;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.DuplicateResourceException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    private static final String BASE_URL = "/api/v1/categories";

    private static final String VALID_BODY = """
            {
              "name": "Electronics",
              "slug": "electronics",
              "description": "Gadgets and devices"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void should_return_200_with_categories_when_categories_exist() throws Exception {
        // Arrange
        when(categoryService.findAll()).thenReturn(List.of(response(1L, "Electronics", "electronics")));

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].slug").value("electronics"));
    }

    @Test
    void should_return_200_with_empty_array_when_no_categories_exist() throws Exception {
        // Arrange
        when(categoryService.findAll()).thenReturn(List.of());

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void should_return_200_with_category_when_id_exists() throws Exception {
        // Arrange
        when(categoryService.findById(1L)).thenReturn(response(1L, "Electronics", "electronics"));

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", 1L));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.slug").value("electronics"))
                .andExpect(jsonPath("$.description").value("Gadgets and devices"));
    }

    @Test
    void should_return_404_when_category_id_does_not_exist() throws Exception {
        // Arrange
        when(categoryService.findById(99L)).thenThrow(new ResourceNotFoundException("Category not found: 99"));

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", 99L));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Category not found: 99"));
    }

    @Test
    void should_return_400_when_path_id_is_not_numeric() throws Exception {
        // Arrange
        // No stubbing: the path variable fails conversion before the service is reached.

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL + "/abc"));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void should_return_403_when_service_denies_access() throws Exception {
        // Arrange
        when(categoryService.findById(1L)).thenThrow(new AccessDeniedException("Access denied to category"));

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", 1L));

        // Assert
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void should_return_201_with_created_category_when_request_is_valid() throws Exception {
        // Arrange
        when(categoryService.create(any(CategoryRequest.class)))
                .thenReturn(response(1L, "Electronics", "electronics"));

        // Act
        ResultActions result = mockMvc.perform(
                post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void should_delegate_request_fields_to_service_when_creating_category() throws Exception {
        // Arrange
        when(categoryService.create(any(CategoryRequest.class)))
                .thenReturn(response(1L, "Electronics", "electronics"));
        CategoryRequest expected = request("Electronics", "electronics", "Gadgets and devices");

        // Act
        mockMvc.perform(post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        // Assert
        verify(categoryService).create(eq(expected));
    }

    @Test
    void should_return_400_with_field_errors_when_name_and_slug_are_missing() throws Exception {
        // Arrange
        String body = """
                {
                  "description": "No name or slug"
                }
                """;

        // Act
        ResultActions result = mockMvc.perform(
                post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("name", "slug")));
    }

    @Test
    void should_return_400_with_field_error_when_name_is_blank() throws Exception {
        // Arrange
        String body = """
                {
                  "name": "   ",
                  "slug": "electronics"
                }
                """;

        // Act
        ResultActions result = mockMvc.perform(
                post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("name")));
    }

    @Test
    void should_return_400_with_field_error_when_name_exceeds_100_characters() throws Exception {
        // Arrange
        String body = """
                {
                  "name": "%s",
                  "slug": "electronics"
                }
                """.formatted("a".repeat(101));

        // Act
        ResultActions result = mockMvc.perform(
                post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("name")));
    }

    @Test
    void should_return_400_with_field_error_when_slug_exceeds_100_characters() throws Exception {
        // Arrange
        String body = """
                {
                  "name": "Electronics",
                  "slug": "%s"
                }
                """.formatted("s".repeat(101));

        // Act
        ResultActions result = mockMvc.perform(
                post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("slug")));
    }

    @Test
    void should_return_400_when_request_body_is_malformed_json() throws Exception {
        // Arrange
        String malformed = "{ \"name\": \"Electronics\", ";

        // Act
        ResultActions result = mockMvc.perform(
                post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(malformed));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void should_return_409_when_category_name_already_exists() throws Exception {
        // Arrange
        when(categoryService.create(any(CategoryRequest.class)))
                .thenThrow(new DuplicateResourceException("Category name already exists: Electronics"));

        // Act
        ResultActions result = mockMvc.perform(
                post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        // Assert
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void should_return_409_when_category_slug_already_exists() throws Exception {
        // Arrange
        when(categoryService.create(any(CategoryRequest.class)))
                .thenThrow(new DuplicateResourceException("Category slug already exists: electronics"));

        // Act
        ResultActions result = mockMvc.perform(
                post(BASE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        // Assert
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category slug already exists: electronics"));
    }

    @Test
    void should_return_200_with_updated_category_when_request_is_valid() throws Exception {
        // Arrange
        when(categoryService.update(eq(1L), any(CategoryRequest.class)))
                .thenReturn(response(1L, "Electronics", "electronics"));

        // Act
        ResultActions result = mockMvc.perform(
                put(BASE_URL + "/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void should_delegate_id_and_request_fields_to_service_when_updating_category() throws Exception {
        // Arrange
        when(categoryService.update(eq(1L), any(CategoryRequest.class)))
                .thenReturn(response(1L, "Electronics", "electronics"));
        CategoryRequest expected = request("Electronics", "electronics", "Gadgets and devices");

        // Act
        mockMvc.perform(put(BASE_URL + "/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        // Assert
        verify(categoryService).update(eq(1L), eq(expected));
    }

    @Test
    void should_return_400_with_field_errors_when_update_body_is_invalid() throws Exception {
        // Arrange
        String body = """
                {
                  "name": "",
                  "slug": ""
                }
                """;

        // Act
        ResultActions result = mockMvc.perform(
                put(BASE_URL + "/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("name", "slug")));
    }

    @Test
    void should_return_404_when_updating_missing_category() throws Exception {
        // Arrange
        when(categoryService.update(eq(99L), any(CategoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category not found: 99"));

        // Act
        ResultActions result = mockMvc.perform(
                put(BASE_URL + "/{id}", 99L).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found: 99"));
    }

    @Test
    void should_return_409_when_updating_to_existing_category_name() throws Exception {
        // Arrange
        when(categoryService.update(eq(1L), any(CategoryRequest.class)))
                .thenThrow(new DuplicateResourceException("Category name already exists: Electronics"));

        // Act
        ResultActions result = mockMvc.perform(
                put(BASE_URL + "/{id}", 1L).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        // Assert
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void should_return_400_when_put_path_id_is_not_numeric() throws Exception {
        // Arrange
        // No stubbing: the path variable fails conversion before the service is reached.

        // Act
        ResultActions result = mockMvc.perform(
                put(BASE_URL + "/abc").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void should_return_204_when_category_is_deleted() throws Exception {
        // Arrange
        // No stubbing: service.delete is void and returns normally by default.

        // Act
        ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", 1L));

        // Assert
        result.andExpect(status().isNoContent());
    }

    @Test
    void should_delegate_id_to_service_when_deleting_category() throws Exception {
        // Arrange
        // No stubbing: service.delete is void and returns normally by default.

        // Act
        mockMvc.perform(delete(BASE_URL + "/{id}", 1L));

        // Assert
        verify(categoryService).delete(1L);
    }

    @Test
    void should_return_404_when_deleting_missing_category() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Category not found: 99")).when(categoryService).delete(99L);

        // Act
        ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", 99L));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found: 99"));
    }

    @Test
    void should_return_422_when_deleting_category_with_products() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Cannot delete category with existing products: 1"))
                .when(categoryService).delete(1L);

        // Act
        ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", 1L));

        // Assert
        result.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void should_return_400_when_delete_path_id_is_not_numeric() throws Exception {
        // Arrange
        // No stubbing: the path variable fails conversion before the service is reached.

        // Act
        ResultActions result = mockMvc.perform(delete(BASE_URL + "/abc"));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    private CategoryResponse response(Long id, String name, String slug) {
        return CategoryResponse.builder()
                .id(id)
                .name(name)
                .slug(slug)
                .description("Gadgets and devices")
                .build();
    }

    private CategoryRequest request(String name, String slug, String description) {
        return CategoryRequest.builder()
                .name(name)
                .slug(slug)
                .description(description)
                .build();
    }
}
