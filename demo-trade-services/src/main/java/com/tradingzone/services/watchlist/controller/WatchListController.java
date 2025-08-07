package com.tradingzone.services.watchlist.controller;

import com.tradingzone.services.auth.UserAuthService;
import com.tradingzone.services.redis.repositories.TradeJedisCache;
import com.tradingzone.services.redis.service.TradeJedisService;
import com.tradingzone.services.watchlist.service.WatchListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/tradingzone/watchlist")
public class WatchListController {

    @Autowired
    private TradeJedisService tradeJedisService;

    @Autowired
    private WatchListService watchListService;

    @Autowired
    private UserAuthService userAuthService;

    // Commented out - Not used by UI, unprotected APIs
    /*
    @GetMapping("/symbols")
    public String[] getSymbols(@RequestParam String cache, @RequestParam String key){
        //log.info("In WatchListController.getSymbols() "+ " cache > {} key > {}  ",cache,key);
        return watchListService.getSymbols(cache, key);
    }

    @PutMapping("/add/{symbol}")
    public boolean addSymbol(@RequestParam String cache, @RequestParam String key, @PathVariable String symbol){
        return watchListService.addSymbol(cache, key, symbol );
    }

    @PutMapping("/remove/{symbol}")
    public boolean removeSymbols(@RequestParam String cache, @RequestParam String key, @PathVariable String symbol){
        return watchListService.removeSymbol(cache, key, symbol );
    }

    @GetMapping("/trades")
    public List<TradeJedisCache> getWLTrades(@RequestParam String cache, @RequestParam String key, @RequestParam String date){
        //log.info("In WatchListController.getWLTrades() "+ " cache > {} key > {} date > {} ", cache, key, date);
        return tradeJedisService.fetchAll(cache, key, date);
    }
    */

    // Public API - Used by UI for latest prices
    @GetMapping("/latestprice/{symbol}")
    public TradeJedisCache fetchLatestPrice(@PathVariable String symbol){
        return tradeJedisService.fetchLatestPrice(symbol);
    }

    // Commented out - Not used by UI, unprotected APIs
    /*
    // New endpoints for user-specific watchlists
    @GetMapping("/user/{tradingUserId}/symbols")
    public String[] getUserSymbols(@PathVariable String tradingUserId, @RequestParam(defaultValue = "1") int watchlistId){
        String key = tradingUserId + ":" + watchlistId;
        log.info("Getting symbols for user: {} watchlist: {}", tradingUserId, watchlistId);
        return watchListService.getSymbols("hash:watchlist", key);
    }

    @GetMapping("/user/{tradingUserId}/watchlists")
    public Map<String, String[]> getUserWatchlists(@PathVariable String tradingUserId){
        log.info("Getting all watchlists for user: {}", tradingUserId);
        return watchListService.getUserWatchlists(tradingUserId);
    }

    @PutMapping("/user/{tradingUserId}/add/{symbol}")
    public boolean addSymbolToUser(@PathVariable String tradingUserId, 
                                   @PathVariable String symbol, 
                                   @RequestParam(defaultValue = "1") int watchlistId){
        String key = tradingUserId + ":" + watchlistId;
        log.info("Adding symbol {} to user {} watchlist {}", symbol, tradingUserId, watchlistId);
        return watchListService.addSymbol("hash:watchlist", key, symbol);
    }

    @PutMapping("/user/{tradingUserId}/remove/{symbol}")
    public boolean removeSymbolFromUser(@PathVariable String tradingUserId, 
                                        @PathVariable String symbol, 
                                        @RequestParam(defaultValue = "1") int watchlistId){
        String key = tradingUserId + ":" + watchlistId;
        log.info("Removing symbol {} from user {} watchlist {}", symbol, tradingUserId, watchlistId);
        return watchListService.removeSymbol("hash:watchlist", key, symbol);
    }

    @GetMapping("/user/{tradingUserId}/trades")
    public List<TradeJedisCache> getUserWLTrades(@PathVariable String tradingUserId, 
                                                 @RequestParam String date, 
                                                 @RequestParam(defaultValue = "1") int watchlistId){
        String key = tradingUserId + ":" + watchlistId;
        log.info("Getting trades for user: {} watchlist: {} date: {}", tradingUserId, watchlistId, date);
        return tradeJedisService.fetchAll("hash:watchlist", key, date);
    }

    @DeleteMapping("/user/{tradingUserId}/watchlist/{watchlistId}")
    public boolean deleteUserWatchlist(@PathVariable String tradingUserId, @PathVariable int watchlistId){
        String key = tradingUserId + ":" + watchlistId;
        log.info("Deleting watchlist {} for user {}", watchlistId, tradingUserId);
        return watchListService.deleteWatchlist("hash:watchlist", key);
    }

    @PostMapping("/user/{tradingUserId}/watchlist/{watchlistId}")
    public boolean createUserWatchlist(@PathVariable String tradingUserId, @PathVariable int watchlistId){
        String key = tradingUserId + ":" + watchlistId;
        log.info("Creating watchlist {} for user {}", watchlistId, tradingUserId);
        return watchListService.createWatchlist("hash:watchlist", key);
    }
    */

    // Authenticated endpoints that automatically get trading user ID from auth token
    @GetMapping("/my/symbols")
    public String[] getMySymbols(HttpServletRequest request, @RequestParam(defaultValue = "1") int watchlistId){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return new String[0];
        }
        
