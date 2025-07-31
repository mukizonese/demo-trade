package com.tradingzone.services.holdings.data;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
public class TradeDetail {
    private String symbol;
    private String action; // "B" for Buy, "S" for Sell
    private Integer qty;
    private BigDecimal price;
    private Timestamp tradeDate;
    private Integer ageInDays;
    private BigDecimal pnl; // Profit & Loss for this trade
    private BigDecimal currentPrice; // Current LTP for P&L calculation
} 