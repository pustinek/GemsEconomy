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

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SQLiteDataStore extends Database {

    private HikariDataSource dataSource;
    private Map<UUID, CachedTopList> cachedTopList;
    private File file;

    public SQLiteDataStore(File file) {
        super("SQLite", true);
        this.file = file;
    }

    @Override
    HikariDataSource getHikariSource() {
        if(dataSource != null)
            return dataSource;

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            return null;
        }


        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + file);
        config.setConnectionTestQuery("SELECT 1");

        this.dataSource = new HikariDataSource(config);
        return this.dataSource;
    }

    @Override
    String getQueryCreateTableAccounts() {
        return null;
    }

    @Override
    String getQueryCreateTableCurrency() {
        return null;
    }

    @Override
    String getQueryCreateTableBalance() {
        return null;
    }
}