        String key = tradingUserId + ":" + watchlistId;
        log.info("Getting symbols for authenticated user: {} watchlist: {}", tradingUserId, watchlistId);
        return watchListService.getSymbols("hash:watchlist", key);
    }

    @GetMapping("/my/watchlists")
    public Map<String, String[]> getMyWatchlists(HttpServletRequest request){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return new HashMap<>();
        }
        
        log.info("Getting all watchlists for authenticated user: {}", tradingUserId);
        return watchListService.getUserWatchlists(tradingUserId.toString());
    }

    @GetMapping("/my/trades")
    public List<TradeJedisCache> getMyWLTrades(HttpServletRequest request, 
                                               @RequestParam String date, 
                                               @RequestParam(defaultValue = "1") int watchlistId){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return List.of();
        }
        
        String key = tradingUserId + ":" + watchlistId;
        log.info("Getting trades for authenticated user: {} watchlist: {} date: {}", tradingUserId, watchlistId, date);
        // Use the new method that returns only current prices (no previous prices)
        // This makes watchlist consistent with trades/holdings for frontend price change effects
        return tradeJedisService.fetchWatchlistTrades("hash:watchlist", key, date);
    }

    @PostMapping("/my/watchlist/{watchlistId}")
    public boolean createMyWatchlist(HttpServletRequest request, @PathVariable int watchlistId){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return false;
        }
        
        // Check if watchlist ID is within valid range (1-5)
        if (watchlistId < 1 || watchlistId > 5) {
            log.warn("Invalid watchlist ID: {}. Must be between 1 and 5", watchlistId);
            return false;
        }
        
        // Check if user already has 5 watchlists
        Map<String, String[]> existingWatchlists = watchListService.getUserWatchlists(tradingUserId.toString());
        if (existingWatchlists.size() >= 5) {
            log.warn("User {} already has maximum number of watchlists (5)", tradingUserId);
            return false;
        }
        
        String key = tradingUserId + ":" + watchlistId;
        log.info("Creating watchlist {} for user {}", watchlistId, tradingUserId);
        return watchListService.createWatchlist("hash:watchlist", key);
    }

    @DeleteMapping("/my/watchlist/{watchlistId}")
    public boolean deleteMyWatchlist(HttpServletRequest request, @PathVariable int watchlistId){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return false;
        }
        
        // Don't allow deleting watchlist 1 (default watchlist)
        if (watchlistId == 1) {
            log.warn("Cannot delete default watchlist (ID: 1)");
            return false;
        }
        
        String key = tradingUserId + ":" + watchlistId;
        log.info("Deleting watchlist {} for authenticated user {}", watchlistId, tradingUserId);
        boolean deleted = watchListService.deleteWatchlist("hash:watchlist", key);
        
        // Renumber watchlists after deletion
        if (deleted) {
            watchListService.renumberWatchlists(tradingUserId.toString());
        }
        
        return deleted;
    }

    @PostMapping("/my/watchlists/renumber")
    public boolean renumberMyWatchlists(HttpServletRequest request){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return false;
        }
        
        log.info("Renumbering watchlists for authenticated user {}", tradingUserId);
        return watchListService.renumberWatchlists(tradingUserId.toString());
    }

    @PutMapping("/my/add/{symbol}")
    public boolean addSymbolToMyWatchlist(HttpServletRequest request, 
                                          @PathVariable String symbol, 
                                          @RequestParam(defaultValue = "1") int watchlistId){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return false;
        }
        
        String key = tradingUserId + ":" + watchlistId;
        
        // Check if watchlist already has 10 symbols
        String[] existingSymbols = watchListService.getSymbols("hash:watchlist", key);
        if (existingSymbols.length >= 10) {
            log.warn("Watchlist {} for user {} already has maximum number of symbols (10)", watchlistId, tradingUserId);
            return false;
        }
        
        // Check if symbol already exists in watchlist
        for (String existingSymbol : existingSymbols) {
            if (existingSymbol.equalsIgnoreCase(symbol)) {
                log.warn("Symbol {} already exists in watchlist {} for user {}", symbol, watchlistId, tradingUserId);
                return false;
            }
        }
        
        log.info("Adding symbol {} to authenticated user {} watchlist {}", symbol, tradingUserId, watchlistId);
        return watchListService.addSymbol("hash:watchlist", key, symbol);
    }

    @PutMapping("/my/remove/{symbol}")
    public boolean removeSymbolFromMyWatchlist(HttpServletRequest request, 
                                               @PathVariable String symbol, 
                                               @RequestParam(defaultValue = "1") int watchlistId){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return false;
        }
        
        String key = tradingUserId + ":" + watchlistId;
        log.info("Removing symbol {} from authenticated user {} watchlist {}", symbol, tradingUserId, watchlistId);
        return watchListService.removeSymbol("hash:watchlist", key, symbol);
    }

    private String extractAuthToken(HttpServletRequest request) {
        // Try to get token from Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Try to get token from cookies
        String cookies = request.getHeader("Cookie");
        if (cookies != null) {
            String[] cookiePairs = cookies.split(";");
            for (String pair : cookiePairs) {
                String[] keyValue = pair.trim().split("=");
                if (keyValue.length == 2 && "auth_token".equals(keyValue[0])) {
                    return keyValue[1];
                }
            }
        }
        
        return null;
    }
}
