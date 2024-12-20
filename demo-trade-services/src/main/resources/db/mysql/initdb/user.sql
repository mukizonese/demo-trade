CREATE DATABASE IF NOT EXISTS tradedb;

ALTER DATABASE tradedb
  DEFAULT CHARACTER SET utf8
  DEFAULT COLLATE utf8_general_ci;

GRANT ALL PRIVILEGES ON tradedb.* TO 'tradedb_usr'@'%' IDENTIFIED BY 'tradedb_usr';


