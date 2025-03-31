package com.hrms.tenant_management.repository;

import com.hrms.tenant_management.dao.KeycloakClientConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeycloakClientConfigRepo extends JpaRepository<KeycloakClientConfig, Long> {

    KeycloakClientConfig getKeycloakClientConfigByRealm(String realm);

    KeycloakClientConfig findKeycloakClientConfigById(String clientUId);

    KeycloakClientConfig findByRealm(String realmName);
}
