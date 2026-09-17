package com.storename.erp.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

public final class AuthUtils {
    
    private AuthUtils() {}

    public static Long getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null || !(auth.getDetails() instanceof JwtAuthDetails)) {
            throw new AccessDeniedException("Branch information is missing in security context");
        }
        JwtAuthDetails details = (JwtAuthDetails) auth.getDetails();
        if (details.getBranchId() == null || details.getBranchId().isBlank() || "HQ".equalsIgnoreCase(details.getBranchId())) {
            throw new AccessDeniedException("Branch ID is missing or invalid in security token");
        }
        try {
            return Long.parseLong(details.getBranchId());
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Invalid branch ID format");
        }
    }

    public static Long getBranchIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null || !(auth.getDetails() instanceof JwtAuthDetails)) {
            return null;
        }
        JwtAuthDetails details = (JwtAuthDetails) auth.getDetails();
        if (details.getBranchId() == null || details.getBranchId().isBlank() || "HQ".equalsIgnoreCase(details.getBranchId())) {
            return null;
        }
        try {
            return Long.parseLong(details.getBranchId());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Lay UUID cua user hien tai tu JWT subject (user_account.public_id).
     * Nem AccessDeniedException (403) neu khong co hoac subject khong phai UUID
     * (vd. token cu phat hanh truoc khi co public_id) -> nguoi dung can dang nhap lai.
     */
    public static UUID getUserId() {
        UUID userId = getUserIdOrNull();
        if (userId == null) {
            throw new AccessDeniedException("User identity is missing or invalid in security token. Please log in again.");
        }
        return userId;
    }

    /** Nhu {@link #getUserId()} nhung tra ve null thay vi nem loi. */
    public static UUID getUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
