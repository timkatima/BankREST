
package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
    boolean existsByCardNumber(String cardNumber);
    Page<Card> findByOwner(User owner, Pageable pageable);
    Page<Card> findByOwnerAndCardNumberContaining(User owner, String query, Pageable pageable);
    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);

}