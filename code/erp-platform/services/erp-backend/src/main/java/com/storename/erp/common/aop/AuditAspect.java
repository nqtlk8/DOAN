package com.storename.erp.common.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class AuditAspect {

    @Before("@annotation(auditable)")
    public void auditAction(JoinPoint joinPoint, Auditable auditable) {
        log.info("Audit log: {} - {}", auditable.action(), joinPoint.getSignature().getName());
    }
}
