package com.tradingzone.services.watchlist.controller;

import com.tradingzone.services.redis.repositories.TradeJedisCache;
import com.tradingzone.services.redis.service.TradeJedisService;
import com.tradingzone.services.watchlist.service.WatchListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tradingzone/watchlist")
public class WatchListController {

    @Autowired
    private TradeJedisService tradeJedisService;

    @Autowired
    private WatchListService watchListService;

    @CrossOrigin(origins = "*")
    @GetMapping("/symbols")
    public String[] getSymbols(@RequestParam String cache, @RequestParam String key){
        //log.info("In WatchListController.getSymbols() "+ " cache > {} key > {}  ",cache,key);
        return watchListService.getSymbols(cache, key);
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/add/{symbol}")
    public boolean addSymbol(@RequestParam String cache, @RequestParam String key, @PathVariable String symbol){
        return watchListService.addSymbol(cache, key, symbol );
    }

    @CrossOrigin(origins = "*")
    @PutMapping("/remove/{symbol}")
    public boolean removeSymbols(@RequestParam String cache, @RequestParam String key, @PathVariable String symbol){
        return watchListService.removeSymbol(cache, key, symbol );
    }


    @CrossOrigin(origins = "*")
    @GetMapping("/trades")
    public List<TradeJedisCache> getWLTrades(@RequestParam String cache, @RequestParam String key, @RequestParam String date){
        //log.info("In WatchListController.getWLTrades() "+ " cache > {} key > {} date > {} ", cache, key, date);
        return tradeJedisService.fetchAll(cache, key, date);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/latestprice/{symbol}")
    public TradeJedisCache fetchLatestPrice(@PathVariable String symbol){
        return tradeJedisService.fetchLatestPrice(symbol);
    }
}
