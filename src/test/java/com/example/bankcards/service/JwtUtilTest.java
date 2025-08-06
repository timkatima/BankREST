package com.example.bankcards.service;

import com.example.bankcards.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.util.Date;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

// JwtUtilTest.java
@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void testGenerateAndValidateToken() throws Exception {
        // Принудительно установить время жизни токена = 10 минут
        Field field = JwtUtil.class.getDeclaredField("JWT_TOKEN_VALIDITY");
        field.setAccessible(true);
        field.set(jwtUtil, 600000L); // 10 минут

        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        assertNotNull(token);
        assertEquals(email, jwtUtil.extractEmail(token));
        assertTrue(jwtUtil.validateToken(token, email));
    }

    @Test
    void testIsTokenExpired_shouldBeFalseForNewToken() {
        try {

            Field field = JwtUtil.class.getDeclaredField("JWT_TOKEN_VALIDITY");
            field.setAccessible(true);
            field.set(jwtUtil, 10_000L);

            String token = jwtUtil.generateToken("test@example.com");
            Date expiration = jwtUtil.extractExpiration(token);

            assertFalse(expiration.before(new Date()), "Token should not be expired immediately after generation");
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    @Test
    void testGenerateToken_shouldReturnNonNullToken() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractEmail_shouldReturnCorrectEmail() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        String extractedEmail = jwtUtil.extractEmail(token);
        assertEquals(email, extractedEmail);
    }

    @Test
    void testExtractExpiration_shouldReturnFutureDate() throws Exception {
        String email = "test@example.com";


        var field = JwtUtil.class.getDeclaredField("JWT_TOKEN_VALIDITY");
        field.setAccessible(true);
        long originalValidity = (long) field.get(jwtUtil);


        field.set(jwtUtil, 5000L);

        try {
            String token = jwtUtil.generateToken(email);
            Date expiration = jwtUtil.extractExpiration(token);

            assertNotNull(expiration, "Expiration date should not be null");
            assertTrue(expiration.after(new Date()), "Expiration should be after current time");
        } finally {

            field.set(jwtUtil, originalValidity);
        }
    }


    @Test
    void testValidateToken_shouldReturnTrueForValidToken() {

        try {
            Field field = JwtUtil.class.getDeclaredField("JWT_TOKEN_VALIDITY");
            field.setAccessible(true);
            field.set(jwtUtil, 10_000L); // 10 секунд

            String email = "test@example.com";
            String token = jwtUtil.generateToken(email);

            assertTrue(jwtUtil.validateToken(token, email));
        } catch (Exception e) {
            fail("Ошибка при настройке теста: " + e.getMessage());
        }
    }


    @Test
    void testValidateToken_shouldReturnFalseForInvalidEmail() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        assertFalse(jwtUtil.validateToken(token, "wrong@example.com"));
    }

    @Test
    void testValidateToken_shouldReturnFalseIfTokenIsExpired() throws NoSuchFieldException, IllegalAccessException {

        var field = JwtUtil.class.getDeclaredField("JWT_TOKEN_VALIDITY");
        field.setAccessible(true);
        field.set(jwtUtil, 1L);

        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);


        assertFalse(jwtUtil.validateToken(token, email));
    }


    @Test
    void testExtractClaim_shouldReturnExpectedClaim() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        String subject = jwtUtil.extractClaim(token, claims -> claims.getSubject());
        assertEquals(email, subject);
    }

    @Test
    void testExtractAllClaims_shouldContainSubjectAndExpiration() {
        String email = "test@example.com";
        String token = jwtUtil.generateToken(email);

        var claims = jwtUtil.extractClaim(token, c -> c);
        assertEquals(email, claims.getSubject());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void testMalformedToken_shouldReturnFalse() {
        String malformedToken = "not_a_valid_token";
        assertThrows(RuntimeException.class, () -> jwtUtil.extractEmail(malformedToken));
        assertThrows(RuntimeException.class, () -> jwtUtil.extractExpiration(malformedToken));
        assertFalse(jwtUtil.validateToken(malformedToken, "any@example.com"));
    }

}
