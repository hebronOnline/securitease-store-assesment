package com.example.store.service;

import com.example.store.config.CacheConfig;
import com.example.store.dto.ProductDTO;
import com.example.store.entity.Product;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.mapper.ProductMapper;
import com.example.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<ProductDTO> getAllProducts() {
        return productMapper.productsToProductDTOs(productRepository.findAll());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "#id")
    public ProductDTO getProduct(Long id) {
        return productRepository
                .findById(id)
                .map(productMapper::productToProductDTO)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(value = CacheConfig.PRODUCTS_CACHE, key = "'all'"),
                @CacheEvict(value = CacheConfig.ORDERS_CACHE, allEntries = true)
            })
    public ProductDTO createProduct(Product product) {
        return productMapper.productToProductDTO(productRepository.save(product));
    }
}
