package com.storename.erp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

public class JwtTokenProviderTest {

    private static String validPrivateKeyPem;
    private static String validPublicKeyPem;
    private static String fakePrivateKeyPem;

    @BeforeAll
    static void setupKeys() throws Exception {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        
        KeyPair validPair = keyPairGen.generateKeyPair();
        validPrivateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(validPair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----";
        validPublicKeyPem = "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(validPair.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----";

        KeyPair fakePair = keyPairGen.generateKeyPair();
        fakePrivateKeyPem = "-----BEGIN PRIVATE KEY-----\n" + Base64.getEncoder().encodeToString(fakePair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----";
    }

    @Test
    void shouldVerifyValidTokenLocally() throws Exception {
        // Arrange
        JwtTokenProvider hqProvider = new JwtTokenProvider(
                new ByteArrayResource(validPrivateKeyPem.getBytes()), 
                new ByteArrayResource(validPublicKeyPem.getBytes())
        );
        ReflectionTestUtils.setField(hqProvider, "expirationMs", 3600000L); // 1 hour

        JwtTokenProvider branchProvider = new JwtTokenProvider(
                null, // Branch doesn't have private key
                new ByteArrayResource(validPublicKeyPem.getBytes())
        );

        // Act
        String token = hqProvider.generateToken("admin", "ADMIN", "HQ", "token-1");

        // Assert
        Claims claims = branchProvider.getClaimsFromToken(token);
        assertEquals("admin", claims.getSubject());
        assertEquals("ADMIN", claims.get("role"));
        assertEquals("HQ", claims.get("branchId"));
    }

    @Test
    void shouldThrowExceptionForExpiredToken() throws Exception {
        // Arrange
        JwtTokenProvider hqProvider = new JwtTokenProvider(
                new ByteArrayResource(validPrivateKeyPem.getBytes()),
                new ByteArrayResource(validPublicKeyPem.getBytes())
        );
        ReflectionTestUtils.setField(hqProvider, "expirationMs", 1L); // 1 ms expiration

        JwtTokenProvider branchProvider = new JwtTokenProvider(
                null,
                new ByteArrayResource(validPublicKeyPem.getBytes())
        );

        // Act
        String token = hqProvider.generateToken("admin", "ADMIN", "HQ", "token-1");
        
        // Wait for token to expire
        Thread.sleep(10);

        // Assert
        assertThrows(ExpiredJwtException.class, () -> branchProvider.getClaimsFromToken(token));
    }

    @Test
    void shouldThrowExceptionForFakeSignature() throws Exception {
        // Arrange
        JwtTokenProvider fakeHqProvider = new JwtTokenProvider(
                new ByteArrayResource(fakePrivateKeyPem.getBytes()), 
                null
        );
        ReflectionTestUtils.setField(fakeHqProvider, "expirationMs", 3600000L);

        JwtTokenProvider branchProvider = new JwtTokenProvider(
                null, 
                new ByteArrayResource(validPublicKeyPem.getBytes())
        );

        // Act
        String forgedToken = fakeHqProvider.generateToken("admin", "ADMIN", "HQ", "token-1");

        // Assert
        assertThrows(SignatureException.class, () -> branchProvider.getClaimsFromToken(forgedToken));
    }

    @Test
    void shouldThrowExceptionForMalformedToken() throws Exception {
        // Arrange
        JwtTokenProvider branchProvider = new JwtTokenProvider(
                null,
                new ByteArrayResource(validPublicKeyPem.getBytes())
        );

        String malformedToken = "invalid_token_without_dots";

        // Assert
        assertThrows(MalformedJwtException.class, () -> branchProvider.getClaimsFromToken(malformedToken));
    }
}
