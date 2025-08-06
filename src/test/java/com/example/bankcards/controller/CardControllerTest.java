package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferRequestDTO;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.ArgumentMatchers.any;

// CardControllerTest.java
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CardController.class)
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private CardService cardService;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;



    @Test
    @DisplayName("getUserCards returns filtered cards when search param is present")
    void getUserCards_withSearchParam_returnsFilteredCards() throws Exception {
        CardDTO dto = new CardDTO();
        dto.setId(1L);
        dto.setMaskedCardNumber("**** **** **** 5678");

        Page<CardDTO> page = new PageImpl<>(List.of(dto));

        when(cardService.getUserCardsBySearch(eq("5678"), any())).thenReturn(page);

        mockMvc.perform(get("/api/user/cards")
                        .param("search", "5678")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 5678"));
    }

    @Test
    @DisplayName("requestBlockCard blocks user-owned card")
    void requestBlockCard_blocksUserCard() throws Exception {
        CardDTO card = new CardDTO();
        card.setId(1L);

        Page<CardDTO> page = new PageImpl<>(List.of(card));

        when(cardService.getUserCards(any())).thenReturn(page);

        mockMvc.perform(post("/api/cards/1/block"))
                .andExpect(status().isOk());

        verify(cardService).blockCard(1L);
    }

    @Test
    @DisplayName("requestBlockCard throws error if card not found or not owned")
    void requestBlockCard_throwsIfCardNotFoundOrNotOwned() throws Exception {
        doThrow(new ResourceNotFoundException("Card not found or not owned"))
                .when(cardService).blockCard(99L);
        when(cardService.getUserCards(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(post("/api/cards/99/block"))
                .andExpect(status().isNotFound());
    }



    @Test
    @DisplayName("searchUserCards returns page of cards")
    void searchUserCards_returnsPage() throws Exception {
        CardDTO dto = new CardDTO();
        dto.setId(1L);
        dto.setMaskedCardNumber("**** **** **** 1111");
        Page<CardDTO> page = new PageImpl<>(List.of(dto));

        when(cardService.getUserCardsBySearch(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/cards/search")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].maskedCardNumber").value("**** **** **** 1111"));
    }



    @Test
    void transfer_shouldReturnOk() throws Exception {
        TransferRequestDTO dto = new TransferRequestDTO();
        dto.setFromCardId(1L);
        dto.setToCardId(2L);
        dto.setAmount(50.0);

        mockMvc.perform(post("/api/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isOk());

        verify(cardService).transfer(1L, 2L, 50.0);
    }



    @Test
    @DisplayName("Request block card with non-existent card returns 404")
    void requestBlockCard_shouldReturnNotFound_whenCardNotOwned() throws Exception {
        Page<CardDTO> cardsPage = new PageImpl<>(List.of()); // Пустой список

        when(cardService.getUserCards(any())).thenReturn(cardsPage);

        mockMvc.perform(post("/api/cards/99/block"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Get card balance returns correct balance")
    void getCardBalance_shouldReturnBalance() throws Exception {
        when(cardService.getCardBalance(1L)).thenReturn(123.45);

        mockMvc.perform(get("/api/cards/1/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string("123.45"));
    }

    @Test
    @DisplayName("Get card balance when card not found returns 404")
    void getCardBalance_shouldReturnNotFound_whenCardMissing() throws Exception {
        when(cardService.getCardBalance(999L)).thenThrow(new ResourceNotFoundException("Card not found"));

        mockMvc.perform(get("/api/cards/999/balance"))
                .andExpect(status().isNotFound());
    }





}

