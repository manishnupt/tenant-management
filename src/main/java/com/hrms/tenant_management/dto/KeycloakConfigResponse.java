package com.hrms.tenant_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakConfigResponse {

    private String clientId;
    private String clientSecret;
    private String realm;
    private String username;
    private String password;
}
