package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@AutoConfigureMockMvc(addFilters = false)

@WebMvcTest(AuthController.class)
class AuthControllerTest {



    @Autowired private MockMvc mockMvc;

    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserRepository userRepository;
    @MockBean private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void register_shouldReturnToken() throws Exception {
        User user = new User();
        user.setUsername("test");
        user.setPassword("123");

        when(passwordEncoder.encode("123")).thenReturn("encoded123");
        when(jwtUtil.generateToken("test")).thenReturn("token");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("token"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_shouldReturnToken() throws Exception {
        User user = new User();
        user.setUsername("test");
        user.setPassword("123");

        when(jwtUtil.generateToken("test")).thenReturn("token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("token"));

        verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("test", "123"));
    }

    @Test
    @DisplayName("Register with empty username returns 400")
    void register_shouldFail_whenUsernameEmpty() throws Exception {
        User user = new User();
        user.setUsername("");  // пустое имя
        user.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Register with already existing username returns 400")
    void register_shouldFail_whenUsernameExists() throws Exception {
        User user = new User();
        user.setUsername("regular_user");
        user.setPassword("password");

        // Эмулируем, что такой пользователь уже есть
        when(userRepository.existsByUsername("regular_user")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("Login with invalid credentials returns 401")
    void login_shouldFail_whenInvalidCredentials() throws Exception {
        User user = new User();
        user.setUsername("wrongUser");
        user.setPassword("wrongPassword");

        // Эмулируем ошибку аутентификации
        doThrow(new BadCredentialsException("Bad credentials")).when(authenticationManager)
                .authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isUnauthorized());
    }
}
