package com.hrms.tenant_management.utils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public class RequestContextUtil {
    public static String getAuthToken() {
        return (String) RequestContextHolder.currentRequestAttributes().getAttribute("Authorization", RequestAttributes.SCOPE_REQUEST);
    }
    public static void setAuthToken(String authToken) {
        RequestContextHolder.currentRequestAttributes()
                .setAttribute("Authorization", authToken, RequestAttributes.SCOPE_REQUEST);
    }

}