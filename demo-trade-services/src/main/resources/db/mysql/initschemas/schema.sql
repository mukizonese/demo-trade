CREATE TABLE IF NOT EXISTS TRADES (
    TradDt TIMESTAMP NOT NULL,
    BizDt TIMESTAMP NOT NULL,
    Sgmt VARCHAR(2) NOT NULL,
    Src VARCHAR(3) NOT NULL,
    FinInstrmTp VARCHAR(3) NOT NULL,
    FinInstrmId INT(6) NOT NULL,
    ISIN VARCHAR(15) NOT NULL,
    TckrSymb VARCHAR(10) NOT NULL,
    SctySrs VARCHAR(2) NOT NULL,
    XpryDt TIMESTAMP,
    FininstrmActlXpryDt TIMESTAMP,
    StrkPric DOUBLE(6,2),
    OptnTp VARCHAR(3),
    FinInstrmNm VARCHAR(30) NOT NULL,
    OpnPric DOUBLE(15,2) UNSIGNED NOT NULL,
    HghPric DOUBLE(15,2) UNSIGNED NOT NULL,
    LwPric DOUBLE(15,2) UNSIGNED NOT NULL,
    ClsPric DOUBLE(15,2) UNSIGNED NOT NULL,
    LastPric DOUBLE(15,2) UNSIGNED NOT NULL,
    PrvsClsgPric DOUBLE(15,2) UNSIGNED NOT NULL,
    UndrlygPric DOUBLE(15,2),
    SttlmPric DOUBLE(15,2) UNSIGNED NOT NULL,
    OpnIntrst VARCHAR(10),
    ChngInOpnIntrst VARCHAR(10),
    TtlTradgVol BIGINT(22) UNSIGNED NOT NULL,
    TtlTrfVal DOUBLE(18,2) UNSIGNED NOT NULL,
    TtlNbOfTxsExctd INT(20) UNSIGNED NOT NULL,
    SsnId VARCHAR(3) NOT NULL,
    NewBrdLotQty INT(20) UNSIGNED NOT NULL,
    Rmks VARCHAR(5),
    Rsvd1 VARCHAR(5),
    Rsvd2 VARCHAR(5),
    Rsvd3 VARCHAR(5),
    Rsvd4 VARCHAR(5),
   PRIMARY KEY (TradDt, FinInstrmId),
  INDEX(TradDt, FinInstrmId,TckrSymb)
) engine=InnoDB;

--CREATE TABLE IF NOT EXISTS HOLDINGS (
--   UsrId INT(20) UNSIGNED NOT NULL,
--   TckrSymb VARCHAR(10) NOT NULL,
--   Pric DOUBLE(15,2) UNSIGNED NOT NULL,
--   Qty INT(20) UNSIGNED NOT NULL,
--   TradDt TIMESTAMP NOT NULL,
--   Action VARCHAR(4) NOT NULL,
--   PRIMARY KEY (UsrId,TckrSymb,TradDt),
--  INDEX(UsrId, TckrSymb)
--) engine=InnoDB;

CREATE TABLE IF NOT EXISTS HOLDINGS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,           -- Surrogate key
    UsrId INT(20) UNSIGNED NOT NULL,
    TckrSymb VARCHAR(10) NOT NULL,
    Pric DOUBLE(15,2) UNSIGNED NOT NULL,
    Qty INT(20) UNSIGNED NOT NULL,
    TradDt TIMESTAMP NOT NULL,
    Action VARCHAR(4) NOT NULL,
    UNIQUE KEY uq_usr_symb_date (UsrId, TckrSymb, TradDt),
    INDEX idx_usr_symb (UsrId, TckrSymb)
) ENGINE=InnoDB;

INSERT INTO HOLDINGS (UsrId, TckrSymb, Pric, Qty, TradDt, Action)
VALUES (3, 'HDFCBANK', 1980.10, 10, '2025-06-09 00:00:00', 'B')
ON DUPLICATE KEY UPDATE
  Pric = VALUES(Pric),
  Qty = VALUES(Qty),
  Action = VALUES(Action);

CREATE
    OR REPLACE
    VIEW LATEST_TRADES
    AS
	(SELECT * FROM (
		SELECT
			*,
			row_number() OVER(PARTITION BY TckrSymb ORDER BY TradDt DESC) AS rn
		FROM
        TRADES
	) t
	WHERE t.rn = 1);

CREATE OR REPLACE VIEW LATEST_HOLDINGS AS (
    SELECT H.UsrId, H.TckrSymb, H.Qty, H.TradDt, H.Pric,
           H.Qty * H.Pric as AvgPric,
           LT.ClsPric as CurPric,
           H.Qty * LT.ClsPric as CurValue,
           (H.Qty * LT.ClsPric) - (H.Qty * H.Pric) as PNL
    FROM HOLDINGS H
    INNER JOIN (
        SELECT TradDt as LatTradDt, TckrSymb, ClsPric FROM LATEST_TRADES
    ) LT
    ON H.TckrSymb = LT.TckrSymb
    WHERE H.Action = 'B'
);
