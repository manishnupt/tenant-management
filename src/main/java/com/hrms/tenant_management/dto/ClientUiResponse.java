package com.hrms.tenant_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientUiResponse {
    private String realm;
    private Long configId;
    private String id;
    private String clientId;
    private String clientSecret;
    private TenantUiResponse tenantDetail;

}
