package com.tradingzone.services.dummy.controller;

import com.tradingzone.services.dummy.service.TradeDummyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tradingzone/dummy")
public class TradeDummyController {

    @Autowired
    private TradeDummyService tradeDummyService;



    @GetMapping("/initiateDummyFull")
    public void initiateDummyFull( @RequestParam String date) throws Exception {
        tradeDummyService.initiateDummyFull(date);
    }

    @GetMapping("/clearDummyFull")
    public void clearDummyFull(@RequestParam String date) throws Exception {
        tradeDummyService.clearDummyFull(date);
    }


    @GetMapping("/initiateDummyWatchList")
    public void initiateDummyWatchList(@RequestParam String cache, @RequestParam String key, @RequestParam String date) throws Exception {
        tradeDummyService.initiateDummyWatchList( cache, key, date);
    }

    @GetMapping("/clearDummyWatchList")
    public void clearDummyWatchList(@RequestParam String cache, @RequestParam String key, @RequestParam String date) throws Exception {
        tradeDummyService.clearDummyWatchList( cache, key, date);
    }

    @GetMapping("/initiateDummyTrade")
    public String initiateDummyData(@RequestParam String symbol, @RequestParam String date) throws Exception {
        return tradeDummyService.initiateDummyTrade(symbol, date);
    }




}
