package com.tradingzone.services.watchlist.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.UnifiedJedis;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class WatchListService {

    @Autowired
    private UnifiedJedis unifiedJedis;

    @Autowired
    private Gson gson;

    public String[] getSymbols(String cache, String key) {

        String[] symMap = new String[0];
        
        String symbols = unifiedJedis.hget(cache, key);

        if (symbols != null && !symbols.trim().isEmpty()) {
            // Split by comma and filter out empty strings
            String[] rawSymbols = symbols.split(",");
            symMap = java.util.Arrays.stream(rawSymbols)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        } else if (symbols != null) {
            // Watchlist exists but is empty
            log.debug("cache {} key {} is empty", cache, key);
        } else {
            // Watchlist doesn't exist, create it (only first watchlist gets default symbols)
            log.info("Watchlist not found for cache: {} key: {}, creating", cache, key);
            if (createWatchlist(cache, key)) {
                // Re-fetch the symbols after creation
                symbols = unifiedJedis.hget(cache, key);
                if (symbols != null && !symbols.trim().isEmpty()) {
                    String[] rawSymbols = symbols.split(",");
                    symMap = java.util.Arrays.stream(rawSymbols)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toArray(String[]::new);
                }
            } else {
                log.error("Failed to create watchlist for cache: {} key: {}", cache, key);
            }
        }

        return symMap;
    }

    public boolean addSymbol(String cache, String key, String symbol ) {

        String symbols = unifiedJedis.hget(cache, key);

        if (symbols != null ) {
            // Handle empty watchlist case
            if (symbols.trim().isEmpty()) {
                unifiedJedis.hset(cache, key, symbol);
                log.info("cache {} key {} symbol {} added to empty watchlist successfully.",cache,key, symbol);
                return true;
            }
            
            if ( !symbols.contains(symbol)) {
                symbols = symbols + "," + symbol;
                unifiedJedis.hset(cache, key, symbols);
                log.info("cache {} key {} symbol {} added successfully.",cache,key, symbol);
            }else{
                log.error("cache {} key {} symbol already exists symbol {} ",cache,key, symbol);
                return false;
            }
        }else{
            log.error("cache {} key {} not found ",cache,key);
            return false;
        }
        return true;
    }

    public boolean removeSymbol(String cache, String key, String symbol ) {

        String symbols = unifiedJedis.hget(cache, key);

        if (symbols != null ) {
            if ( symbols.contains(symbol)) {
                // Split by comma, remove the symbol, and rejoin
                String[] symbolArray = symbols.split(",");
                StringBuilder newSymbols = new StringBuilder();
                
                for (String sym : symbolArray) {
                    String trimmedSym = sym.trim();
                    if (!trimmedSym.isEmpty() && !trimmedSym.equals(symbol)) {
                        if (newSymbols.length() > 0) {
                            newSymbols.append(",");
                        }
                        newSymbols.append(trimmedSym);
                    }
                }
                
                // Update the cache with cleaned symbols
                String result = newSymbols.toString();
                // Keep empty watchlists instead of deleting them to prevent data loss on Redis restarts
                // Empty watchlists are represented as empty strings ""
                unifiedJedis.hset(cache, key, result);
                
                log.info("cache {} key {} symbol {} removed succesfully.",cache,key, symbol);
            }else{
                log.error("cache {} key {} symbol doesnt exists symbol {} ",cache,key, symbol);
                return false;
            }
        }else{
            log.error("cache {} key {} not found ",cache,key);
            return false;
        }
        return true;
    }

    public Map<String, String[]> getUserWatchlists(String tradingUserId) {
        Map<String, String[]> userWatchlists = new HashMap<>();
        
        try {
            // Ensure the first watchlist exists for the user
            ensureFirstWatchlistExists(tradingUserId);
            
            // Get all watchlists for the user
            String pattern = tradingUserId + ":*";
            Set<String> keys = unifiedJedis.hkeys("hash:watchlist");
            
            for (String key : keys) {
                if (key.startsWith(tradingUserId + ":")) {
                    String[] symbols = getSymbols("hash:watchlist", key);
                    userWatchlists.put(key, symbols);
                }
            }
            
            log.info("Found {} watchlists for user: {}", userWatchlists.size(), tradingUserId);
        } catch (Exception e) {
            log.error("Error getting watchlists for user {}: {}", tradingUserId, e.getMessage());
        }
        
        return userWatchlists;
    }

    /**
     * Ensure the first watchlist (ID: 1) exists for a user
     */
    private void ensureFirstWatchlistExists(String tradingUserId) {
        String firstWatchlistKey = tradingUserId + ":1";
        String existingWatchlist = unifiedJedis.hget("hash:watchlist", firstWatchlistKey);
        
        if (existingWatchlist == null) {
            log.info("First watchlist doesn't exist for user: {}, creating with default symbols", tradingUserId);
            createWatchlist("hash:watchlist", firstWatchlistKey);
        } else if (existingWatchlist.isEmpty() || existingWatchlist.trim().isEmpty()) {
            log.info("First watchlist exists but is empty for user: {}, adding default symbols", tradingUserId);
            addDefaultSymbols("hash:watchlist", firstWatchlistKey);
        }
    }

    public boolean deleteWatchlist(String cache, String key) {
        try {
            // Check if services are healthy before deleting
            if (!isServiceHealthy()) {
                log.warn("Services not healthy, skipping watchlist deletion for cache: {} key: {}", cache, key);
                return false;
            }
            
            log.info("Deleting watchlist cache: {} key: {}", cache, key);
            Long result = unifiedJedis.hdel(cache, key);
            boolean deleted = result != null && result > 0;
            log.info("Watchlist deletion result: {} for cache: {} key: {}", deleted, cache, key);
            return deleted;
        } catch (Exception e) {
            log.error("Error deleting watchlist cache: {} key: {}: {}", cache, key, e.getMessage());
            return false;
        }
    }

    private boolean isServiceHealthy() {
        try {
            // Simple Redis ping to check if services are ready
            String pingResult = unifiedJedis.ping();
            return "PONG".equals(pingResult);
        } catch (Exception e) {
            log.warn("Service health check failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean createWatchlist(String cache, String key) {
        try {
            // Check if services are healthy before creating
            if (!isServiceHealthy()) {
                log.warn("Services not healthy, skipping watchlist creation for cache: {} key: {}", cache, key);
                return false;
            }
            
            log.info("Creating watchlist cache: {} key: {}", cache, key);
            
            // Check if watchlist already exists
            String existingWatchlist = unifiedJedis.hget(cache, key);
            if (existingWatchlist != null) {
                log.info("Watchlist already exists for cache: {} key: {}", cache, key);
                return true;
            }
            
            // Create empty watchlist
            unifiedJedis.hset(cache, key, "");
            
            // Only add default symbols to the first watchlist (ID: 1)
            if (key.endsWith(":1")) {
                addDefaultSymbols(cache, key);
                log.info("Successfully created first watchlist with default symbols for cache: {} key: {}", cache, key);
            } else {
                log.info("Successfully created empty watchlist for cache: {} key: {}", cache, key);
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error creating watchlist cache: {} key: {}: {}", cache, key, e.getMessage());
            return false;
        }
    }

    /**
     * Add default symbols to a newly created watchlist
     */
    private void addDefaultSymbols(String cache, String key) {
        try {
            String[] defaultSymbols = {"CHEMPLASTS", "HDFCBANK", "RELIANCE", "SWIGGY", "INFY"};
            
            for (String symbol : defaultSymbols) {
                addSymbol(cache, key, symbol);
            }
            
            log.info("Added {} default symbols to watchlist cache: {} key: {}", defaultSymbols.length, cache, key);
        } catch (Exception e) {
            log.error("Error adding default symbols to watchlist cache: {} key: {}: {}", cache, key, e.getMessage());
        }
    }

    public boolean renumberWatchlists(String tradingUserId) {
        try {
            // Get all existing watchlists for the user
            Map<String, String[]> userWatchlists = getUserWatchlists(tradingUserId);
            
            // Create a sorted list of watchlist IDs
            List<Integer> watchlistIds = userWatchlists.keySet().stream()
                .map(key -> Integer.parseInt(key.split(":")[1]))
                .sorted()
                .collect(java.util.stream.Collectors.toList());
            
            // Renumber watchlists starting from 1
            for (int i = 0; i < watchlistIds.size(); i++) {
                int oldId = watchlistIds.get(i);
                int newId = i + 1;
                
                if (oldId != newId) {
                    String oldKey = tradingUserId + ":" + oldId;
                    String newKey = tradingUserId + ":" + newId;
                    
                    // Get symbols from old watchlist
                    String[] symbols = getSymbols("hash:watchlist", oldKey);
                    
                    // Create new watchlist with symbols
                    if (symbols.length > 0) {
                        String symbolsString = String.join(",", symbols);
                        unifiedJedis.hset("hash:watchlist", newKey, symbolsString);
                    } else {
                        unifiedJedis.hset("hash:watchlist", newKey, "");
                    }
                    
                    // Delete old watchlist
                    unifiedJedis.hdel("hash:watchlist", oldKey);
                    
                    log.info("Renumbered watchlist {} to {} for user {}", oldId, newId, tradingUserId);
                }
            }
            
            log.info("Successfully renumbered watchlists for user {}", tradingUserId);
            return true;
        } catch (Exception e) {
            log.error("Error renumbering watchlists for user {}: {}", tradingUserId, e.getMessage());
            return false;
        }
    }
}
