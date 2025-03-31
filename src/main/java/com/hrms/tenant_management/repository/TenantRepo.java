package com.hrms.tenant_management.repository;

import com.hrms.tenant_management.dao.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TenantRepo extends JpaRepository<Tenant, UUID> {

    boolean existsByName(String name);
}