package com.hrms.tenant_management.repository;

import com.hrms.tenant_management.dao.ClientDbConnectionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CliendDbRepo extends JpaRepository<ClientDbConnectionData,Long> {

}
