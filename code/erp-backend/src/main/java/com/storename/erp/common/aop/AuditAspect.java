package com.storename.erp.common.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    @Before("@annotation(auditable)")
    public void auditAction(JoinPoint joinPoint, Auditable auditable) {
        System.out.println("Audit log: " + auditable.action() + " - " + joinPoint.getSignature().getName());
    }
}
