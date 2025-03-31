package com.hrms.tenant_management.utils;

public class TenantOnboardingException extends RuntimeException{
    private final int errorCode;

    public TenantOnboardingException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TenantOnboardingException(int errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
