package com.hrms.tenant_management.utils;

import lombok.Data;

@Data
public class KeycloakException extends RuntimeException {
    private final int errorCode;

    public KeycloakException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public KeycloakException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}

