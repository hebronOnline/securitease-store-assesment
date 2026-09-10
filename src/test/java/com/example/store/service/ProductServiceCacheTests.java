package com.example.store.service;

import com.example.store.config.CacheConfig;
import com.example.store.dto.ProductDTO;
import com.example.store.entity.Product;
import com.example.store.mapper.ProductMapper;
import com.example.store.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class ProductServiceCacheTests {

    @TestConfiguration
    @EnableCaching
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheConfig.ORDERS_CACHE, CacheConfig.CUSTOMERS_CACHE, CacheConfig.PRODUCTS_CACHE);
        }

        @Bean
        ProductService productService(ProductRepository repository, ProductMapper mapper) {
            return new ProductService(repository, mapper);
        }
    }

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductMapper productMapper;

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        reset(productRepository, productMapper);
    }

    @Test
    @DisplayName("Given getProduct called twice for the same id, "
            + "When the second call runs, "
            + "Then the repository is queried only once (cache hit)")
    void testGetProductById_isCachedById() {
        // Given
        Product product = new Product();
        product.setId(1L);
        ProductDTO dto = new ProductDTO();
        dto.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.productToProductDTO(product)).thenReturn(dto);

        // When
        ProductDTO first = productService.getProduct(1L);
        ProductDTO second = productService.getProduct(1L);

        // Then
        assertThat(first).isSameAs(second);
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Given getAllProducts called twice, "
            + "When the second call runs, "
            + "Then the repository is queried only once (cache hit)")
    void testGetAllProducts_isCached() {
        // Given
        when(productRepository.findAll()).thenReturn(List.of(new Product()));
        when(productMapper.productsToProductDTOs(anyList())).thenReturn(List.of(new ProductDTO()));

        // When
        productService.getAllProducts();
        productService.getAllProducts();

        // Then
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Given a cached product list and by-id entry, "
            + "When createProduct is called, "
            + "Then the list cache is evicted but the by-id entry survives")
    void testCreateProduct_evictsListButKeepsById() {
        // Given
        Product product = new Product();
        product.setId(1L);
        ProductDTO dto = new ProductDTO();
        dto.setId(1L);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productMapper.productToProductDTO(product)).thenReturn(dto);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productMapper.productsToProductDTOs(anyList())).thenReturn(List.of(dto));
        when(productRepository.save(product)).thenReturn(product);

        productService.getProduct(1L);
        productService.getAllProducts();

        // When
        productService.createProduct(product);

        // Then
        productService.getProduct(1L);
        productService.getAllProducts();

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(2)).findAll();
    }
}
