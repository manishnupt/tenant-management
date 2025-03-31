package com.hrms.tenant_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;



@Data
public class TenantOnboardingUiRequest {
    @NotBlank(message = "Name cannot be blank")
    @Pattern(regexp = "^[a-zA0-9]+$", message = "Name must be lower case alphanumeric without spaces")
    private String name;
    @NotBlank(message = "User name cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,20}$",
            message = "Username must be 3-20 characters long and can only contain letters, numbers, dots (.), underscores (_), and hyphens (-)")
    private String adminUsername;
    @NotBlank(message = "password cannot be blank")
    private String adminPassword;
    @NotBlank(message = "admin email cannot be blank")
    private String adminEmail;
    @NotBlank(message = "company name cannot be blank")
    private String companyFullName;
    private String industry;
    private String companySize;
    private String companyWebsite;
}
