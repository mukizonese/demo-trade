package com.tradingzone.services.holdings.controller;

import com.tradingzone.services.holdings.repositories.HoldingEntity;
import com.tradingzone.services.holdings.service.HoldingsService;
import com.tradingzone.services.holdings.data.Holdings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tradingzone/holdings")
public class HoldingsController {

    @Autowired
    private HoldingsService holdingsService;

    @GetMapping("all")
    public List<HoldingEntity> getHoldings() throws Exception {
        return holdingsService.fetchAllHoldings();
    }

    @GetMapping("/{userId}")
    public Holdings getHoldings(@PathVariable Integer userId) throws Exception {
        log.info("Received request for holdings for userId: {}", userId);

        Holdings holdings = holdingsService.fetchHoldings(userId);
        
        log.info("Returning holdings for userId {}: {} transactions, total value: {}", 
                userId, holdings.getTransactionlist().size(), holdings.getTotCurrValue());
        
        return holdings;
    }

    @PutMapping("/buy/{symbol}")
    public boolean buyTradeToHolding(@PathVariable String symbol, @RequestParam Integer qty, @RequestParam Integer userId) {
        return holdingsService.buyTradeToHolding(symbol, qty, userId);
    }

    @PutMapping("/sell/{symbol}")
    public boolean sellTradeToHolding(@PathVariable String symbol, @RequestParam Integer qty, @RequestParam Integer userId) {
        return holdingsService.sellTradeToHolding(symbol, qty, userId);
    }


}
