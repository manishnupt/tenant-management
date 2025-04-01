package com.hrms.tenant_management.utils;


import com.hrms.tenant_management.dao.ClientDbConnectionData;
import com.hrms.tenant_management.dao.KeycloakClientConfig;
import com.hrms.tenant_management.dao.Tenant;
import com.hrms.tenant_management.dto.*;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

public class TenantOnboardingHelper {

    public static Tenant convertRequestToTenantEntity(TenantOnboardingUiRequest tenantUiRequest) {
        Timestamp now = new Timestamp(System.currentTimeMillis()); // Get current timestamp

        return Tenant.builder().name(tenantUiRequest.getName())
                .adminEmail(tenantUiRequest.getAdminEmail())
                .adminUsername(tenantUiRequest.getAdminUsername())
                .adminPassword(tenantUiRequest.getAdminPassword())
                .createdDate(now)
                .modifiedDate(now)
                .build();
    }

    public static TenantUiResponse convertToTenantUiResponse(Tenant tenant) {
        return TenantUiResponse.builder().id(tenant.getId().toString())
                .name(tenant.getName())
                .build();
    }

    public static GenerateTokenRequest getGenerateTokenRequest(KeycloakClientConfig clientByRealm) {
       return GenerateTokenRequest.builder().
                password(clientByRealm.getPassword()).
                clientId(clientByRealm.getClientId()).
                username(clientByRealm.getUsername()).
                clientSecret(clientByRealm.getClientSecret()).
                build();}
    public static OnboardKeycloakUserRequest getOnboardKeycloakUserRequest(Tenant savedTenant) {
        return OnboardKeycloakUserRequest.builder().
                password(savedTenant.getAdminPassword()).
                userName(savedTenant.getAdminUsername()).
                email(savedTenant.getAdminEmail()).
                realmName(savedTenant.getName()).
                build();
    }
    public static ClientDbConnectionData getClientDbConnectionData(Map<String, String> clientDbConnectionData, UUID tenantId,String tenantName) {
        Timestamp now = new Timestamp(System.currentTimeMillis()); // Get current timestamp
        ClientDbConnectionData clientDbConnectionData1 =  ClientDbConnectionData.builder()
                .tenantId(tenantId)
                .dbUrl(clientDbConnectionData.get("url"))
                .username(clientDbConnectionData.get("user"))
                .password(clientDbConnectionData.get("password"))
                .tenantName(tenantName)
                .createdDate(now)
                .modifiedDate(now).build();
        return clientDbConnectionData1;
    }

    public static KeycloakClientConfig getKeycloakClientConfig(Tenant savedTenant, Map<String, String> clientForRealm) {
        Timestamp now = new Timestamp(System.currentTimeMillis()); // Get current timestamp
        KeycloakClientConfig clientConfig = KeycloakClientConfig.builder().
                tenantId(savedTenant.getId()).
                clientId(clientForRealm.get("clientId")).
                clientSecret(clientForRealm.get("clientSecret")).
                realm(savedTenant.getName()).
                id(clientForRealm.get("id")).
                username(savedTenant.getAdminUsername()).
                password(savedTenant.getAdminPassword())
                .createdDate(now)
                .modifiedDate(now).build();
        return clientConfig;
    }



}
