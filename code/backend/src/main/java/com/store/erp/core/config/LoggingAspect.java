package com.store.erp.core.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Pointcut("within(com.store.erp.controller..*) || within(com.store.erp.service..*)")
    public void applicationPackagePointcut() {
        // Method is empty as this is just a Pointcut
    }

    @Around("applicationPackagePointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        if (log.isDebugEnabled()) {
            log.debug("Enter: {}() with argument[s] = {}", methodName, Arrays.toString(joinPoint.getArgs()));
        } else {
            log.info("Enter: {}", methodName);
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - start;
            
            if (log.isDebugEnabled()) {
                log.debug("Exit: {}() with result = {}, Execution time: {} ms", methodName, result, elapsedTime);
            } else {
                log.info("Exit: {}() Execution time: {} ms", methodName, elapsedTime);
            }
            
            return result;
        } catch (IllegalArgumentException e) {
            log.error("Illegal argument: {} in {}()", Arrays.toString(joinPoint.getArgs()), methodName);
            throw e;
        } catch (Throwable e) {
            long elapsedTime = System.currentTimeMillis() - start;
            log.error("Exception in {}() with cause = {}, Execution time: {} ms", methodName, e.getCause() != null ? e.getCause() : "NULL", elapsedTime, e);
            throw e;
        }
    }
}

