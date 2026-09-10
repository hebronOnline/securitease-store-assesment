package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "description should be populated")
    private String description;

    @NotNull(message = "customerId should be populated") private Long customerId;

    @NotEmpty(message = "an order must contain at least one product")
    private List<Long> productIds;
}
