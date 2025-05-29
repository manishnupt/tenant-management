package com.hrms.tenant_management.utils;

import java.util.Arrays;
import java.util.List;

public class Constants {


    public static final String GET_ADMIN_TOKEN = "/iamcontroller/keycloak-token";
    public static final String CREATE_REALM = "/iamcontroller/create-realm";
    public static final String CREATE_CLIENT = "/iamcontroller/create-client";
    public static final String ONBOARD_KEYCLOAK_USER = "/iamcontroller/onboard-first-user";
    public static final String GRANT_ADMIN_ACCESS = "/iamcontroller/grant-super-admin-access";

    public static final String CREATE_USER = "CREATE USER %s WITH PASSWORD '%s'";
    public static final String GRANT_ACCESS = "GRANT ALL PRIVILEGES ON DATABASE %s TO %s";
    public static final String GRANT_SCHEMA_USAGE = "GRANT USAGE ON SCHEMA public TO %s";
    public static final String NEW_USER_OWNER = "ALTER SCHEMA public OWNER TO %s";
    public static final String GRANT_EXISTING_TABLES = "GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO %s";
    public static final String GRANT_FUTURE_TABLES = "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL PRIVILEGES ON TABLES TO %s";
    public static final String ORG_POSTGRESQL_DRIVER = "org.postgresql.Driver";
    public static final String CREATE_ROLES="/create-roles";
    public static final String BASE_URL_EXTRACT_REGEX="jdbc:postgresql://[^/]+:\\d+";

    public static final String INSERT_USER_QUERY = "INSERT INTO public.employee (" +
            " employee_id,name, username, email,kc_reference_id,group_id) " +
            "VALUES (?, ?, ?, ?, ?, ?);";

    public static final String INSERT_ROLE_QUERY = "INSERT INTO public.role (" +
            " name, module,description) " +
            "VALUES (?, ?, ?);";

    public static final List<String> roles = Arrays.asList(
            "EMPLOYEE_MGMT",
            "HIRING_MGMT",
            "ASSET_MANAGEMENT",
            "EMPLOYEE_ADD",
            "EMPLOYEE_EDIT",
            "DEACTIVATE_EMPLOYEE",
            "TIMESHEET_ACCESS",
            "LEAVE_MGMT_ACCESS",
            "ASSET_REGISTRATION",
            "ASSET_ALLOCATION"
    );
    public static final String CREATE_GROUP = "/iamcontroller/create-group";
    public static final String ASSIGN_GROUP_ROLES ="/iamcontroller/assign-group-roles" ;
}
