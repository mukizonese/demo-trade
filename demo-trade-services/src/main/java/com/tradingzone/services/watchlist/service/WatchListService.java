package com.tradingzone.services.watchlist.service;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.UnifiedJedis;

@Slf4j
@Service
public class WatchListService {

    @Autowired
    private UnifiedJedis unifiedJedis;

    @Autowired
    private Gson gson;

    public String[] getSymbols(String cache, String key) {

        String[] symMap = new String[0];
        
        String symbols = unifiedJedis.hget(cache, key);

        if (symbols != null) {
            symMap = symbols.split(",");
        }else{
            log.error("cache {} key {} not found ",cache,key);
        }

        return symMap;
    }

    public boolean addSymbol(String cache, String key, String symbol ) {

        String symbols = unifiedJedis.hget(cache, key);

        if (symbols != null ) {
            if ( !symbols.contains(symbol)) {
                symbols = symbols + "," + symbol;
                unifiedJedis.hset(cache, key, symbols);
                log.info("cache {} key {} symbol {} added succesfully.",cache,key, symbol);
            }else{
                log.error("cache {} key {} symbol already exists symbol {} ",cache,key, symbol);
                return false;
            }
        }else{
            log.error("cache {} key {} not found ",cache,key);
            return false;
        }
        return true;
    }

    public boolean removeSymbol(String cache, String key, String symbol ) {

        String symbols = unifiedJedis.hget(cache, key);

        if (symbols != null ) {
            if ( symbols.contains(symbol)) {
                symbols = symbols.replace(symbol,"");
                unifiedJedis.hset(cache, key, symbols);
                log.info("cache {} key {} symbol {} removed succesfully.",cache,key, symbol);
            }else{
                log.error("cache {} key {} symbol doesnt exists symbol {} ",cache,key, symbol);
                return false;
            }
        }else{
            log.error("cache {} key {} not found ",cache,key);
            return false;
        }
        return true;
    }
}
