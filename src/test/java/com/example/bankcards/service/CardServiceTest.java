package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// CardServiceTest.java
@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CardService cardService;

    @Value("${encryption.key}")
    private String encryptionKey;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(cardService, "encryptionKey", "1234567890123456");
    }


    @Test
    void createCard_shouldReturnCardDTO() {
        CardCreateDTO dto = new CardCreateDTO();
        dto.setOwnerUsername("user");
        dto.setExpiryDate(LocalDate.now().plusYears(3));
        dto.setInitialBalance(100.0);

        User user = new User();
        user.setUsername("user");

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.existsByCardNumber(anyString())).thenReturn(false);
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CardDTO result = cardService.createCard(dto);

        assertNotNull(result);
        assertEquals("user", result.getOwnerUsername());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void getUserCards_shouldReturnUserCards() {
        User user = new User();
        user.setUsername("user");

        Card card = new Card();
        card.setId(1L);
        card.setCardNumber(cardService.encryptCardNumber("4111111111111111"));
        card.setOwner(user);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(100.0);

        mockAuthentication("user", "USER");

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findByOwner(eq(user), any())).thenReturn(new PageImpl<>(List.of(card)));

        Page<CardDTO> result = cardService.getUserCards(Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals("user", result.getContent().get(0).getOwnerUsername());
    }

    @Test
    void createCard_shouldThrowIfUserNotFound() {
        CardCreateDTO dto = new CardCreateDTO();
        dto.setOwnerUsername("missing");

        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cardService.createCard(dto));
    }

    @Test
    void blockCard_shouldThrowIfNotOwnerOrAdmin() {
        User owner = new User();
        owner.setUsername("someone");

        Card card = new Card();
        card.setId(1L);
        card.setOwner(owner);
        card.setStatus(CardStatus.ACTIVE);

        mockAuthentication("user", "USER");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(RuntimeException.class, () -> cardService.blockCard(1L));
    }

    @Test
    void activateCard_shouldThrowIfNotAdmin() {
        Card card = new Card();
        card.setStatus(CardStatus.BLOCKED);

        mockAuthentication("user", "USER");
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(RuntimeException.class, () -> cardService.activateCard(1L));
    }

    @Test
    void deleteCard_shouldThrowIfNotAdmin() {
        Card card = new Card();
        card.setId(1L);

        mockAuthentication("user", "USER");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(RuntimeException.class, () -> cardService.deleteCard(1L));
    }

    @Test
    void transfer_shouldThrowIfCardNotFound() {
        mockAuthentication("user", "USER");

        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cardService.transfer(1L, 2L, 10.0));
    }

    @Test
    void transfer_shouldThrowIfNotOwner() {
        User another = new User();
        another.setUsername("another");

        Card from = new Card();
        from.setId(1L);
        from.setOwner(another);

        Card to = new Card();
        to.setId(2L);
        to.setOwner(another);

        mockAuthentication("user", "USER");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        assertThrows(RuntimeException.class, () -> cardService.transfer(1L, 2L, 10.0));
    }

    @Test
    void transfer_shouldThrowIfInsufficientBalance() {
        User user = new User();
        user.setUsername("user");

        Card from = new Card();
        from.setId(1L);
        from.setBalance(5.0);
        from.setOwner(user);

        Card to = new Card();
        to.setId(2L);
        to.setOwner(user);

        mockAuthentication("user", "USER");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        assertThrows(RuntimeException.class, () -> cardService.transfer(1L, 2L, 10.0));
    }

    @Test
    void getAllCards_shouldThrowIfNotAdmin() {
        mockAuthentication("user", "USER");

        assertThrows(RuntimeException.class, () -> cardService.getAllCards(Pageable.unpaged()));
    }

    @Test
    void getUserCards_shouldThrowIfUserNotFound() {
        mockAuthentication("user", "USER");

        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cardService.getUserCards(Pageable.unpaged()));
    }

    @Test
    void getUserCardsBySearch_shouldThrowIfUserNotFound() {
        mockAuthentication("user", "USER");

        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> cardService.getUserCardsBySearch("1234", Pageable.unpaged()));
    }

    @Test
    void decryptCardNumber_shouldThrowIfBase64Invalid() {
        ReflectionTestUtils.setField(cardService, "encryptionKey", "1234567890123456");

        String invalid = "%%%notbase64%%%";

        assertThrows(RuntimeException.class, () -> cardService.decryptCardNumber(invalid));
    }


    @Test
    void getUserCardsBySearch_shouldReturnFilteredCards() {
        User user = new User();
        user.setUsername("user");

        Card card = new Card();
        card.setId(1L);
        card.setCardNumber(cardService.encryptCardNumber("4111111111115678"));
        card.setOwner(user);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(50.0);

        mockAuthentication("user", "USER");

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(cardRepository.findByOwnerAndCardNumberContaining(eq(user), eq("5678"), any()))
                .thenReturn(new PageImpl<>(List.of(card)));

        Page<CardDTO> result = cardService.getUserCardsBySearch("5678", Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("user", result.getContent().get(0).getOwnerUsername());
    }

    @Test
    void deleteCard_shouldDeleteIfAdmin() {
        Card card = new Card();
        card.setId(1L);

        mockAuthentication("admin", "ADMIN");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.deleteCard(1L);

        verify(cardRepository).delete(card);
    }

    @Test
    void getAllCards_shouldReturnAllCardsIfAdmin() {
        User admin = new User();
        admin.setUsername("admin");

        Card card = new Card();
        card.setId(1L);
        card.setCardNumber(cardService.encryptCardNumber("4111111111111111"));
        card.setOwner(admin);
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(99.0);

        mockAuthentication("admin", "ADMIN");

        when(cardRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(card)));

        Page<CardDTO> result = cardService.getAllCards(Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals("admin", result.getContent().get(0).getOwnerUsername());
    }

    @Test
    void getCardBalance_shouldReturnBalanceIfOwner() {
        User user = new User();
        user.setUsername("user");

        Card card = new Card();
        card.setId(1L);
        card.setOwner(user);
        card.setBalance(123.0);

        mockAuthentication("user", "ROLE_USER");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        Double balance = cardService.getCardBalance(1L);

        assertEquals(123.0, balance);
    }

    @Test
    void getCardBalance_shouldThrowIfNotOwner() {
        User owner = new User();
        owner.setUsername("someone");

        Card card = new Card();
        card.setOwner(owner);

        mockAuthentication("user", "USER");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        assertThrows(RuntimeException.class, () -> cardService.getCardBalance(1L));
    }

    @Test
    void encryptAndDecryptCardNumber_shouldReturnOriginal() {
        String cardNumber = "4111111111111111";
        ReflectionTestUtils.setField(cardService, "encryptionKey", "1234567890123456");

        String encrypted = cardService.encryptCardNumber(cardNumber);
        String decrypted = cardService.decryptCardNumber(encrypted);

        assertEquals(cardNumber, decrypted);
    }


    @Test
    void transfer_shouldTransferBalance() {
        User user = new User();
        user.setUsername("user");

        Card from = new Card();
        from.setId(1L);
        from.setBalance(100.0);
        from.setOwner(user);

        Card to = new Card();
        to.setId(2L);
        to.setBalance(50.0);
        to.setOwner(user);

        mockAuthentication("user", "ROLE_USER");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(from));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(to));

        cardService.transfer(1L, 2L, 30.0);

        assertEquals(70.0, from.getBalance());
        assertEquals(80.0, to.getBalance());
        verify(cardRepository, times(2)).save(any());
    }

    @Test
    void blockCard_shouldBlockIfOwnerOrAdmin() {
        User user = new User();
        user.setUsername("user");

        Card card = new Card();
        card.setId(1L);
        card.setOwner(user);
        card.setStatus(CardStatus.ACTIVE);

        mockAuthentication("user", "ROLE_USER");

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.blockCard(1L);

        assertEquals(CardStatus.BLOCKED, card.getStatus());
    }

    @Test
    void activateCard_shouldActivateIfAdmin() {
        Card card = new Card();
        card.setStatus(CardStatus.BLOCKED);

        mockAuthentication("admin", "ADMIN");
        when(cardRepository.findById(any())).thenReturn(Optional.of(card));

        cardService.activateCard(1L);

        assertEquals(CardStatus.ACTIVE, card.getStatus());
    }

    private void mockAuthentication(String username, String... roles) {
        var authorities = Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();

        Authentication auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
