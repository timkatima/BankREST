package com.example.bankcards.controller;


import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.PasswordUpdateDTO;
import com.example.bankcards.dto.RoleUpdateDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtAuthenticationFilter;
import com.example.bankcards.service.CardService;
import com.example.bankcards.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private CardService cardService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private UserRepository userRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setup() {
        // Устанавливаем mock-аутентификацию с ролью ADMIN
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

    }

    @Test
    @DisplayName("Update user password with invalid userId returns 404")
    void updateUserPassword_shouldReturnNotFound_whenUserNotExists() throws Exception {
        PasswordUpdateDTO dto = new PasswordUpdateDTO();
        dto.setPassword("newPass123");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/admin/users/999/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update user role with invalid role returns 400")
    void updateUserRole_shouldReturnBadRequest_whenRoleInvalid() throws Exception {
        RoleUpdateDTO dto = new RoleUpdateDTO();
        dto.setRole("INVALID_ROLE");

        User user = new User();
        user.setId(1L);
        user.setRole("USER");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCard_shouldReturnCreatedCard() throws Exception {
        CardCreateDTO createDTO = new CardCreateDTO();
        createDTO.setOwnerUsername("admin");
        createDTO.setInitialBalance(1000.0);

        CardDTO cardDTO = new CardDTO();
        cardDTO.setId(1L);

        when(cardService.createCard(any())).thenReturn(cardDTO);

        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void activateCard_shouldReturnOk() throws Exception {
        mockMvc.perform(put("/api/admin/cards/1/activate"))
                .andExpect(status().isOk());

        verify(cardService).activateCard(1L);
    }

    @Test
    void blockCard_shouldReturnOk() throws Exception {
        mockMvc.perform(put("/api/admin/cards/1/block"))
                .andExpect(status().isOk());

        verify(cardService).blockCard(1L);
    }

    @Test
    void deleteCard_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/admin/cards/1"))
                .andExpect(status().isOk());

        verify(cardService).deleteCard(1L);
    }

    @Test
    void getAllCards_shouldReturnList() throws Exception {
        CardDTO cardDTO = new CardDTO();
        cardDTO.setId(1L);

        Page<CardDTO> page = new PageImpl<>(List.of(cardDTO));

        when(cardService.getAllCards(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/cards")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }



    @Test
    void createUser_shouldReturnSuccess() throws Exception {
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("password");
        user.setRole("USER");

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("User created"));
    }

    @Test
    void deleteUser_shouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk());

        verify(userRepository).deleteById(1L);
    }
}

