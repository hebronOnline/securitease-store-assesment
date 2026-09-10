package com.example.store.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CreateProductRequest {
    @NotBlank(message = "description should be populated")
    private String description;
}
