package com.tradingzone.services.trades.controller;

import com.google.gson.JsonObject;
import com.tradingzone.services.redis.repositories.TradeCache;
import com.tradingzone.services.redis.repositories.TradeJedisCache;
import com.tradingzone.services.redis.service.TradeJedisService;
import com.tradingzone.services.redis.service.TradeRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class TradeController {

    @Autowired
    private TradeJedisService tradeJedisService;

    @GetMapping("/tradingzone/trades/symbols/")
    public List<String> getSymbols(){
        //log.info("In WatchListController.getSymbols() "+ " cache > {} key > {}  ",cache,key);
        return tradeJedisService.getSymbols();
    }

    @GetMapping("/tradingzone/trades/latestdate/")
    public String getLatestTradeDate(){
        return tradeJedisService.getLatestTradeDate();
    }

    @GetMapping("/tradingzone/trades")
    public List<TradeJedisCache> getTrades(){
        System.out.println("In TradeController.getTrades()");
        return tradeJedisService.fetchAll();
    }

    @GetMapping("/tradingzone/tradesByDate")
    public List<TradeJedisCache> getTradesByDate( @RequestParam String date, @RequestParam boolean live){
        log.info("In TradeController.getTradesByDate() date {} live {}", date, live);
        return tradeJedisService.fetchAllByDate(date, live);
    }

    @GetMapping("/tradingzone/trades/{symbol}/{date}")
    public TradeJedisCache getTrade(@PathVariable String symbol, @PathVariable String date){
        return tradeJedisService.fetchSymbol(symbol, date);
    }

    @GetMapping("/tradingzone/tradeshistory/{symbol}/{timeRange}")
    public List<TradeJedisCache> getTradeHistory(@PathVariable String symbol , @PathVariable String timeRange){

        return tradeJedisService.fetchSymbolHistory(symbol, timeRange);
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



}
