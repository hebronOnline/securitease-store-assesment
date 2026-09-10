package com.example.store.exception;

public class OrderNotFoundException extends NotFoundException {

    public OrderNotFoundException(Long id) {
        super("Order not found for id: " + id);
    }
}
