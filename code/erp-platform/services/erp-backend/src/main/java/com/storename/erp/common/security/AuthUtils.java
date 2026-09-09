package com.storename.erp.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

public final class AuthUtils {
    
    private AuthUtils() {}

    public static Long getBranchId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null || !(auth.getDetails() instanceof JwtAuthDetails)) {
            throw new AccessDeniedException("Branch information is missing in security context");
        }
        JwtAuthDetails details = (JwtAuthDetails) auth.getDetails();
        if (details.getBranchId() == null || details.getBranchId().isBlank()) {
            throw new AccessDeniedException("Branch ID is missing in security token");
        }
        return Long.parseLong(details.getBranchId());
    }

    public static Long getBranchIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null || !(auth.getDetails() instanceof JwtAuthDetails)) {
            return null;
        }
        JwtAuthDetails details = (JwtAuthDetails) auth.getDetails();
        return details.getBranchId() != null && !details.getBranchId().isBlank() ? Long.parseLong(details.getBranchId()) : null;
    }
}
