package com.tradingzone.services.trades.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeJpaRepository extends JpaRepository<TradeEntity, String> {

}
