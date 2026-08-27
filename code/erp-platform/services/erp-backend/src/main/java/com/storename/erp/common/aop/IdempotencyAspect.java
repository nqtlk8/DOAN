package com.storename.erp.common.aop;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storename.erp.common.domain.IdempotencyRecord;
import com.storename.erp.common.infrastructure.IdempotencyRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final IdempotencyRecordRepository idempotencyRepo;
    private final ObjectMapper objectMapper;
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @Around("@annotation(IdempotencyProtected)")
    public Object checkIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();
        String idempotencyKey = request.getHeader("Idempotency-Key");

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        // Tính Request Hash từ Method + URI + Payload (args)
        java.util.List<Object> serializableArgs = java.util.Arrays.stream(joinPoint.getArgs())
            .filter(arg -> !(arg instanceof jakarta.servlet.http.HttpServletRequest) && !(arg instanceof jakarta.servlet.http.HttpServletResponse))
            .collect(java.util.stream.Collectors.toList());
        String payload = objectMapper.writeValueAsString(serializableArgs);
        String rawToHash = request.getMethod() + ":" + request.getRequestURI() + ":" + payload;
        String requestHash = org.springframework.util.DigestUtils.md5DigestAsHex(rawToHash.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        try {
            return transactionTemplate.execute(status -> {
                Optional<IdempotencyRecord> existing = idempotencyRepo.findByIdempotencyKey(idempotencyKey);
                if (existing.isPresent()) {
                    if (!requestHash.equals(existing.get().getRequestHash())) {
                        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Idempotency key reused for different request");
                    }
                    log.info("Idempotency key '{}' already processed, returning cached response", idempotencyKey);
                    return deserializeResponse(existing.get().getResponseSnapshot(), joinPoint);
                }

                try {
                    Object result = joinPoint.proceed();
                    IdempotencyRecord record = new IdempotencyRecord();
                    record.setIdempotencyKey(idempotencyKey);
                    record.setRequestHash(requestHash);
                    record.setResponseSnapshot(objectMapper.writeValueAsString(result));
                    idempotencyRepo.save(record);
                    return result;
                } catch (Throwable e) {
                    if (e instanceof RuntimeException) throw (RuntimeException) e;
                    if (e instanceof Error) throw (Error) e;
                    throw new RuntimeException(e);
                }
            });
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Race condition: another thread inserted the key
            log.info("Idempotency key '{}' inserted by another thread, recovering snapshot...", idempotencyKey);
            Optional<IdempotencyRecord> existing = idempotencyRepo.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                if (!requestHash.equals(existing.get().getRequestHash())) {
                    throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Idempotency key reused for different request");
                }
                return deserializeResponse(existing.get().getResponseSnapshot(), joinPoint);
            }
            throw e;
        }
    }

    private Object deserializeResponse(String snapshot, ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            JavaType javaType = objectMapper.getTypeFactory().constructType(signature.getMethod().getGenericReturnType());
            return objectMapper.readValue(snapshot, javaType);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to deserialize idempotency response", ex);
        }
    }
}
