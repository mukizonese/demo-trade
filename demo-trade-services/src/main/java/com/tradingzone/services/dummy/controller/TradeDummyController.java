package com.tradingzone.services.dummy.controller;

import com.tradingzone.services.dummy.service.TradeDummyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/tradingzone/dummy")
public class TradeDummyController {

    @Autowired
    private TradeDummyService tradeDummyService;
    
    // ========== FULL SYMBOL METHODS ==========

    @PostMapping("/initiate-full")
    public ResponseEntity<Map<String, Object>> initiateDummyFull(@RequestParam String date) {
        try {
            log.info("Starting dummy full trading for date: {}", date);
            tradeDummyService.initiateDummyFull(date);
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Dummy full trading started",
                "date", date
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting dummy full trading for date: {}", date, e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to start dummy full trading: " + e.getMessage(),
                "date", date
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/clear-full")
    public ResponseEntity<Map<String, Object>> clearDummyFull(@RequestParam String date) {
        try {
            log.info("Clearing dummy full trading for date: {}", date);
            tradeDummyService.clearDummyFull(date);
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Dummy full trading cleared",
                "date", date
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing dummy full trading for date: {}", date, e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to clear dummy full trading: " + e.getMessage(),
                "date", date
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/clear-full-cleanup")
    public ResponseEntity<Map<String, Object>> clearFullCleanup() {
        try {
            log.info("Starting full cleanup of all manually created dummy trades across all symbols");
            Map<String, Object> cleanupResult = tradeDummyService.clearFullCleanup();
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Full cleanup completed",
                "result", cleanupResult
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during full cleanup", e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to perform full cleanup: " + e.getMessage()
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========== SINGLE WATCHLIST METHODS ==========

    @PostMapping("/initiate-watchlist")
    public ResponseEntity<Map<String, Object>> initiateDummyWatchList(
            @RequestParam String cache, 
            @RequestParam String key, 
            @RequestParam String date) {
        try {
            log.info("Starting dummy watchlist trading for cache: {} key: {} date: {}", cache, key, date);
            tradeDummyService.initiateDummyWatchList(cache, key, date);
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Dummy watchlist trading started",
                "cache", cache,
                "key", key,
                "date", date
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting dummy watchlist trading for cache: {} key: {} date: {}", cache, key, date, e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to start dummy watchlist trading: " + e.getMessage(),
                "cache", cache,
                "key", key,
                "date", date
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/clear-watchlist")
    public ResponseEntity<Map<String, Object>> clearDummyWatchList(
            @RequestParam String cache, 
            @RequestParam String key, 
            @RequestParam String date) {
        try {
            log.info("Clearing dummy watchlist trading for cache: {} key: {} date: {}", cache, key, date);
            tradeDummyService.clearDummyWatchList(cache, key, date);
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Dummy watchlist trading cleared",
                "cache", cache,
                "key", key,
                "date", date
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing dummy watchlist trading for cache: {} key: {} date: {}", cache, key, date, e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to clear dummy watchlist trading: " + e.getMessage(),
                "cache", cache,
                "key", key,
                "date", date
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========== SINGLE USER WATCHLISTS METHODS ==========

    @PostMapping("/initiate-user-watchlists")
    public ResponseEntity<Map<String, Object>> initiateDummyUserWatchlists(
            @RequestParam String userId, 
            @RequestParam String date) {
        try {
            log.info("Starting dummy user watchlists trading for user: {} date: {}", userId, date);
            tradeDummyService.initiateDummyUserWatchlists(userId, date);
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Dummy user watchlists trading started",
                "userId", userId,
                "date", date
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting dummy user watchlists trading for user: {} date: {}", userId, date, e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to start dummy user watchlists trading: " + e.getMessage(),
                "userId", userId,
                "date", date
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/clear-user-watchlists")
    public ResponseEntity<Map<String, Object>> clearDummyUserWatchlists(
            @RequestParam String userId, 
            @RequestParam String date) {
        try {
            log.info("Clearing dummy user watchlists trading for user: {} date: {}", userId, date);
            tradeDummyService.clearDummyUserWatchlists(userId, date);
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Dummy user watchlists trading cleared",
                "userId", userId,
                "date", date
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing dummy user watchlists trading for user: {} date: {}", userId, date, e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to clear dummy user watchlists trading: " + e.getMessage(),
                "userId", userId,
                "date", date
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========== MULTIPLE USERS WATCHLISTS METHODS ==========

    @PostMapping("/initiate-multiple-users-watchlists")
    public ResponseEntity<Map<String, Object>> initiateDummyMultipleUsersWatchlists(
            @RequestBody List<String> userIds, 
            @RequestParam String date) {
        try {
            log.info("Starting dummy multiple users watchlists trading for {} users date: {}", userIds.size(), date);
            tradeDummyService.initiateDummyMultipleUsersWatchlists(userIds, date);
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Dummy multiple users watchlists trading started",
                "userCount", userIds.size(),
                "userIds", userIds,
                "date", date
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error starting dummy multiple users watchlists trading for {} users date: {}", userIds.size(), date, e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to start dummy multiple users watchlists trading: " + e.getMessage(),
                "userCount", userIds.size(),
                "userIds", userIds,
                "date", date
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/clear-multiple-users-watchlists")
    public ResponseEntity<Map<String, Object>> clearDummyMultipleUsersWatchlists(
            @RequestBody List<String> userIds, 
            @RequestParam String date) {
        try {
            log.info("Clearing dummy multiple users watchlists trading for {} users date: {}", userIds.size(), date);
            tradeDummyService.clearDummyMultipleUsersWatchlists(userIds, date);
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Dummy multiple users watchlists trading cleared",
                "userCount", userIds.size(),
                "userIds", userIds,
                "date", date
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error clearing dummy multiple users watchlists trading for {} users date: {}", userIds.size(), date, e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to clear dummy multiple users watchlists trading: " + e.getMessage(),
                "userCount", userIds.size(),
                "userIds", userIds,
                "date", date
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========== SERVICE CONTROL METHODS ==========

    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopDummyService() {
        try {
            log.info("Stopping dummy trading service");
            tradeDummyService.stopDummyService();
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Dummy trading service stopped"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error stopping dummy trading service", e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to stop dummy trading service: " + e.getMessage()
            );
            return ResponseEntity.status(500).body(response);
        }
    }


     // ========== DEBUG ENDPOINTS ==========

    @GetMapping("/debug-symbol")
    public ResponseEntity<Map<String, Object>> debugSymbol(@RequestParam String symbol) {
        try {
            tradeDummyService.debugSymbolData(symbol);
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Debug info logged for symbol: " + symbol,
                "symbol", symbol
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error debugging symbol: {}", symbol, e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to debug symbol: " + e.getMessage(),
                "symbol", symbol
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    // ========== CONFIGURATION ENDPOINTS ==========

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        try {
            Map<String, Object> config = Map.of(
                "batchSize", 500,
                "createIntervalSeconds", 10,
                "deleteIntervalSeconds", 120,
                "baseVolatility", 0.02,
                "largeMoveProbability", 0.1,
                "largeMoveRange", 0.06,
                "trendBias", 0.005,
                "note", "Auto-start configuration is managed via application properties"
            );
            
            Map<String, Object> response = Map.of(
                "status", "SUCCESS",
                "message", "Current configuration",
                "config", config
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting configuration", e);
            Map<String, Object> response = Map.of(
                "status", "ERROR",
                "message", "Failed to get configuration: " + e.getMessage()
            );
            return ResponseEntity.status(500).body(response);
        }
    }
}
