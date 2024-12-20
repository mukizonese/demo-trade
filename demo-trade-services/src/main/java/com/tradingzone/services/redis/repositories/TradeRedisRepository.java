package com.tradingzone.services.redis.repositories;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TradeRedisRepository extends CrudRepository<TradeCache, String> {

    List<TradeCache> findByTckrSymb(String symbol);
}
