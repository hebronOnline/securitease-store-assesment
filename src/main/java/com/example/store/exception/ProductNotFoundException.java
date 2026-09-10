package com.example.store.exception;

public class ProductNotFoundException extends NotFoundException {

    public ProductNotFoundException(Long id) {
        super("Product not found for id: " + id);
    }
}
