package com.hrms.tenant_management.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter

public class ErrorCodes {
        public static final int KEYCLOAK_ADMIN_TOKEN_ERROR = 1001;
        public static final int KEYCLOAK_REALM_CREATION_ERROR = 1002;
        public static final int KEYCLOAK_CLIENT_CREATION_ERROR = 1003;
        public static final int KEYCLOAK_USER_ONBOARD_ERROR = 1004;
        public static final int KEYCLOAK_ADMIN_ACCESS_ERROR = 1005;
        public static final int DB_USER_SETUP_ERROR = 1006;
        public static final int DB_CREATION_ERROR = 1007;
        public static final int DB_SETUP_ERROR = 1008;
        public static final int LIQUIBASE_ERROR = 1009;
        public static final int DB_SAVE_ERROR = 1010;
        public static final int TENANT_CREATION_ERROR = 1011;
        public static final int DB_INSERT_ERROR = 1012;
        public static final int BAD_REQUEST=1013;
        public static final int ILLEGAL_ARGUMENTS=1014;
        public static final int MASTER_REALM_SETUP_ERR=1015;

        public static final int DB_CONNECTION_ERROR =1111 ;
}
