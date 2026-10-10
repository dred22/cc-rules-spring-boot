package com.mk.ecommerce.controller;

import com.mk.ecommerce.dto.ProductRequest;
import com.mk.ecommerce.dto.ProductResponse;
import com.mk.ecommerce.exception.AccessDeniedException;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.DuplicateResourceException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String BASE_URL = "/api/v1/products";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private ProductService productService;

    // ---------- GET /api/v1/products ----------

    @Test
    void should_return_200_with_products_when_no_category_filter() throws Exception {
        // Arrange
        when(productService.findAll(null)).thenReturn(List.of(
                productResponse(1L, "Wireless Mouse"),
                productResponse(2L, "Keyboard")));

        // Act & Assert
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"));
    }

    @Test
    void should_call_service_with_null_category_when_no_category_param() throws Exception {
        // Arrange
        // (service returns an empty list by default)

        // Act
        mockMvc.perform(get(BASE_URL));

        // Assert
        verify(productService).findAll(null);
    }

    @Test
    void should_return_200_with_matching_products_when_category_id_given() throws Exception {
        // Arrange
        when(productService.findAll(5L)).thenReturn(List.of(productResponse(1L, "Wireless Mouse")));

        // Act & Assert
        mockMvc.perform(get(BASE_URL).param("categoryId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void should_call_service_with_category_id_when_category_param_given() throws Exception {
        // Arrange
        // (service returns an empty list by default)

        // Act
        mockMvc.perform(get(BASE_URL).param("categoryId", "5"));

        // Assert
        verify(productService).findAll(5L);
    }

    @Test
    void should_return_empty_array_when_no_products_match_category() throws Exception {
        // Arrange
        when(productService.findAll(99L)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get(BASE_URL).param("categoryId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void should_return_400_when_category_id_is_not_numeric() throws Exception {
        // Arrange
        // (no service interaction expected)

        // Act & Assert
        mockMvc.perform(get(BASE_URL).param("categoryId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'categoryId'"));
    }

    // ---------- GET /api/v1/products/{id} ----------

    @Test
    void should_return_200_with_product_when_id_exists() throws Exception {
        // Arrange
        when(productService.findById(1L)).thenReturn(productResponse(1L, "Wireless Mouse"));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.sku").value("ELEC-MOU-001"))
                .andExpect(jsonPath("$.price").value(24.99))
                .andExpect(jsonPath("$.categoryName").value("Electronics"));
    }

    @Test
    void should_call_service_with_path_id_when_get_by_id() throws Exception {
        // Arrange
        when(productService.findById(7L)).thenReturn(productResponse(7L, "Monitor"));

        // Act
        mockMvc.perform(get(BASE_URL + "/7"));

        // Assert
        verify(productService).findById(7L);
    }

    @Test
    void should_return_404_when_product_not_found_by_id() throws Exception {
        // Arrange
        when(productService.findById(99L)).thenThrow(new ResourceNotFoundException("Product not found: 99"));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found: 99"));
    }

    @Test
    void should_return_400_when_path_id_is_not_numeric_on_get() throws Exception {
        // Arrange
        // (no service interaction expected)

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void should_return_403_when_service_denies_access_on_get() throws Exception {
        // Arrange
        when(productService.findById(1L)).thenThrow(new AccessDeniedException("Access denied"));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    // ---------- POST /api/v1/products ----------

    @Test
    void should_return_201_with_created_product_when_request_valid() throws Exception {
        // Arrange
        when(productService.create(any(ProductRequest.class)))
                .thenReturn(productResponse(10L, "Wireless Mouse"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"));
    }

    @Test
    void should_pass_request_body_to_service_when_create_valid() throws Exception {
        // Arrange
        when(productService.create(any(ProductRequest.class)))
                .thenReturn(productResponse(10L, "Wireless Mouse"));

        // Act
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validRequest())));

        // Assert
        verify(productService).create(validRequest());
    }

    @Test
    void should_return_400_with_field_errors_when_create_body_is_empty_object() throws Exception {
        // Arrange
        String body = "{}";

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field")
                        .value(containsInAnyOrder("name", "sku", "price", "stockQuantity", "categoryId")));
    }

    @Test
    void should_return_400_with_price_field_error_when_price_is_zero() throws Exception {
        // Arrange
        ProductRequest request = validRequest();
        request.setPrice(new BigDecimal("0.00"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
    }

    @Test
    void should_return_400_with_price_field_error_when_price_is_negative() throws Exception {
        // Arrange
        ProductRequest request = validRequest();
        request.setPrice(new BigDecimal("-1.00"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
    }

    @Test
    void should_return_400_with_price_field_error_when_price_has_too_many_fraction_digits() throws Exception {
        // Arrange
        ProductRequest request = validRequest();
        request.setPrice(new BigDecimal("9.999"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
    }

    @Test
    void should_return_400_with_price_field_error_when_price_exceeds_integer_digits() throws Exception {
        // Arrange
        ProductRequest request = validRequest();
        request.setPrice(new BigDecimal("123456789.00"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
    }

    @Test
    void should_return_400_with_stock_field_error_when_stock_quantity_is_negative() throws Exception {
        // Arrange
        ProductRequest request = validRequest();
        request.setStockQuantity(-1);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("stockQuantity"));
    }

    @Test
    void should_return_400_with_name_field_error_when_name_exceeds_max_length() throws Exception {
        // Arrange
        ProductRequest request = validRequest();
        request.setName("A".repeat(201));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void should_return_400_with_sku_field_error_when_sku_is_blank() throws Exception {
        // Arrange
        ProductRequest request = validRequest();
        request.setSku("   ");

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("sku"));
    }

    @Test
    void should_return_400_when_create_body_is_malformed_json() throws Exception {
        // Arrange
        String body = "{\"name\": \"Wireless Mouse\", \"price\": ";

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void should_return_404_when_create_references_missing_category() throws Exception {
        // Arrange
        when(productService.create(any(ProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Category not found: 2"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category not found: 2"));
    }

    @Test
    void should_return_409_when_create_sku_already_exists() throws Exception {
        // Arrange
        when(productService.create(any(ProductRequest.class)))
                .thenThrow(new DuplicateResourceException("Product SKU already exists: ELEC-MOU-001"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ---------- PUT /api/v1/products/{id} ----------

    @Test
    void should_return_200_with_updated_product_when_request_valid() throws Exception {
        // Arrange
        when(productService.update(eq(1L), any(ProductRequest.class)))
                .thenReturn(productResponse(1L, "Wireless Mouse v2"));

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Mouse v2"));
    }

    @Test
    void should_call_service_with_path_id_and_body_when_update_valid() throws Exception {
        // Arrange
        when(productService.update(anyLong(), any(ProductRequest.class)))
                .thenReturn(productResponse(3L, "Wireless Mouse"));

        // Act
        mockMvc.perform(put(BASE_URL + "/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(validRequest())));

        // Assert
        verify(productService).update(3L, validRequest());
    }

    @Test
    void should_return_400_with_name_field_error_when_update_body_has_blank_name() throws Exception {
        // Arrange
        ProductRequest request = validRequest();
        request.setName("");

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void should_return_404_when_updating_missing_product() throws Exception {
        // Arrange
        when(productService.update(eq(99L), any(ProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product not found: 99"));

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: 99"));
    }

    @Test
    void should_return_409_when_update_sku_belongs_to_another_product() throws Exception {
        // Arrange
        when(productService.update(anyLong(), any(ProductRequest.class)))
                .thenThrow(new DuplicateResourceException("Product SKU already exists: ELEC-MOU-001"));

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void should_return_400_when_path_id_is_not_numeric_on_update() throws Exception {
        // Arrange
        // (no service interaction expected)

        // Act & Assert
        mockMvc.perform(put(BASE_URL + "/abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    // ---------- DELETE /api/v1/products/{id} ----------

    @Test
    void should_return_204_when_product_deleted() throws Exception {
        // Arrange
        // (service delete is a no-op by default)

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/4"))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_call_service_with_path_id_when_delete() throws Exception {
        // Arrange
        // (service delete is a no-op by default)

        // Act
        mockMvc.perform(delete(BASE_URL + "/4"));

        // Assert
        verify(productService).delete(4L);
    }

    @Test
    void should_return_404_when_deleting_missing_product() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Product not found: 99")).when(productService).delete(99L);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found: 99"));
    }

    @Test
    void should_return_422_when_product_is_referenced_by_orders() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Cannot delete product referenced by orders: 4"))
                .when(productService).delete(4L);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/4"))
                .andExpect(status().is(HttpStatus.UNPROCESSABLE_CONTENT.value()))
                .andExpect(jsonPath("$.message").value("Cannot delete product referenced by orders: 4"));
    }

    @Test
    void should_return_400_when_path_id_is_not_numeric_on_delete() throws Exception {
        // Arrange
        // (no service interaction expected)

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    // ---------- helpers ----------

    private String json(Object value) {
        return jsonMapper.writeValueAsString(value);
    }

    private static ProductRequest validRequest() {
        return ProductRequest.builder()
                .name("Wireless Mouse")
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("24.99"))
                .sku("ELEC-MOU-001")
                .stockQuantity(10)
                .active(true)
                .categoryId(2L)
                .build();
    }

    private static ProductResponse productResponse(Long id, String name) {
        return ProductResponse.builder()
                .id(id)
                .name(name)
                .description("Ergonomic wireless mouse")
                .price(new BigDecimal("24.99"))
                .sku("ELEC-MOU-001")
                .stockQuantity(10)
                .active(true)
                .categoryId(2L)
                .categoryName("Electronics")
                .build();
    }
}
