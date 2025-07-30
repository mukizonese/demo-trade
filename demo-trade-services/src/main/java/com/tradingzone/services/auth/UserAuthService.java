package com.tradingzone.services.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

@Slf4j
@Service
public class UserAuthService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${auth.api.base.url}")
    private String authApiBaseUrl;  
    //private static final String AUTH_API_BASE_URL = "http://demo-trade-auth-api:8050/api";  
    
    // Simple in-memory cache to reduce API calls
    private final Map<String, CachedUserInfo> userCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes

    public Integer getTradingUserId(String authToken) {
        try {
            // Check cache first
            CachedUserInfo cached = userCache.get(authToken);
            if (cached != null && !cached.isExpired()) {
                log.debug("Returning cached trading user ID: {}", cached.tradingUserId);
                return cached.tradingUserId;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", "auth_token=" + authToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                authApiBaseUrl + "/auth/me",
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> userData = (Map<String, Object>) response.getBody().get("user");
                if (userData != null && userData.containsKey("id")) {
                    String userId = userData.get("id").toString();
                    
                    // Get trading user ID mapping
                    ResponseEntity<Map> mappingResponse = restTemplate.exchange(
                        authApiBaseUrl + "/mapping/trading-user-id",
                        HttpMethod.GET,
                        entity,
                        Map.class
                    );
                    
                    if (mappingResponse.getStatusCode().is2xxSuccessful() && mappingResponse.getBody() != null) {
                        Map<String, Object> mappingData = mappingResponse.getBody();
                        if (mappingData.containsKey("trading_user_id")) {
                            Integer tradingUserId = Integer.valueOf(mappingData.get("trading_user_id").toString());
                            
                            // Cache the result
                            userCache.put(authToken, new CachedUserInfo(tradingUserId));
                            log.debug("Cached trading user ID: {} for token: {}", tradingUserId, authToken.substring(0, Math.min(20, authToken.length())));
                            
                            return tradingUserId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting trading user ID for token: {}", e.getMessage());
        }
        
        return null;
    }

    public String getUserRole(String authToken) {
        try {
            Map<String, Object> userInfo = getUserInfo(authToken);
            if (userInfo != null && userInfo.containsKey("user")) {
                Map<String, Object> userData = (Map<String, Object>) userInfo.get("user");
                if (userData != null && userData.containsKey("role")) {
                    return userData.get("role").toString();
                }
            }
        } catch (Exception e) {
            log.error("Error getting user role for token: {}", e.getMessage());
        }
        
        return "guest"; // Default to guest role
    }

    public boolean hasRole(String authToken, String requiredRole) {
        String userRole = getUserRole(authToken);
        
        // Role hierarchy: guest < trader < admin
        int userRoleLevel = getRoleLevel(userRole);
        int requiredRoleLevel = getRoleLevel(requiredRole);
        
        return userRoleLevel >= requiredRoleLevel;
    }

    private int getRoleLevel(String role) {
        switch (role.toLowerCase()) {
            case "admin":
                return 3;
            case "aitrader":
                return 2;
            case "trader":
                return 1;
            case "guest":
            default:
                return 0;
        }
    }

    public Map<String, Object> getUserInfo(String authToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Cookie", "auth_token=" + authToken);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                authApiBaseUrl + "/auth/me",
                HttpMethod.GET,
                entity,
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Error getting user info for token: {}", e.getMessage());
        }
        
        return null;
    }
    
    // Simple cache entry class
    private static class CachedUserInfo {
        private final Integer tradingUserId;
        private final long createdAt;
        
        public CachedUserInfo(Integer tradingUserId) {
            this.tradingUserId = tradingUserId;
            this.createdAt = Instant.now().getEpochSecond();
        }
        
        public Integer getTradingUserId() {
            return tradingUserId;
        }
        
        public boolean isExpired() {
            return Instant.now().getEpochSecond() - createdAt > CACHE_TTL_SECONDS;
        }
    }
} 