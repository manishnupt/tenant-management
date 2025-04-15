package com.hrms.tenant_management.dao;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role_group_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleModuleMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role", nullable = false, length = 100)
    private String role;

    @Column(name = "module", nullable = false, length = 100)
    private String module;
}

