package com.example.store.service;

import com.example.store.dto.ProductDTO;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.mapper.ProductMapper;
import com.example.store.repository.ProductRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Given products exist, "
            + "When getAllProducts is called, "
            + "Then the repository results are mapped and returned")
    void testGivenProductsExist_whenGetAllProducts_thenReturnsMappedProducts() {
        // Given
        ProductDTO dto = new ProductDTO();
        dto.setId(1L);
        dto.setDescription("Widget");
        dto.setOrders(List.of(10L));
        when(productRepository.findAll()).thenReturn(List.of(new Product()));
        when(productMapper.productsToProductDTOs(anyList())).thenReturn(List.of(dto));

        // When
        List<ProductDTO> result = productService.getAllProducts();

        // Then
        assertThat(result).extracting(ProductDTO::getDescription).containsExactly("Widget");
        assertThat(result).flatExtracting(ProductDTO::getOrders).containsExactly(10L);
        verify(productRepository).findAll();
    }

    @Test
    @DisplayName("Given a product exists for the id, "
            + "When getProduct is called, "
            + "Then the mapped product is returned with its containing order IDs")
    void testGivenProductExists_whenGetProduct_thenReturnsMappedProduct() {
        // Given
        Product product = new Product();
        product.setId(1L);
        ProductDTO dto = new ProductDTO();
        dto.setId(1L);
        dto.setDescription("Widget");
        dto.setOrders(List.of(10L, 20L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.productToProductDTO(product)).thenReturn(dto);

        // When
        ProductDTO result = productService.getProduct(1L);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrders()).containsExactly(10L, 20L);
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("Given no product exists for the id, "
            + "When getProduct is called, "
            + "Then a ProductNotFoundException is thrown")
    void testGivenNoProduct_whenGetProduct_thenThrowsNotFound() {
        // Given
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> productService.getProduct(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Given a product to create, "
            + "When createProduct is called, "
            + "Then the product is saved and the mapped result is returned")
    void testGivenProduct_whenCreateProduct_thenSavesAndReturnsMapped() {
        // Given
        Product product = new Product();
        product.setDescription("Widget");
        product.setOrders(List.of(new Order()));
        ProductDTO dto = new ProductDTO();
        dto.setId(1L);
        dto.setDescription("Widget");
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.productToProductDTO(product)).thenReturn(dto);

        // When
        ProductDTO result = productService.createProduct(product);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        verify(productRepository).save(product);
    }
}
