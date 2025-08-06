package com.example.bankcards.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PasswordUpdateDTO {
    @NotNull
    @Size(min = 4, max = 100, message = "Password must be between 4 and 100 characters")
    private String password;


    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
