package com.example.bankcards.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CardDTO {
    private Long id;
    private String maskedCardNumber; // **** **** **** 1234
    private String ownerUsername;
    private LocalDate expiryDate;
    private String status;
    private Double balance;
}