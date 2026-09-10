package com.example.store.exception;

public class CustomerNotFoundException extends NotFoundException {

    public CustomerNotFoundException(Long id) {
        super("Customer not found for id: " + id);
    }
}
