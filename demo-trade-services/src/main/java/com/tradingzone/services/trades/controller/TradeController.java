package com.tradingzone.services.trades.controller;

import com.google.gson.JsonObject;
import com.tradingzone.services.auth.UserAuthService;
import com.tradingzone.services.redis.repositories.TradeCache;
import com.tradingzone.services.redis.repositories.TradeJedisCache;
import com.tradingzone.services.redis.service.TradeJedisService;
import com.tradingzone.services.redis.service.TradeRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
public class TradeController {

    @Autowired
    private TradeJedisService tradeJedisService;

    @Autowired
    private UserAuthService userAuthService;

    // Commented out - Not used by UI, unprotected APIs
    /*
    @GetMapping("/tradingzone/trades")
    public List<TradeJedisCache> getTrades(){
        System.out.println("In TradeController.getTrades()");
        return tradeJedisService.fetchAll();
    }

    @GetMapping("/tradingzone/trades/{symbol}/{date}")
    public TradeJedisCache getTrade(@PathVariable String symbol, @PathVariable String date){
        return tradeJedisService.fetchSymbol(symbol, date);
    }

    @PutMapping("/tradingzone/trades/{symbol}/{date}")
    public TradeJedisCache setLTPSymbol(@PathVariable String symbol, @PathVariable String date) throws Exception {
        return tradeJedisService.setLTPSymbol(symbol, date);
    }

    @GetMapping("/tradingzone/tradeDatesByDate")
    public List<String> fetchAllTradeDatesByDate(@RequestParam String date){
        System.out.println("In TradeController.fetchAllTradeDatesByDate(date) "+ date);
        return tradeJedisService.fetchAllTradeDatesByDate(date);
    }
    */

    // Public APIs - Used by UI
    @GetMapping("/tradingzone/trades/symbols/")
    public List<String> getSymbols(){
        //log.info("In WatchListController.getSymbols() "+ " cache > {} key > {}  ",cache,key);
        return tradeJedisService.getSymbols();
    }

    @GetMapping("/tradingzone/trades/latestdate/")
    public String getLatestTradeDate(){
        return tradeJedisService.getLatestTradeDate();
    }

    @GetMapping("/tradingzone/tradesByDate")
    public List<TradeJedisCache> getTradesByDate( @RequestParam String date, @RequestParam boolean live){
        log.info("In TradeController.getTradesByDate() date {} live {}", date, live);
        return tradeJedisService.fetchAllByDate(date, live);
    }

    @GetMapping("/tradingzone/tradeshistory/{symbol}/{timeRange}")
    public List<TradeJedisCache> getTradeHistory(@PathVariable String symbol , @PathVariable String timeRange){
        return tradeJedisService.fetchSymbolHistory(symbol, timeRange);
    }

    // Protected endpoints for authenticated users
    @GetMapping("/tradingzone/my/trades")
    public List<TradeJedisCache> getMyTrades(HttpServletRequest request){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return List.of();
        }
        
        log.info("Getting trades for authenticated user: {}", tradingUserId);
        return tradeJedisService.fetchAll();
    }

    @GetMapping("/tradingzone/my/tradesByDate")
    public List<TradeJedisCache> getMyTradesByDate(HttpServletRequest request, 
                                                   @RequestParam String date, 
                                                   @RequestParam boolean live){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return List.of();
        }
        
        log.info("Getting trades by date for authenticated user: {} date {} live {}", tradingUserId, date, live);
        return tradeJedisService.fetchAllByDate(date, live);
    }

    @GetMapping("/tradingzone/my/trades/{symbol}/{date}")
    public TradeJedisCache getMyTrade(HttpServletRequest request, 
                                      @PathVariable String symbol, 
                                      @PathVariable String date){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return null;
        }
        
        log.info("Getting trade for authenticated user: {} symbol {} date {}", tradingUserId, symbol, date);
        return tradeJedisService.fetchSymbol(symbol, date);
    }

    @GetMapping("/tradingzone/my/tradeshistory/{symbol}/{timeRange}")
    public List<TradeJedisCache> getMyTradeHistory(HttpServletRequest request, 
                                                   @PathVariable String symbol, 
                                                   @PathVariable String timeRange){
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return List.of();
        }
        
        log.info("Getting trade history for authenticated user: {} symbol {} timeRange {}", tradingUserId, symbol, timeRange);
        return tradeJedisService.fetchSymbolHistory(symbol, timeRange);
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
