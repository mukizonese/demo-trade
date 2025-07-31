package com.tradingzone.services.holdings.controller;

import com.tradingzone.services.auth.UserAuthService;
import com.tradingzone.services.holdings.repositories.HoldingEntity;
import com.tradingzone.services.holdings.service.HoldingsService;
import com.tradingzone.services.holdings.data.Holdings;
import com.tradingzone.services.holdings.data.TradeDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/tradingzone/holdings")
public class HoldingsController {

    @Autowired
    private HoldingsService holdingsService;

    @Autowired
    private UserAuthService userAuthService;

    // Commented out - Not used by UI, unprotected API
    /*
    @GetMapping("all")
    public List<HoldingEntity> getHoldings() throws Exception {
        return holdingsService.fetchAllHoldings();
    }
    */

    // Commented out - Not used by UI, unprotected API
    /*
    @GetMapping("/{userId}")
    public Holdings getHoldings(@PathVariable Integer userId) throws Exception {
        log.info("Received request for holdings for userId: {}", userId);

        Holdings holdings = holdingsService.fetchHoldings(userId);
        
        log.info("Returning holdings for userId {}: {} transactions, total value: {}", 
                userId, holdings.getTransactionlist().size(), holdings.getTotCurrValue());
        
        return holdings;
    }
    */

    // Commented out - Not used by UI, unprotected APIs
    /*
    @PutMapping("/buy/{symbol}")
    public boolean buyTradeToHolding(@PathVariable String symbol, @RequestParam Integer qty, @RequestParam Integer userId) {
        return holdingsService.buyTradeToHolding(symbol, qty, userId);
    }

    @PutMapping("/sell/{symbol}")
    public boolean sellTradeToHolding(@PathVariable String symbol, @RequestParam Integer qty, @RequestParam Integer userId) {
        return holdingsService.sellTradeToHolding(symbol, qty, userId);
    }
    */

    // Protected endpoints for authenticated users
    @GetMapping("/my")
    public Holdings getMyHoldings(HttpServletRequest request) throws Exception {
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return new Holdings(); // Return empty holdings
        }
        
        log.info("Received request for holdings for authenticated user: {}", tradingUserId);

        Holdings holdings = holdingsService.fetchHoldings(tradingUserId);
        
        log.info("Returning holdings for authenticated user {}: {} transactions, total value: {}", 
                tradingUserId, holdings.getTransactionlist().size(), holdings.getTotCurrValue());
        
        return holdings;
    }

    @PutMapping("/my/buy/{symbol}")
    public boolean buyTradeToMyHolding(HttpServletRequest request, 
                                       @PathVariable String symbol, 
                                       @RequestParam Integer qty) {
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return false;
        }

        // Check if user has trader or aitrader role (not guest)
        String userRole = userAuthService.getUserRole(authToken);
        if ("guest".equals(userRole)) {
            log.warn("User {} attempted to buy {} but has insufficient role. Required: trader/aitrader, Current: {}", 
                    tradingUserId, symbol, userRole);
            return false;
        }
        
        log.info("User {} ({} role) buying {} quantity {}", tradingUserId, userRole, symbol, qty);
        return holdingsService.buyTradeToHolding(symbol, qty, tradingUserId);
    }

    @PutMapping("/my/sell/{symbol}")
    public boolean sellTradeFromMyHolding(HttpServletRequest request, 
                                          @PathVariable String symbol, 
                                          @RequestParam Integer qty) {
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return false;
        }

        // Check if user has trader or aitrader role (not guest)
        String userRole = userAuthService.getUserRole(authToken);
        if ("guest".equals(userRole)) {
            log.warn("User {} attempted to sell {} but has insufficient role. Required: trader/aitrader, Current: {}", 
                    tradingUserId, symbol, userRole);
            return false;
        }
        
        log.info("User {} ({} role) selling {} quantity {}", tradingUserId, userRole, symbol, qty);
        return holdingsService.sellTradeToHolding(symbol, qty, tradingUserId);
    }

    @GetMapping("/my/trades/{symbol}")
    public List<TradeDetail> getMyTradeDetails(HttpServletRequest request, 
                                               @PathVariable String symbol) throws Exception {
        String authToken = extractAuthToken(request);
        Integer tradingUserId = userAuthService.getTradingUserId(authToken);
        
        if (tradingUserId == null) {
            log.warn("Could not determine trading user ID from auth token");
            return new ArrayList<>(); // Return empty list
        }
        
        log.info("Received request for trade details for symbol {} for authenticated user: {}", symbol, tradingUserId);

        List<TradeDetail> tradeDetails = holdingsService.fetchTradeDetailsForSymbol(tradingUserId, symbol);
        
        log.info("Returning {} trade details for symbol {} for authenticated user {}", 
                tradeDetails.size(), symbol, tradingUserId);
        
        return tradeDetails;
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
