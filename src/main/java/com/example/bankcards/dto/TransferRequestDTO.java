package com.example.bankcards.dto;
import lombok.Data;


@Data
public class TransferRequestDTO {
    private Long fromCardId;
    private Long toCardId;
    private Double amount;
}
