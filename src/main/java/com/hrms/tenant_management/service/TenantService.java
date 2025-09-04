package com.hrms.tenant_management.service;


import com.hrms.tenant_management.dao.ClientDbConnectionData;
import com.hrms.tenant_management.dao.KeycloakClientConfig;
import com.hrms.tenant_management.dao.RoleModuleMapping;
import com.hrms.tenant_management.dao.Tenant;
import com.hrms.tenant_management.dto.ClientDbResponse;
import com.hrms.tenant_management.dto.TenantOnboardingUiRequest;
import com.hrms.tenant_management.dto.TenantUiResponse;
import com.hrms.tenant_management.repository.CliendDbRepo;
import com.hrms.tenant_management.repository.RoleModuleMapRepo;
import com.hrms.tenant_management.repository.TenantRepo;
import com.hrms.tenant_management.utils.*;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.validation.constraints.NotBlank;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;



import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Log4j2
public class TenantService {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private CliendDbRepo cliendDbRepo;

    @Autowired
    private TenantRepo tenantRepo;

    @Autowired
    private KeycloakServiceV2 keycloakServicev2;

    @Value("${tenant.liquibase.changeLog}")
    private String tenantChangelogPath;

    // change the below logic _P1
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${admin_service_base_url}")
    private String adminUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RoleModuleMapRepo roleModuleMapRepo;

    public TenantService(){
        log.info("Tenant service");
    }


    public Map<String, String> createTenantDatabase(String tenantDbName) {
        log.info("Start the process of creating tenant specific db");
        String randomUsername = "user_" + CommonUtils.generateRandomString(8).toLowerCase();
        String randomPassword = CommonUtils.generateRandomString(12);

        // Step 1: Create Tenant Database
        createDatabaseInMainDb(tenantDbName);

        // Step 2: Create User and Grant Privileges in Tenant DB
        int lastSlashIndex = dbUrl.lastIndexOf("/");
        String extractedUrl = (lastSlashIndex != -1) ? dbUrl.substring(0, lastSlashIndex+1) : dbUrl;
        String tenantDbUrl = extractedUrl + tenantDbName;
        try (Connection tenantConnection = DriverManager.getConnection(tenantDbUrl, dbUsername ,dbPassword)) {
            executeTenantDbSetup(tenantConnection, randomUsername,tenantDbName,randomPassword);
        } catch (SQLException e) {
            log.error("error connecting the tenant db ,{}",e);
            e.printStackTrace();
            throw new TenantOnboardingException(ErrorCodes.DB_SETUP_ERROR,"Error connecting to tenant database", e);
        }

         //Step 3: Run Liquibase Changelog
        try {
            runLiquibaseChangelog(tenantDbUrl, randomUsername, randomPassword);
        } catch (LiquibaseException | SQLException e) {
            log.error("error executing the liquibase changelog");
            e.printStackTrace();
            throw new TenantOnboardingException(ErrorCodes.LIQUIBASE_ERROR,"Error executing Liquibase changelog", e);
        }

        // Return database connection details
        return createTenantDbResponse(tenantDbName,tenantDbUrl, randomUsername, randomPassword);
    }

    private Map<String, String> createTenantDbResponse(String tenantDbName,String tenantDbUrl, String randomUsername, String randomPassword) {
        Map<String, String> response = new HashMap<>();
        response.put("url", tenantDbUrl);
        response.put("user", randomUsername);
        response.put("password", randomPassword);
        return response;
    }

    private void executeTenantDbSetup(Connection tenantConnection, String randomUsername, String tenantDbName, String randomPassword) throws SQLException {
        log.info("starting the tenant db setup");
        try (Statement tenantStatement = tenantConnection.createStatement()) {
            tenantStatement.executeUpdate(String.format(Constants.CREATE_USER, randomUsername, randomPassword));
            tenantStatement.executeUpdate(String.format(Constants.GRANT_ACCESS, tenantDbName, randomUsername));
            tenantStatement.executeUpdate(String.format(Constants.GRANT_SCHEMA_USAGE, randomUsername));
            tenantStatement.executeUpdate(String.format(Constants.GRANT_EXISTING_TABLES, randomUsername));
            tenantStatement.executeUpdate(String.format(Constants.GRANT_FUTURE_TABLES, randomUsername));
            tenantStatement.executeUpdate(String.format(Constants.NEW_USER_OWNER, randomUsername));// Change schema owner
            log.info("done setting up tenant db setup");
        }
        catch(Exception e){
            e.printStackTrace();
            log.info("Error doing the setup :{}",e);
            throw new TenantOnboardingException(ErrorCodes.DB_SETUP_ERROR,"granting privileges to db user failed",e);
        }
    }


