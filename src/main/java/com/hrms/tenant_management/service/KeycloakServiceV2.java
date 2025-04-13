package com.hrms.tenant_management.service;

import com.hrms.tenant_management.dao.KeycloakClientConfig;
import com.hrms.tenant_management.repository.KeycloakClientConfigRepo;
import com.hrms.tenant_management.dao.Tenant;
import com.hrms.tenant_management.dto.GenerateTokenRequest;
import com.hrms.tenant_management.dto.KeycloakConfigResponse;
import com.hrms.tenant_management.dto.OnboardKeycloakUserRequest;
import com.hrms.tenant_management.utils.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Log4j2
public class KeycloakServiceV2 {

    @Value("${iam_service_base_url}")
    private String iamServiceBaseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private KeycloakClientConfigRepo keycloakClientConfigRepo;

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private <T> ResponseEntity<T> makeApiCall(String url, HttpMethod method, HttpEntity<?> entity, Class<T> responseType, int errorCode, String errorMessage) {
        try {
            return restTemplate.exchange(url, method, entity, responseType);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("{} - Status: {}, Response: {}", errorMessage, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new KeycloakException(errorCode, errorMessage + " - Status: " + e.getStatusCode(), e);
        }
    }

    private <T> ResponseEntity<T> makeApiCall(String url, HttpMethod method, HttpEntity<?> entity, ParameterizedTypeReference<T> responseType, int errorCode, String errorMessage) {
        try {
            return restTemplate.exchange(url, method, entity, responseType);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("{} - Status: {}, Response: {}", errorMessage, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new KeycloakException(errorCode, errorMessage + " - Status: " + e.getStatusCode(), e);
        }
    }

    public String getAdminAccessToken(KeycloakClientConfig clientByRealm) {
        String adminAccessTokenUrl = iamServiceBaseUrl + Constants.GET_ADMIN_TOKEN;
        GenerateTokenRequest tokenRequest = TenantOnboardingHelper.getGenerateTokenRequest(clientByRealm);
        HttpEntity<GenerateTokenRequest> entity = new HttpEntity<>(tokenRequest, createHeaders(null));

        ResponseEntity<Map<String, String>> response = makeApiCall(adminAccessTokenUrl, HttpMethod.POST, entity,
                new ParameterizedTypeReference<>() {}, ErrorCodes.KEYCLOAK_ADMIN_TOKEN_ERROR, "Failed to obtain admin access token");
        log.info("fetched the admin token succesfully");

        return response.getBody().get("token");
    }

    public void createRealm(String realmName, String token) {
        log.info("Started the process to create a realm in keycloak");
        String createRealmUrl = iamServiceBaseUrl + Constants.CREATE_REALM;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(createRealmUrl).queryParam("realmName", realmName);
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

        ResponseEntity<String> response = makeApiCall(uriBuilder.toUriString(), HttpMethod.POST, entity, String.class,
                ErrorCodes.KEYCLOAK_REALM_CREATION_ERROR, "Failed to create realm: " + realmName);
        log.info("Successfully created the realm");
    }

    public Map<String, String> createClient(String token, String realmName) {
        String url = iamServiceBaseUrl + Constants.CREATE_CLIENT;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url).queryParam("realmName", realmName);
        HttpEntity<String> entity = new HttpEntity<>(createHeaders(token));

        ResponseEntity<Map<String, String>> response = makeApiCall(uriBuilder.toUriString(), HttpMethod.POST, entity,
                new ParameterizedTypeReference<>() {}, ErrorCodes.KEYCLOAK_CLIENT_CREATION_ERROR, "Failed to create client in Keycloak");
        log.info("finished the process to create client in keycloak realm");
        return response.getBody();
    }

    public String onboardKeycloakUser(Tenant savedTenant, String token) {
        String url = iamServiceBaseUrl + Constants.ONBOARD_KEYCLOAK_USER;
        OnboardKeycloakUserRequest userRequest = TenantOnboardingHelper.getOnboardKeycloakUserRequest(savedTenant);
        HttpEntity<OnboardKeycloakUserRequest> entity = new HttpEntity<>(userRequest, createHeaders(token));

        ResponseEntity<Map<String, String>> response = makeApiCall(url, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {},
                ErrorCodes.KEYCLOAK_USER_ONBOARD_ERROR, "Failed to onboard Keycloak user");

        Map<String, String> responseBody = response.getBody();
        if (responseBody == null || !responseBody.containsKey("userId")) {
            throw new KeycloakException(ErrorCodes.KEYCLOAK_USER_ONBOARD_ERROR, "Missing user ID in response");
        }
        return responseBody.get("userId");
    }

    public void grantAdminAccess(String token, String userId, String realmName,String groupId) {
        String url = iamServiceBaseUrl + Constants.GRANT_ADMIN_ACCESS;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("realmName", realmName)
                .queryParam("userId", userId)
                .queryParam("groupId",groupId);

        HttpEntity<Void> entity = new HttpEntity<>(createHeaders(token));

        makeApiCall(uriBuilder.toUriString(), HttpMethod.GET, entity, Void.class,
                ErrorCodes.KEYCLOAK_ADMIN_ACCESS_ERROR, "Failed to grant admin access");
    }

    public KeycloakClientConfig getClientByRealm(String realmName) {
        return keycloakClientConfigRepo.getKeycloakClientConfigByRealm(realmName);
    }

    public KeycloakClientConfig saveClientDetails(KeycloakClientConfig clientConfig) {
        return keycloakClientConfigRepo.save(clientConfig);
    }

    public KeycloakConfigResponse getKeycloakConfig(String realmName) {
       KeycloakClientConfig keycloakCOnfig= keycloakClientConfigRepo.findByRealm(realmName);
       if(keycloakCOnfig!=null) {
           return KeycloakConfigResponse.builder().realm(keycloakCOnfig.getRealm()).
                   clientSecret(keycloakCOnfig.getClientSecret())
                   .clientId(keycloakCOnfig.getClientId())
                   .username(keycloakCOnfig.getUsername())
                   .password(keycloakCOnfig.getPassword())
                   .build();
       }
       else{
           throw new RuntimeException("No tenant present with name: "+ realmName);
       }
    }

    public void createRoles(String token, String realm, List<String> roles) {
        HttpHeaders headers = createHeaders(token);
        String createRolesUrl = iamServiceBaseUrl + Constants.CREATE_ROLES;
        String finalUrl= createRolesUrl+"?realmName=" + realm;
        String[] rolesArr=roles.toArray(new String[roles.size()]);

        HttpEntity<String[]> request = new HttpEntity<>(rolesArr, headers);

        restTemplate.exchange(
                finalUrl,
                HttpMethod.POST,
                request,
                String.class
        );
    }

    public String createGroup(String token, String realm, String group) {
        HttpHeaders headers = createHeaders(token);
        String createGroupUrl = iamServiceBaseUrl + Constants.CREATE_GROUP + "?groupName=" + URLEncoder.encode(group, StandardCharsets.UTF_8) + "&realmName=" + URLEncoder.encode(realm, StandardCharsets.UTF_8);

        // Parameters
      /*  Map<String, String> params = new HashMap<>();
        params.put("groupName", group);
        params.put("realmName", realm);*/

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> exchange = restTemplate.exchange(createGroupUrl, HttpMethod.POST, requestEntity, String.class);
        return String.valueOf(exchange.getBody());
    }

    public void assignRolesToGroup(String groupId, String realm, String token, List<String> roles) {
        HttpHeaders headers = createHeaders(token);
        String createGroupUrl = iamServiceBaseUrl + Constants.ASSIGN_GROUP_ROLES +"?groupId=" +URLEncoder.encode(groupId, StandardCharsets.UTF_8)+ "&realmName=" + URLEncoder.encode(realm, StandardCharsets.UTF_8);
        HttpEntity<List<String>> requestEntity = new HttpEntity<>(roles, headers);
        restTemplate.exchange(createGroupUrl, HttpMethod.POST, requestEntity, String.class).toString();
    }
}
