package com.hrms.tenant_management.dao;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeycloakClientConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long configId;
    private String id;
    private String clientId;
    private String clientSecret;
    private UUID tenantId;
    private String realm;
    private String username;
    private String password;
    private Timestamp createdDate;
    private Timestamp modifiedDate;

}
