package com.storename.erp.common.aop;

import com.storename.erp.common.security.JwtAuthDetails;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class BranchScopedAspect {

    private static final Logger log = LoggerFactory.getLogger(BranchScopedAspect.class);

    @org.springframework.beans.factory.annotation.Value("${instance.role:HQ}")
    private String instanceRole;

    @org.springframework.beans.factory.annotation.Value("${branch-id:}")
    private String configuredBranchId;

    @Before("@annotation(BranchScoped) || @within(BranchScoped)")
    public void validateBranchScope(JoinPoint joinPoint) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String branchId = null;
        
        if (auth != null && auth.getDetails() instanceof JwtAuthDetails) {
            JwtAuthDetails details = (JwtAuthDetails) auth.getDetails();
            branchId = details.getBranchId();
        }
        
        log.info("BranchScoped validation for branchId: {}", branchId);
        
        if (branchId == null || branchId.trim().isEmpty()) {
            throw new SecurityException("Branch scope validation failed: branchId is missing in token");
        }
        
        if ("BRANCH".equalsIgnoreCase(instanceRole) && !branchId.equals(configuredBranchId)) {
            log.error("Branch isolation violation: token branchId {} does not match configured branchId {}", branchId, configuredBranchId);
            throw new SecurityException("Branch scope validation failed: branchId mismatch");
        }
    }
}
