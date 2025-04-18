package com.hrms.tenant_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardKeycloakUserRequest {
    private String password;
    private String email;
    private String userName;
    private String realmName;
    private boolean temporaryPassword;
}
