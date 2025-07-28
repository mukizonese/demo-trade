package com.tradingzone.services.watchlist.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.UnifiedJedis;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

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
        } else {
            log.error("cache {} key {} not found ", cache, key);
        }

        return symMap;
    }

    public boolean addSymbol(String cache, String key, String symbol ) {

        String symbols = unifiedJedis.hget(cache, key);

        if (symbols != null ) {
            if ( !symbols.contains(symbol)) {
                symbols = symbols + "," + symbol;
                unifiedJedis.hset(cache, key, symbols);
                log.info("cache {} key {} symbol {} added succesfully.",cache,key, symbol);
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
                if (result.isEmpty()) {
                    // If no symbols left, delete the entire watchlist entry
                    unifiedJedis.hdel(cache, key);
                } else {
                    unifiedJedis.hset(cache, key, result);
                }
                
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
        
        // Check for up to 5 watchlists per user
        for (int i = 1; i <= 5; i++) {
            String key = tradingUserId + ":" + i;
            String symbols = unifiedJedis.hget("hash:watchlist", key);
            
            // Include watchlist if it exists (even if empty)
            if (symbols != null) {
                String[] symbolArray = getSymbols("hash:watchlist", key);
                userWatchlists.put("watchlist_" + i, symbolArray);
                log.info("Found watchlist {} for user {} with {} symbols", i, tradingUserId, symbolArray.length);
            }
        }
        
        return userWatchlists;
    }

    public boolean deleteWatchlist(String cache, String key) {
        try {
            Long result = unifiedJedis.hdel(cache, key);
            if (result > 0) {
                log.info("Successfully deleted watchlist: {}", key);
                return true;
            } else {
                log.warn("Watchlist {} not found for deletion", key);
                return false;
            }
        } catch (Exception e) {
            log.error("Error deleting watchlist {}: {}", key, e.getMessage());
            return false;
        }
    }

    public boolean createWatchlist(String cache, String key) {
        try {
            // Create an empty watchlist
            unifiedJedis.hset(cache, key, "");
            log.info("Successfully created empty watchlist: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Error creating watchlist {}: {}", key, e.getMessage());
            return false;
        }
    }

    public boolean renumberWatchlists(String tradingUserId) {
        try {
            // Get all existing watchlists for the user
            Map<String, String[]> userWatchlists = getUserWatchlists(tradingUserId);
            
            // Create a sorted list of watchlist IDs
            List<Integer> watchlistIds = userWatchlists.keySet().stream()
                .map(key -> Integer.parseInt(key.split("_")[1]))
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
