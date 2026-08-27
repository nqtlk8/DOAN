package com.storename.erp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @Value("${jwt.expiration-ms:1800000}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.private-key:#{null}}") Resource privateKeyResource,
            @Value("${jwt.public-key:#{null}}") Resource publicKeyResource) throws Exception {
        
        if (privateKeyResource != null && privateKeyResource.exists()) {
            this.privateKey = loadPrivateKey(privateKeyResource);
        }
        if (publicKeyResource != null && publicKeyResource.exists()) {
            this.publicKey = loadPublicKey(publicKeyResource);
        }
    }

    public String generateToken(String username, String role, String branchId, String tokenId) {
        return buildToken(username, role, branchId, tokenId, "access", expirationMs);
    }

    public String generateRefreshToken(String username, String role, String branchId, String tokenId) {
        return buildToken(username, role, branchId, tokenId, "refresh", refreshExpirationMs);
    }

    private String buildToken(String username, String role, String branchId, String tokenId, String type, long expirationTime) {
        if (privateKey == null) {
            throw new IllegalStateException("Private key not configured. Cannot generate token.");
        }
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("branchId", branchId)
                .claim("tokenId", tokenId)
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    

    private PrivateKey loadPrivateKey(Resource resource) throws Exception {
        byte[] keyBytesArr = FileCopyUtils.copyToByteArray(resource.getInputStream());
        String key = new String(keyBytesArr, StandardCharsets.UTF_8)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private PublicKey loadPublicKey(Resource resource) throws Exception {
        byte[] keyBytesArr = FileCopyUtils.copyToByteArray(resource.getInputStream());
        String key = new String(keyBytesArr, StandardCharsets.UTF_8)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
