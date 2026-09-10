package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CreateCustomerRequest {
    @NotBlank(message = "name should be populated")
    private String name;
}
