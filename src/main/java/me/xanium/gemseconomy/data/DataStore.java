/*
 * Copyright Xanium Development (c) 2013-2018. All Rights Reserved.
 * Any code contained within this document, and any associated APIs with similar branding
 * are the sole property of Xanium Development. Distribution, reproduction, taking snippets or claiming
 * any contents as your own will break the terms of the license, and void any agreements with you, the third party.
 * Thank you.
 */

package me.xanium.gemseconomy.data;

import me.xanium.gemseconomy.GemsEconomy;
import me.xanium.gemseconomy.account.Account;
import me.xanium.gemseconomy.currency.Currency;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public abstract class DataStore {

    private static final ArrayList<DataStore> methods = new ArrayList<>();
    public final GemsEconomy plugin = GemsEconomy.getInstance();
    private final String name;
    private final boolean topSupported;

    public DataStore(String name, boolean topSupported) {
        this.name = name;
        this.topSupported = topSupported;
    }

    public static DataStore getMethod(String name) {
        for (DataStore store : getMethods()) {
            if (store.getName().equalsIgnoreCase(name)) {
                return store;
            }
        }
        return null;
    }

    public static ArrayList<DataStore> getMethods() {
        return methods;
    }

    public abstract void initialize();

    public abstract void close();

    public abstract void loadCurrencies();

    public abstract void updateCurrencyLocally(Currency currency);

    public abstract void saveCurrency(Currency currency);

    public abstract void deleteCurrency(Currency currency);

    public abstract Map<String, Double> getTopList(Currency currency, int offset, int amount);

    public abstract Account loadAccount(String string);

    public abstract Account loadAccount(UUID uuid);

    public abstract void saveAccount(Account account);

    public abstract void deleteAccount(Account account);

    public abstract void createAccount(Account account);

    public abstract ArrayList<Account> getOfflineAccounts();

    public String getName() {
        return this.name;
    }

    public boolean isTopSupported() {
        return this.topSupported;
    }
}

