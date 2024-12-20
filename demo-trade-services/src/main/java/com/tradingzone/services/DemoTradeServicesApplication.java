package com.tradingzone.services;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories( basePackages = {"com.tradingzone.services.trades.repositories",
		"com.tradingzone.services.holdings.repositories"})
@EnableBatchProcessing
public class DemoTradeServicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoTradeServicesApplication.class, args);
	}

}
