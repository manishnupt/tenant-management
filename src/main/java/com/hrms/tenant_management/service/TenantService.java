package com.hrms.tenant_management.service;


import com.hrms.tenant_management.dao.ClientDbConnectionData;
import com.hrms.tenant_management.dao.KeycloakClientConfig;
import com.hrms.tenant_management.dao.Tenant;
import com.hrms.tenant_management.dto.ClientDbResponse;
import com.hrms.tenant_management.dto.TenantOnboardingUiRequest;
import com.hrms.tenant_management.dto.TenantUiResponse;
import com.hrms.tenant_management.repository.CliendDbRepo;
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

        // Step 3: Run Liquibase Changelog
        /*try {
            runLiquibaseChangelog(tenantDbUrl, randomUsername, randomPassword);
        } catch (LiquibaseException | SQLException e) {
            log.error("error executing the liquibase changelog");
            e.printStackTrace();
            throw new TenantOnboardingException(ErrorCodes.LIQUIBASE_ERROR,"Error executing Liquibase changelog", e);
        }*/

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

    public TenantUiResponse createTenant(TenantOnboardingUiRequest tenantUiRequest) {
        log.info("stared the process to onboard tenant");
        Tenant tenant=validateAndConvertTenantRequest(tenantUiRequest);
        log.info("Request validated,stared the process to save tenant");
        Tenant savedTenant = saveTenant(tenant);
        log.info("Tenant saved,stared the process of keycloak onboarding");
        String userId=handleKeycloakIntegration(savedTenant);
        log.info("Tenant onboarding in keycloak complete, onboarded user with id :{}. Starting to setup tenant db",userId);
        Map<String, String> tenantDatabase = createTenantDatabase(savedTenant.getName());
        log.info("tenant db setup complete saving db details to meta");
        saveTenantDatabaseDetails(tenantDatabase,savedTenant.getId(),savedTenant.getName());
        log.info("process started to onboard user to tenant's db");
        //persistUserToTenantDb(userId,tenantDatabase,tenantUiRequest);
        log.info("tenant onboarded successfully");
        return TenantOnboardingHelper.convertToTenantUiResponse(savedTenant);
    }

    private Tenant validateAndConvertTenantRequest(TenantOnboardingUiRequest tenantUiRequest) {
        if (existsByName(tenantUiRequest.getName())) {
            log.info("tenant name needs to be unique");
            throw new IllegalArgumentException("Tenant name must be unique");
        }
        return TenantOnboardingHelper.convertRequestToTenantEntity(tenantUiRequest);
    }

    private String handleKeycloakIntegration(Tenant savedTenant) {
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
        keycloakServicev2.createRoles(token,savedTenant.getName(),Constants.roles);
        String groupId = keycloakServicev2.createGroup(token, savedTenant.getName(), "SUPER_ADMIN");
        keycloakServicev2.assignRolesToGroup(groupId,savedTenant.getName(),token);
        // done
        log.info("admin user onboarded to keycloak");
        String userId=keycloakServicev2.onboardKeycloakUser(savedTenant,newToken);
        log.info("granting admin rights to user with id :{}",userId);
        keycloakServicev2.grantAdminAccess(newToken,userId,savedTenant.getName(),groupId);
        log.info("saving client details in meta db");
        saveClientDetails(savedTenant,clientForRealm);
        return userId;
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


    private void persistUserToTenantDb(String userId, Map<String, String> tenantDatabase, TenantOnboardingUiRequest tenantUiRequest) {
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
                preparedStatement.setString(1, tenantUiRequest.getAdminEmail()); // email
                preparedStatement.setString(2, userId); // user_ref_id
                preparedStatement.setString(3, tenantUiRequest.getAdminUsername()); // user_name
                preparedStatement.setString(4,"Admin");
                preparedStatement.setObject(5, LocalDateTime.now()); // created_date
                preparedStatement.setObject(6, LocalDateTime.now()); // modified_date

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