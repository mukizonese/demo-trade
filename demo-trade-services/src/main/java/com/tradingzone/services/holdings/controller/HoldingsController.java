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

    @CrossOrigin(origins = "*")
    @GetMapping("all")
    public List<HoldingEntity> getHoldings() throws Exception {
        return holdingsService.fetchAllHoldings();
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/{userId}")
    public Holdings getHoldings(@PathVariable Integer userId) throws Exception {

        Holdings holdings = holdingsService.fetchHoldings(userId);
        ///log.info("In HoldingsController.getHoldings() userId > {} Curr Val {} PNL {} Day PNL {}",
           //     userId, holdings.getTotCurrValue(), holdings.getTotNetChng(), holdings.getTotDayChng());
        return  holdings;
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/buy/{symbol}")
    public boolean buyTradeToHolding(@PathVariable String symbol, @RequestParam Integer qty) {
        return holdingsService.buyTradeToHolding(symbol,qty);
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/sell/{symbol}")
    public boolean sellTradeToHolding(@PathVariable String symbol, @RequestParam Integer qty) {
        return holdingsService.sellTradeToHolding(symbol,qty);
    }


}
