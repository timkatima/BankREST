
package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateDTO;
import com.example.bankcards.dto.CardDTO;
import com.example.bankcards.dto.TransferRequestDTO;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.service.CardService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CardController {



    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping("/user/cards")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardDTO>> getUserCards(@RequestParam(required = false) String search, Pageable pageable) {
        if (search != null && !search.isEmpty()) {
            return ResponseEntity.ok(cardService.getUserCardsBySearch(search, pageable));
        }
        return ResponseEntity.ok(cardService.getUserCards(pageable));
    }

    @PostMapping("/cards/{cardId}/block")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> requestBlockCard(@PathVariable Long cardId) {
        CardDTO card = cardService.getUserCards(Pageable.unpaged())
                .getContent().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Card not found or not owned"));
        cardService.blockCard(cardId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/transfer")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> transfer(@RequestBody TransferRequestDTO request) {
        cardService.transfer(request.getFromCardId(), request.getToCardId(), request.getAmount());
        return ResponseEntity.ok().build();
    }


    @GetMapping("/cards/{cardId}/balance")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Double> getCardBalance(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardService.getCardBalance(cardId));
    }

    @GetMapping("/cards/search")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<CardDTO>> searchUserCards(Pageable pageable) {
        try {
            Page<CardDTO> cards = cardService.getUserCardsBySearch(pageable);
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

}