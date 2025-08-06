package com.example.bankcards.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String role;
    private List<CardDTO> cards;
}
