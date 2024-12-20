package com.tradingzone.services.redis.service;

import com.tradingzone.services.redis.repositories.TradeCache;
import com.tradingzone.services.redis.repositories.TradeRedisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeRedisService{

    @Autowired
    private TradeRedisRepository tradeRedisRepository;


    public List<TradeCache>  fetchAll(){
        return (List<TradeCache>) tradeRedisRepository.findAll();
    }

    public TradeCache fetchTicker(String symbol){
        List<TradeCache> tickers  = tradeRedisRepository.findByTckrSymb(symbol);
        return tickers.getFirst();

    }
    public void displayAll(){
        List<TradeCache> tickers  = (List<TradeCache>) tradeRedisRepository.findAll();
        for(TradeCache ticker : tickers)
            System.out.println(ticker.toString());
    }
}
