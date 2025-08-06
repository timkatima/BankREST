package com.example.bankcards.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CardCreateDTO {
    private String ownerUsername;
    private LocalDate expiryDate;
    private Double initialBalance;
}
