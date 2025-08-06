
package com.example.bankcards.controller;

import com.example.bankcards.dto.*;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/admin")
public class    AdminController {

    @Autowired
    private UserRepository userRepository;
    private final CardService cardService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(CardService cardService, PasswordEncoder passwordEncoder) {
        this.cardService = cardService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDTO> createCard(@RequestBody CardCreateDTO createDTO) {
        return ResponseEntity.ok(cardService.createCard(createDTO));
    }

    @PutMapping("/cards/{cardId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateCard(@PathVariable Long cardId) {
        cardService.activateCard(cardId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/cards/{cardId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> blockCard(@PathVariable Long cardId) {
        cardService.blockCard(cardId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cards/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardDTO>> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getAllCards(pageable));
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createUser(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        // Проверка, может ли текущий пользователь назначить роль ADMIN
        String currentRole = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().substring(5)) // Удаляем "ROLE_"
                .orElse("USER");
        if ("ADMIN".equals(user.getRole()) && !"ADMIN".equals(currentRole)) {
            return ResponseEntity.status(403).body("Only an admin can assign the ADMIN role");
        }
        if (!"USER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.badRequest().body("Invalid role. Use 'USER' or 'ADMIN'");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("User created");
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userRepository.deleteById(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserPassword(@PathVariable Long userId, @RequestBody @Valid PasswordUpdateDTO passwordDTO, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body("Invalid password data");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(passwordDTO.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("Password updated");
    }

    @PutMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUserRole(@PathVariable Long userId, @RequestBody @Valid RoleUpdateDTO roleDTO) {

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String currentRole = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .findFirst().map(auth -> auth.getAuthority().substring(5)).orElse("USER");
        if ("ADMIN".equals(roleDTO.getRole()) && !"ADMIN".equals(currentRole)) {
            return ResponseEntity.status(403).body("Only an admin can assign the ADMIN role");
        }
        if (!"USER".equals(roleDTO.getRole()) && !"ADMIN".equals(roleDTO.getRole())) {
            return ResponseEntity.badRequest().body("Invalid role. Use 'USER' or 'ADMIN'");
        }
        user.setRole(roleDTO.getRole());
        userRepository.save(user);
        return ResponseEntity.ok("Role updated");
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getUsers(@RequestParam(required = false) String username, Pageable pageable) {
        Page<User> usersPage;
        if (username != null && !username.isEmpty()) {
            usersPage = userRepository.findByUsernameContaining(username, pageable);
        } else {
            usersPage = userRepository.findAll(pageable);
        }
        Page<UserDTO> userDTOPage = usersPage.map(user -> {
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setRole(user.getRole());
            dto.setCards(user.getCards().stream().map(card -> {
                CardDTO cardDTO = new CardDTO();
                cardDTO.setId(card.getId());
                // Маскирование номера карты (последние 4 цифры видны)
                String decryptedNumber = cardService.decryptCardNumber(card.getCardNumber()); // Предполагается, что метод есть
                cardDTO.setMaskedCardNumber(String.format("**** **** **** %s", decryptedNumber.substring(12)));
                cardDTO.setOwnerUsername(card.getOwner().getUsername()); // Имя владельца
                cardDTO.setExpiryDate(card.getExpiryDate()); // Срок действия
                cardDTO.setStatus(card.getStatus().name());
                cardDTO.setBalance(card.getBalance());
                return cardDTO;
            }).collect(Collectors.toList()));
            return dto;
        });
        return ResponseEntity.ok(userDTOPage);
    }
}