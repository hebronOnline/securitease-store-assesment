package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "description should be populated")
    private String description;

    private Long customerId;
}