    private void createDatabaseInMainDb(String tenantDbName) {
        log.info("Creating the database :{}",tenantDbName);
        String createDbQuery = "CREATE DATABASE " + tenantDbName;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createDbQuery);
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Error creating tenant db in main db ,{}",e);
            throw new TenantOnboardingException(ErrorCodes.DB_CREATION_ERROR,"Error creating tenant database", e);
        }
    }


    private void runLiquibaseChangelog(String url, String randomUsername, String randomPassword) throws LiquibaseException, SQLException {
        log.info("started to run the liquibase change log");
        Connection connection = null;
        Liquibase liquibase = null;
        SpringLiquibase liquibase1 = new SpringLiquibase();
        liquibase1.setChangeLog(tenantChangelogPath);
        liquibase1.setDataSource(dataSource(url,randomUsername,randomPassword));
        liquibase1.afterPropertiesSet();
    }

    private DataSource dataSource(String url, String randomUsername, String randomPassword) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(randomUsername);
        dataSource.setPassword(randomPassword);
        dataSource.setDriverClassName(Constants.ORG_POSTGRESQL_DRIVER);
        return dataSource;
    }


    public void saveTenantDatabaseDetails(Map<String, String> clientDbConnectionData, UUID tenantId,String tenantName) {
        ClientDbConnectionData clientDbConnectionData1 = TenantOnboardingHelper.getClientDbConnectionData(clientDbConnectionData, tenantId,tenantName);
        cliendDbRepo.save(clientDbConnectionData1);
    }



    public Tenant saveTenant(Tenant tenant) {
        return tenantRepo.save(tenant);
    }

    public boolean existsByName(@NotBlank(message = "Name cannot be blank") String name) {
        return tenantRepo.existsByName(name);
    }

    public List<Tenant> findAll() {
        return tenantRepo.findAll();
    }

    @Transactional
    public TenantUiResponse createTenant(TenantOnboardingUiRequest tenantUiRequest) {
        log.info("stared the process to onboard tenant");
        Tenant tenant=validateAndConvertTenantRequest(tenantUiRequest);
        log.info("Request validated,stared the process to save tenant");
        Tenant savedTenant = saveTenant(tenant);
        tenantUiRequest.getModules().add("groups");
        List<String> roles =getAllApplicableRoles(tenantUiRequest.getModules());
        log.info("got roles :{}",roles);
        log.info("Tenant saved,stared the process of keycloak onboarding");
        Map<String,String> responseData=handleKeycloakIntegration(savedTenant,roles);
        log.info("Tenant onboarding in keycloak complete, onboarded user with id :{}. Starting to setup tenant db",responseData.get("USER_ID"));
        Map<String, String> tenantDatabase = createTenantDatabase(savedTenant.getName());
        log.info("tenant db setup complete saving db details to meta");
        saveTenantDatabaseDetails(tenantDatabase,savedTenant.getId(),savedTenant.getName());
        log.info("process started to onboard user to tenant's db");
        persistRoleToTenantTable(tenantUiRequest.getModules(),tenantDatabase,tenantUiRequest);
        Long groupId = persistGroupAndAssignRolesToTenantTable(tenantDatabase, "SUPER_ADMIN", "group containing all the features your organisation has opted for",responseData.get("KC_GROUP_ID"));
        persistUserToTenantDb(responseData.get("USER_ID"),tenantDatabase,tenantUiRequest,groupId);
        log.info("tenant onboarded successfully");
        return TenantOnboardingHelper.convertToTenantUiResponse(savedTenant);
    }

    private Long persistGroupAndAssignRolesToTenantTable(Map<String, String> tenantDatabase, String group,String groupDescription,String kcGroupId) {
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection tenantConnection = DriverManager.getConnection(
                    tenantDatabase.get("url"),
                    tenantDatabase.get("user"),
                    tenantDatabase.get("password"))) {

                // Create transaction to ensure atomicity
                tenantConnection.setAutoCommit(false);

                try {
                    // 1. Insert the role group
                    Long roleGroupId = insertRoleGroup(tenantConnection, group, groupDescription,kcGroupId);

                    // 2. Get all roles from the role table
                    List<Long> roleIds = getAllRoleIds(tenantConnection);

                    // 3. Assign all roles to the role group
                    assignRolesToGroup(tenantConnection, roleGroupId, roleIds);

                    // Commit the transaction
                    tenantConnection.commit();
                    log.info("Successfully created role group '{}' with {} roles assigned", group, roleIds.size());
                    return roleGroupId;
                } catch (SQLException e) {
                    // Rollback in case of error
                    tenantConnection.rollback();
                    log.error("Error creating role group. Transaction rolled back.", e);
                    throw new TenantOnboardingException(ErrorCodes.DB_INSERT_ERROR, "Error creating role group", e);
                } finally {
                    tenantConnection.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error("Error connecting to tenant database", e);
                throw new TenantOnboardingException(ErrorCodes.DB_CONNECTION_ERROR, "Error connecting to tenant database", e);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL driver not found", e);
        }

    }

    private List<Long> getAllRoleIds(Connection tenantConnection) throws SQLException {
            List<Long> roleIds = new ArrayList<>();
            String selectQuery = "SELECT id FROM role";

            try (PreparedStatement preparedStatement = tenantConnection.prepareStatement(selectQuery);
                 ResultSet resultSet = preparedStatement.executeQuery()) {

                while (resultSet.next()) {
                    roleIds.add(resultSet.getLong("id"));
                }
            }
            return roleIds;
    }
    private void assignRolesToGroup(Connection connection, Long roleGroupId, List<Long> roleIds) throws SQLException {
        String insertQuery = "INSERT INTO role_group_roles (role_group_id, role_id) VALUES (?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            for (Long roleId : roleIds) {
                preparedStatement.setLong(1, roleGroupId);
                preparedStatement.setLong(2, roleId);
                preparedStatement.addBatch();
            }

            int[] batchResults = preparedStatement.executeBatch();
            log.info("Assigned {} roles to role group {}", batchResults.length, roleGroupId);
        }
    }



    private Long insertRoleGroup(Connection tenantConnection, String group, String groupDescription,String kcGroupId)throws SQLException  {
        String insertQuery = "INSERT INTO role_group (name, description,kc_group_id_ref, creation_date, last_modification_date) VALUES (?, ?, ?, ?,?) RETURNING id";

        try (PreparedStatement preparedStatement = tenantConnection.prepareStatement(insertQuery)) {
            preparedStatement.setString(1, group);
            preparedStatement.setString(2, groupDescription);
            preparedStatement.setString(3,kcGroupId);
            LocalDateTime now = LocalDateTime.now();
            preparedStatement.setTimestamp(4, Timestamp.valueOf(now));
            preparedStatement.setTimestamp(5, Timestamp.valueOf(now));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                } else {
                    throw new SQLException("Failed to retrieve role group id after insertion");
                }
            }
        }

    }

    private void persistRoleToTenantTable(List<String> modules, Map<String, String> tenantDatabase, TenantOnboardingUiRequest tenantUiRequest)  {
        List<RoleModuleMapping> allRolesByModules = getAllRolesByModules(modules);
        log.info("got {} roles as per modules ",allRolesByModules.size());
        String insertQuery = Constants.INSERT_ROLE_QUERY;
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection tenantConnection = DriverManager.getConnection(
                    tenantDatabase.get("url"),
                    tenantDatabase.get("user"),
                    tenantDatabase.get("password"));
                 PreparedStatement preparedStatement = tenantConnection.prepareStatement(insertQuery)) {

                for (RoleModuleMapping role : allRolesByModules) {
                    // Set the parameters for the query
                    preparedStatement.setString(1, role.getRole());
                    preparedStatement.setString(2, role.getModule());
                    preparedStatement.setString(3, role.getDescription());
                    int rowsAffected = preparedStatement.executeUpdate();
                    log.info("rows affected:{}",rowsAffected);
                }
            }  catch (SQLException e) {
                e.printStackTrace();
                throw new TenantOnboardingException(ErrorCodes.DB_INSERT_ERROR, "Error connecting to tenant database or executing query", e);
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private List<RoleModuleMapping> getAllRolesByModules(List<String> modules) {
        return roleModuleMapRepo.getAllRoleByModule(modules);
    }

    private List<String> getAllApplicableRoles(List<String> modules) {
        return roleModuleMapRepo.findRolesByModules(modules);
    }

    private Tenant validateAndConvertTenantRequest(TenantOnboardingUiRequest tenantUiRequest) {
        if (existsByName(tenantUiRequest.getName())) {
            log.info("tenant name needs to be unique");
            throw new IllegalArgumentException("Tenant name must be unique");
        }
        return TenantOnboardingHelper.convertRequestToTenantEntity(tenantUiRequest);
    }

    private Map<String,String> handleKeycloakIntegration(Tenant savedTenant, List<String> roles) {
        Map<String,String> responseData = new HashMap<String,String>();
        log.info("getting master realm details from db");
        KeycloakClientConfig clientByRealm = keycloakServicev2.getClientByRealm("master");
        log.info("getting admin token for master realm");
        String token=keycloakServicev2.getAdminAccessToken(clientByRealm);
        RequestContextUtil.setAuthToken(token);
        log.info("starting to create keycloak realm");
        keycloakServicev2.createRealm(savedTenant.getName(),token);
        log.info("getting new admin access token");
        String newToken=keycloakServicev2.getAdminAccessToken(clientByRealm);
        log.info("creating myca-client in the new realm");
        Map<String,String> clientForRealm = keycloakServicev2.createClient(newToken,savedTenant.getName());
        //new code
        keycloakServicev2.createRoles(token,savedTenant.getName(),roles);
        String groupId = keycloakServicev2.createGroup(token, savedTenant.getName(), "SUPER_ADMIN");
        keycloakServicev2.assignRolesToGroup(groupId,savedTenant.getName(),token,roles);
        // done
        log.info("admin user onboarded to keycloak");
        String userId=keycloakServicev2.onboardKeycloakUser(savedTenant,newToken);
        log.info("granting admin rights to user with id :{}",userId);
        keycloakServicev2.grantAdminAccess(newToken,userId,savedTenant.getName(),groupId);
        log.info("saving client details in meta db");
        saveClientDetails(savedTenant,clientForRealm);
        responseData.put("USER_ID",userId);
        responseData.put("KC_GROUP_ID",groupId);
        return responseData;
    }

    private void saveClientDetails(Tenant savedTenant, Map<String, String> clientForRealm) {
        KeycloakClientConfig keycloakClientConfig = TenantOnboardingHelper.getKeycloakClientConfig(savedTenant, clientForRealm);
        keycloakServicev2.saveClientDetails(keycloakClientConfig);
    }

    public List<TenantUiResponse> getAllTenants() {
        List<Tenant> tenants = findAll();
        List<TenantUiResponse> tenantUiResponses = new ArrayList<>();
        for (Tenant tenant : tenants) {
            tenantUiResponses.add(TenantOnboardingHelper.convertToTenantUiResponse(tenant));
        }
        return tenantUiResponses;
    }


    private void persistUserToTenantDb(String userId, Map<String, String> tenantDatabase, TenantOnboardingUiRequest tenantUiRequest,Long groupId) {
       // log.info("starting process to ")
        String insertQuery = Constants.INSERT_USER_QUERY;
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection tenantConnection = DriverManager.getConnection(
                    tenantDatabase.get("url"),
                    tenantDatabase.get("user"),
                    tenantDatabase.get("password"));
                 PreparedStatement preparedStatement = tenantConnection.prepareStatement(insertQuery)) {

                // Set the parameters for the query
                preparedStatement.setString(1,userId);
                preparedStatement.setString(2,tenantUiRequest.getAdminFirstName()+ " "+ tenantUiRequest.getAdminLastName()); // email
                preparedStatement.setString(3,  tenantUiRequest.getAdminUsername()); // user_ref_id
                preparedStatement.setString(4, tenantUiRequest.getAdminEmail()); // user_name
                //preparedStatement.setString(5,userId);
                preparedStatement.setLong(5,groupId);
               // preparedStatement.setObject(5, LocalDateTime.now()); // created_date
                //preparedStatement.setObject(6, LocalDateTime.now()); // modified_date

                // Execute the query
                int rowsAffected = preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                throw new TenantOnboardingException(ErrorCodes.DB_INSERT_ERROR,"Error connecting to tenant database or executing query", e);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }

    public List<ClientDbResponse> getDbConnections() {
        List<ClientDbConnectionData> allDbConnections = cliendDbRepo.findAll();
        List<ClientDbResponse> allDbDataResponse= new ArrayList<>();
        for(ClientDbConnectionData connection:allDbConnections){
            ClientDbResponse dbResponse= ClientDbResponse.builder().
                    tenantId(connection.getTenantName()) //This needs to be changed anyhow priority 1 :
                    .dbUrl(connection.getDbUrl())
                    .username(connection.getUsername())
                    .password(connection.getPassword())
                    .build();
            allDbDataResponse.add(dbResponse);
        }
        return allDbDataResponse;
    }
}