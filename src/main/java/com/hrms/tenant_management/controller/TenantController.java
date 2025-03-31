package com.hrms.tenant_management.controller;



import com.hrms.tenant_management.dto.*;
import com.hrms.tenant_management.service.KeycloakServiceV2;
import com.hrms.tenant_management.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {
    @Autowired
    TenantService tenantService;

    @Autowired
    KeycloakServiceV2 keycloakServiceV2;

    @PostMapping
    public ResponseEntity<?> createTenant(@RequestBody TenantOnboardingUiRequest uiRequest) {
        TenantUiResponse tenant = tenantService.createTenant(uiRequest);
        BaseResponse<TenantUiResponse> response = new BaseResponse<>(
                201,
                "Tenant created successfully",
                tenant
        );
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<?> getAllTenants() {
        List<TenantUiResponse> tenants = tenantService.getAllTenants();
        BaseResponse<List<TenantUiResponse>> response = new BaseResponse<>(
                200,
                "Fetched all tenants",
                tenants
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping(path = "/databases")
    public ResponseEntity<?> getClientdbs() {
        List<ClientDbResponse> clientDbResponse = tenantService.getDbConnections();
        BaseResponse<List<ClientDbResponse>> response = new BaseResponse<>(
                200,
                "Fetched all db connections",
                clientDbResponse
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping(path="/auth-config/{realm}")
    public ResponseEntity<?> getKeycloakConfig(@PathVariable String realm){
        KeycloakConfigResponse keycloakConfig=keycloakServiceV2.getKeycloakConfig(realm);
        BaseResponse<KeycloakConfigResponse> response = new BaseResponse<>(
                200,
                "Fetched keycloak config",
                keycloakConfig
        );
        return ResponseEntity.ok(response);

    }

}
