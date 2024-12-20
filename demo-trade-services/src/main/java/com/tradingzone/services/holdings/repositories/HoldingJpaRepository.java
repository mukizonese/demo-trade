package com.tradingzone.services.holdings.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HoldingJpaRepository extends JpaRepository<HoldingEntity, Integer> {

    List<HoldingEntity> findAll();
    List<HoldingEntity> findByUsrId(Integer userId);
}
