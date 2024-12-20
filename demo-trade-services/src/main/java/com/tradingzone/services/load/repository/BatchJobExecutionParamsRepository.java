package com.tradingzone.services.load.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BatchJobExecutionParamsRepository extends JpaRepository<BatchJobExecutionParams, String> {
}
