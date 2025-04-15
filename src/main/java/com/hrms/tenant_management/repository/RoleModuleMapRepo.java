package com.hrms.tenant_management.repository;

import com.hrms.tenant_management.dao.ClientDbConnectionData;
import com.hrms.tenant_management.dao.RoleModuleMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoleModuleMapRepo extends JpaRepository<RoleModuleMapping,Long> {


    @Query("SELECT r.role FROM RoleModuleMapping r WHERE r.module IN :modules")
    List<String> findRolesByModules(@Param("modules") List<String> modules);

    @Query("SELECT r FROM RoleModuleMapping r WHERE r.module IN :modules")
    List<RoleModuleMapping> getAllRoleByModule(List<String> modules);
}
