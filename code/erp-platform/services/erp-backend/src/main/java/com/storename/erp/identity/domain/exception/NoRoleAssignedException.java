package com.storename.erp.identity.domain.exception;

public class NoRoleAssignedException extends RuntimeException {
    public NoRoleAssignedException(String message) {
        super(message);
    }
}
