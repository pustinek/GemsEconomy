/*
 * Copyright Xanium Development (c) 2013-2018. All Rights Reserved.
 * Any code contained within this document, and any associated APIs with similar branding
 * are the sole property of Xanium Development. Distribution, reproduction, taking snippets or claiming
 * any contents as your own will break the terms of the license, and void any agreements with you, the third party.
 * Thank you.
 */

package me.xanium.gemseconomy.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MySQLStorage extends Database {

    private final HikariDataSource hikariDataSource;

    public MySQLStorage(String host, int port, String database, String username, String password) {
        super("MySQL", true);
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?allowPublicKeyRetrieval=true&useSSL=false");
        hikariConfig.setPassword(password);
        hikariConfig.setUsername(username);
        hikariConfig.setMaxLifetime(30000);
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("userServerPrepStmts", "true");

        hikariDataSource = new HikariDataSource(hikariConfig);
    }


    @Override
    HikariDataSource getHikariSource() {
        return hikariDataSource;
    }

    @Override
    String getQueryCreateTableAccounts() {
        return "CREATE TABLE IF NOT EXISTS " + tableAccounts + "(" +
                "nickname VARCHAR(255)," +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "payable INTEGER);";
    }

    @Override
    String getQueryCreateTableCurrency() {
        return "CREATE TABLE IF NOT EXISTS " + tableCurrencies + "(" +
                "uuid VARCHAR(36) PRIMARY KEY," +
                "name_singular VARCHAR(255),"+
                "name_plural VARCHAR(255),"+
                "default_balance DECIMAL," +
                "symbol VARCHAR(10),"+
                "decimals_supported INTEGER,"+
                "is_default INTEGER,"+
                "payable INTEGER,"+
                "color VARCHAR(255),"+
                "exchange_rate DECIMAL);";
    }

    @Override
    String getQueryCreateTableBalance() {
        /*
        return "CREATE TABLE IF NOT EXISTS " + tableBalances + "(" +
                "account_id VARCHAR(255)," +
                "currency_id VARCHAR(255)," +
                "balance DECIMAL);";

         */

        return "CREATE TABLE IF NOT EXISTS " + tableBalances + "(" +
                "account_id VARCHAR(36)," +
                "currency_id VARCHAR(36)," +
                "balance DECIMAL," +
                "FOREIGN KEY (`account_id`) REFERENCES " + tableAccounts + "(`uuid`)," +
                "FOREIGN KEY (`currency_id`) REFERENCES " + tableCurrencies + "(`uuid`)," +
                "CONSTRAINT fk_balances UNIQUE (account_id, currency_id));";

    }
}
