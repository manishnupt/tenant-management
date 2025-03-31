package com.hrms.tenant_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDbResponse {
    private String tenantId;
    private String dbUrl;
    private String username;
    private String password;
}
