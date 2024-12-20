package com.tradingzone.services.dummy.controller;

import com.tradingzone.services.dummy.service.TradeDummyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tradingzone/dummy")
public class TradeDummyController {

    @Autowired
    private TradeDummyService tradeDummyService;



    @CrossOrigin(origins = "*")
    @GetMapping("/initiateDummyFull")
    public void initiateDummyFull( @RequestParam String date) throws Exception {
        tradeDummyService.initiateDummyFull(date);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/clearDummyFull")
    public void clearDummyFull(@RequestParam String date) throws Exception {
        tradeDummyService.clearDummyFull(date);
    }


    @CrossOrigin(origins = "*")
    @GetMapping("/initiateDummyWatchList")
    public void initiateDummyWatchList(@RequestParam String cache, @RequestParam String key, @RequestParam String date) throws Exception {
        tradeDummyService.initiateDummyWatchList( cache, key, date);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/clearDummyWatchList")
    public void clearDummyWatchList(@RequestParam String cache, @RequestParam String key, @RequestParam String date) throws Exception {
        tradeDummyService.clearDummyWatchList( cache, key, date);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/initiateDummyTrade")
    public String initiateDummyData(@RequestParam String symbol, @RequestParam String date) throws Exception {
        return tradeDummyService.initiateDummyTrade(symbol, date);
    }




}
