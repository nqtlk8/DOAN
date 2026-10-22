package com.storename.erp.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Ky va xac minh JWT (RS256).
 * <ul>
 *   <li>HQ (va ALL khi chay dev) giu private key de cap token.</li>
 *   <li>Branch chi giu public key de xac minh, khong the tu cap token.</li>
 * </ul>
 * Claim: {@code sub} = user_account.public_id, {@code role}, {@code branchId},
 * {@code tokenId}, {@code type} (access | refresh), {@code tv} = user_account.token_version.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TOKEN_VERSION = "tv";

    private PrivateKey privateKey;
    private PublicKey publicKey;

    @Value("${jwt.expiration-ms:1800000}")
    private long expirationMs;

    @Value("${jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Value("${instance.role:ALL}")
    private String instanceRole = "ALL";

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

    /**
     * Kiem tra khoa ngay khi khoi dong thay vi de loi xuat hien o lan dang nhap dau tien.
     * <ul>
     *   <li>Instance HQ va BRANCH phai co public key (de xac minh token).</li>
     *   <li>Instance HQ phai co private key (de cap token).</li>
     *   <li>Instance ALL (chi dung khi dev/test) chi ghi log ERROR de khong chan moi truong thu nghiem.</li>
     * </ul>
     */
    @PostConstruct
    void validateKeys() {
        boolean strict = "HQ".equalsIgnoreCase(instanceRole) || "BRANCH".equalsIgnoreCase(instanceRole);
        if (publicKey == null) {
            if (strict) {
                throw new IllegalStateException("JWT public key is not configured (jwt.public-key / JWT_PUBLIC_KEY_PATH). Cannot verify tokens.");
            }
            log.error("JWT public key is not configured: every authenticated request will be rejected.");
        }
        if (privateKey == null) {
            if ("HQ".equalsIgnoreCase(instanceRole)) {
                throw new IllegalStateException("JWT private key is not configured (jwt.private-key / JWT_PRIVATE_KEY_PATH). HQ cannot issue tokens.");
            }
            if ("ALL".equalsIgnoreCase(instanceRole)) {
                log.error("JWT private key is not configured: login and refresh will return 503 until jwt.private-key is set.");
            }
        }
    }

    public String generateToken(String subject, String role, String branchId, String tokenId) {
        return generateToken(subject, role, branchId, tokenId, 0);
    }

    public String generateToken(String subject, String role, String branchId, String tokenId, int tokenVersion) {
        return buildToken(subject, role, branchId, tokenId, "access", expirationMs, tokenVersion);
    }

    public String generateRefreshToken(String subject, String role, String branchId, String tokenId) {
        return generateRefreshToken(subject, role, branchId, tokenId, 0);
    }

    public String generateRefreshToken(String subject, String role, String branchId, String tokenId, int tokenVersion) {
        return buildToken(subject, role, branchId, tokenId, "refresh", refreshExpirationMs, tokenVersion);
    }

    private String buildToken(String subject, String role, String branchId, String tokenId, String type,
                              long expirationTime, int tokenVersion) {
        if (privateKey == null) {
            throw new com.storename.erp.identity.domain.exception.SigningKeyNotConfiguredException("Private key not configured. Cannot generate token.");
        }
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("branchId", branchId)
                .claim("tokenId", tokenId)
                .claim("type", type)
                .claim(CLAIM_TOKEN_VERSION, tokenVersion)
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

    /** Doc claim {@code tv}; token phat hanh truoc khi co claim nay duoc coi la version 0. */
    public static int getTokenVersion(Claims claims) {
        Object tv = claims.get(CLAIM_TOKEN_VERSION);
        if (tv instanceof Number n) {
            return n.intValue();
        }
        return 0;
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
