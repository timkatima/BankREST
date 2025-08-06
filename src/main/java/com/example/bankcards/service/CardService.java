
package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.entity.Card;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    @Value("${encryption.key}")
    private String encryptionKey;

    public CardService(CardRepository cardRepository, UserRepository userRepository) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
    }

    public CardDTO createCard(CardCreateDTO createDTO) {
        User owner = userRepository.findByUsername(createDTO.getOwnerUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Card card = new Card();
        card.setCardNumber(encryptCardNumber(generateCardNumber()));
        card.setOwner(owner);
        card.setExpiryDate(createDTO.getExpiryDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(createDTO.getInitialBalance());
        card = cardRepository.save(card);
        return mapToDTO(card);
    }

    public Page<CardDTO> getUserCards(Pageable pageable) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return cardRepository.findByOwner(user, pageable).map(this::mapToDTO);
    }

    public Page<CardDTO> getUserCardsBySearch(String query, Pageable pageable) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Card> cardsPage = cardRepository.findByOwnerAndCardNumberContaining(user, query, pageable);
        return cardsPage.map(card -> {
            CardDTO dto = new CardDTO();
            dto.setId(card.getId());
            String decryptedNumber = decryptCardNumber(card.getCardNumber());
            dto.setMaskedCardNumber(decryptedNumber); // Или маскированный номер
            dto.setOwnerUsername(card.getOwner().getUsername());
            dto.setExpiryDate(card.getExpiryDate());
            dto.setStatus(card.getStatus().name());
            dto.setBalance(card.getBalance());
            return dto;
        });
    }

    public Page<CardDTO> getUserCardsBySearch(Pageable pageable) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<Card> cardsPage = cardRepository.findByOwnerAndStatus(user, CardStatus.ACTIVE, pageable);
        return cardsPage.map(card -> {
            CardDTO dto = new CardDTO();
            dto.setId(card.getId());
            String decryptedNumber = decryptCardNumber(card.getCardNumber());
            dto.setMaskedCardNumber(decryptedNumber);
            dto.setOwnerUsername(card.getOwner().getUsername());
            dto.setExpiryDate(card.getExpiryDate());
            dto.setStatus(card.getStatus().name());
            dto.setBalance(card.getBalance());
            return dto;
        });
    }



    public void blockCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!card.getOwner().getUsername().equals(username) && !isAdmin()) {
            throw new RuntimeException("Access denied");
        }
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }

    public void activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        if (!isAdmin()) {
            throw new RuntimeException("Access denied");
        }
        card.setStatus(CardStatus.ACTIVE);
        cardRepository.save(card);
    }

    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        if (!isAdmin()) {
            throw new RuntimeException("Access denied");
        }
        cardRepository.delete(card);
    }

    public void transfer(Long fromCardId, Long toCardId, Double amount) {
        Card fromCard = cardRepository.findById(fromCardId)
                .orElseThrow(() -> new RuntimeException("Source card not found"));
        Card toCard = cardRepository.findById(toCardId)
                .orElseThrow(() -> new RuntimeException("Destination card not found"));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!fromCard.getOwner().getUsername().equals(username) || !toCard.getOwner().getUsername().equals(username)) {
            throw new RuntimeException("Access denied");
        }
        if (fromCard.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }
        fromCard.setBalance(fromCard.getBalance() - amount);
        toCard.setBalance(toCard.getBalance() + amount);
        cardRepository.save(fromCard);
        cardRepository.save(toCard);
    }

    public Page<CardDTO> getAllCards(Pageable pageable) {
        if (!isAdmin()) {
            throw new RuntimeException("Access denied");
        }
        return cardRepository.findAll(pageable).map(this::mapToDTO);
    }

    public Double getCardBalance(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!card.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("Access denied");
        }
        return card.getBalance();
    }

    private String generateCardNumber() {
        Random random = new Random();
        String cardNumber;
        boolean isUnique;

        do {
            StringBuilder tempNumber = new StringBuilder("4");
            for (int i = 0; i < 14; i++) {
                tempNumber.append(random.nextInt(10));
            }
            int checkDigit = calculateLuhnCheckDigit(tempNumber.toString());
            cardNumber = tempNumber.append(checkDigit).toString();
            isUnique = !cardRepository.existsByCardNumber(cardNumber);
        } while (!isUnique);

        return cardNumber;
    }

    private int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean isEven = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (isEven) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            isEven = !isEven;
        }
        return (10 - (sum % 10)) % 10;
    }

    String encryptCardNumber(String cardNumber) {
        try {
            if (cardNumber.length() != 16) {
                throw new IllegalArgumentException("Card number must be 16 digits");
            }
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            byte[] encrypted = cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            System.err.println("Encryption error: " + e.getMessage());
            throw new RuntimeException("Encryption error", e);
        }
    }

    public String decryptCardNumber(String encryptedCardNumber) {
        try {

            byte[] combined = Base64.getDecoder().decode(encryptedCardNumber);

            byte[] iv = Arrays.copyOfRange(combined, 0, 16);
            byte[] encrypted = Arrays.copyOfRange(combined, 16, combined.length);
            SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            System.err.println("Base64 decode error: " + e.getMessage());
            throw new RuntimeException("Decryption error: Invalid Base64", e);
        } catch (BadPaddingException e) {
            System.err.println("Decryption error: Bad padding - " + e.getMessage());
            throw new RuntimeException("Decryption error: Bad padding", e);
        } catch (IllegalBlockSizeException e) {
            System.err.println("Decryption error: Invalid block size - " + e.getMessage());
            throw new RuntimeException("Decryption error: Invalid block size", e);
        } catch (InvalidKeyException e) {
            System.err.println("Decryption error: Invalid key - " + e.getMessage());
            throw new RuntimeException("Decryption error: Invalid key", e);
        } catch (Exception e) {
            System.err.println("General decryption error: " + e.getMessage());
            throw new RuntimeException("Decryption error", e);
        }
    }

    private CardDTO mapToDTO(Card card) {
        CardDTO dto = new CardDTO();
        dto.setId(card.getId());
        String decrypted = decryptCardNumber(card.getCardNumber());
        dto.setMaskedCardNumber(String.format("**** **** **** %s", decrypted.substring(12))); // Последние 4 цифры
        dto.setOwnerUsername(card.getOwner().getUsername());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setStatus(card.getStatus().name());
        dto.setBalance(card.getBalance());
        return dto;
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}