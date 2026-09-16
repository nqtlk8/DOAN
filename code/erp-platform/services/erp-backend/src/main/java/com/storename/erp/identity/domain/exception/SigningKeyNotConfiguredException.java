package com.storename.erp.identity.domain.exception;

public class SigningKeyNotConfiguredException extends RuntimeException {
    public SigningKeyNotConfiguredException(String message) {
        super(message);
    }
}
