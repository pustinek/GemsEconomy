package me.xanium.gemseconomy.data;

import com.zaxxer.hikari.HikariDataSource;
import me.xanium.gemseconomy.GemsEconomy;
import me.xanium.gemseconomy.account.Account;
import me.xanium.gemseconomy.currency.Currency;
import me.xanium.gemseconomy.utils.UtilServer;
import org.bukkit.ChatColor;

import java.sql.*;
import java.util.*;

public abstract class Database extends DataStore {

    public Database(String name, boolean topSupported) {
        super(name, topSupported);
    }



    private Map<UUID, CachedTopList> cachedTopList;

    abstract HikariDataSource getHikariSource();

    abstract String getQueryCreateTableAccounts();

    abstract String getQueryCreateTableCurrency();

    abstract String getQueryCreateTableBalance();


    protected String tableAccounts = getTablePrefix() + "_accounts";
    protected String tableCurrencies = getTablePrefix() + "_currencies";
    protected String tableBalances = getTablePrefix() + "_balances";



    protected String getTablePrefix() {
        return GemsEconomy.getInstance().getConfig().getString("mysql.tableprefix");
    }


    private void setupTables() throws SQLException {
        try (PreparedStatement ps = getHikariSource().getConnection().prepareStatement(getQueryCreateTableCurrency())) {
            ps.execute();
        }
        try (PreparedStatement ps = getHikariSource().getConnection().prepareStatement(getQueryCreateTableAccounts())) {
            ps.execute();
        }
        try (PreparedStatement ps = getHikariSource().getConnection().prepareStatement(getQueryCreateTableBalance())) {
            ps.execute();
        }
    }

    @Override
    public void initialize() {
        this.cachedTopList = new HashMap<>();

        try (Connection connection = getHikariSource().getConnection()) {
            setupTables();

            PreparedStatement stmt;
            List<String> columns = new ArrayList<>();
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet tableResultSet = metaData.getTables(null, "public", null, new String[]{"TABLE"})) {
                while (tableResultSet.next()) {
                    String tableName = tableResultSet.getString("TABLE_NAME");
                    try (ResultSet columnResultSet = metaData.getColumns(null, "public", tableName, null)) {
                        while (columnResultSet.next()) {
                            String columnName = columnResultSet.getString("COLUMN_NAME");
                            columns.add(columnName);
                        }
                    }
                }
            }

            if (!columns.contains("exchange_rate")) {
                stmt = getHikariSource().getConnection().prepareStatement("ALTER TABLE " + this.getTablePrefix() + "_currencies ADD exchange_rate DECIMAL NULL DEFAULT NULL AFTER `color`;");
                stmt.execute();

                UtilServer.consoleLog("Altered Table " + this.getTablePrefix() + "_currencies to support the new variable.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        if (getHikariSource() != null) {
            getHikariSource().close();
        }
    }

    @Override
    public void loadCurrencies() {
        if (getHikariSource() == null) {
            return;
        }
        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "_currencies");
            ResultSet set = stmt.executeQuery();
            while (set.next()) {
                UUID uuid = UUID.fromString(set.getString("uuid"));
                String singular = set.getString("name_singular");
                String plural = set.getString("name_plural");
                double defaultBalance = set.getDouble("default_balance");
                String symbol = set.getString("symbol");
                boolean decimals = set.getInt("decimals_supported") == 1;
                boolean isDefault = set.getInt("is_default") == 1;
                boolean payable = set.getInt("payable") == 1;
                ChatColor color = ChatColor.valueOf(set.getString("color"));
                double exchangeRate = set.getDouble("exchange_rate");
                Currency currency = new Currency(uuid, singular, plural);
                currency.setDefaultBalance(defaultBalance);
                currency.setSymbol(symbol);
                currency.setDecimalSupported(decimals);
                currency.setDefaultCurrency(isDefault);
                currency.setPayable(payable);
                currency.setColor(color);
                currency.setExchangeRate(exchangeRate);

                plugin.getCurrencyManager().add(currency);
                UtilServer.consoleLog("Loaded currency: " + currency.getSingular());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateCurrencyLocally(Currency currency) {
        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "_currencies WHERE uuid = ? LIMIT 1;");
            stmt.setString(1, currency.getUuid().toString());
            ResultSet set = stmt.executeQuery();
            while (set.next()) {
                double defaultBalance = set.getDouble("default_balance");
                String symbol = set.getString("symbol");
                boolean decimals = set.getInt("decimals_supported") == 1;
                boolean isDefault = set.getInt("is_default") == 1;
                boolean payable = set.getInt("payable") == 1;
                ChatColor color = ChatColor.valueOf(set.getString("color"));
                double exchangeRate = set.getDouble("exchange_rate");

                currency.setDefaultBalance(defaultBalance);
                currency.setSymbol(symbol);
                currency.setDecimalSupported(decimals);
                currency.setDefaultCurrency(isDefault);
                currency.setPayable(payable);
                currency.setColor(color);
                currency.setExchangeRate(exchangeRate);
                UtilServer.consoleLog("Updated currency: " + currency.getPlural());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveCurrency(Currency currency) {
        final String SAVE_CURRENCY = "INSERT INTO `" + getTablePrefix() + "_currencies` (`uuid`, `name_singular`, `name_plural`, `default_balance`, `symbol`, `decimals_supported`, `is_default`, `payable`, `color`, `exchange_rate`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE `uuid` = VALUES(`uuid`), `name_singular` = VALUES(`name_singular`), `name_plural` = VALUES(`name_plural`), `default_balance` = VALUES(`default_balance`), `symbol` = VALUES(`symbol`), `decimals_supported` = VALUES(`decimals_supported`), `is_default` = VALUES(`is_default`), `payable` = VALUES(`payable`), `color` = VALUES(`color`), `exchange_rate` = VALUES(`exchange_rate`)";
        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(SAVE_CURRENCY);
            stmt.setString(1, currency.getUuid().toString());
            stmt.setString(2, currency.getSingular());
            stmt.setString(3, currency.getPlural());
            stmt.setDouble(4, currency.getDefaultBalance());
            stmt.setString(5, currency.getSymbol());
            stmt.setInt(6, currency.isDecimalSupported() ? 1 : 0);
            stmt.setInt(7, currency.isDefaultCurrency() ? 1 : 0);
            stmt.setInt(8, currency.isPayable() ? 1 : 0);
            stmt.setString(9, currency.getColor().name());
            stmt.setDouble(10, currency.getExchangeRate());
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        plugin.getUpdateForwarder().sendUpdateMessage("currency", currency.getUuid().toString());
    }

    @Override
    public void deleteCurrency(Currency currency) {
        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM "+ tableBalances +" WHERE currency_id = ?");
            stmt.setString(1, currency.getUuid().toString());
            stmt.execute();
            stmt = connection.prepareStatement("DELETE FROM " + tableCurrencies + " WHERE uuid = ?");
            stmt.setString(1, currency.getUuid().toString());
            stmt.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @Override
    public LinkedHashMap<String, Double> getTopList(Currency currency, int offset, int amount) {
        if (this.cachedTopList.containsKey(currency.getUuid())) {
            CachedTopList ctl = this.cachedTopList.get(currency.getUuid());
            if (ctl.matches(currency, offset, amount) && !ctl.isExpired()) {
                return ctl.getResults();
            }
        }

        LinkedHashMap<String, Double> resultPair = new LinkedHashMap<>();
        try (Connection connection = getHikariSource().getConnection()) {
            LinkedHashMap<String, Double> idBalancePair = new LinkedHashMap<>();
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "_balances WHERE currency_id = ? ORDER BY balance DESC LIMIT " + offset + ", " + amount);
            stmt.setString(1, currency.getUuid().toString());
            ResultSet set = stmt.executeQuery();
            while (set.next()) {
                idBalancePair.put(set.getString("account_id"), set.getDouble("balance"));
            }
            set.close();
            if (idBalancePair.size() > 0) {
                for (String id : idBalancePair.keySet()) {
                    stmt = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "_accounts WHERE uuid = ? LIMIT 1");
                    stmt.setString(1, id);
                    set = stmt.executeQuery();
                    if (set.next()) {
                        resultPair.put(set.getString("nickname"), idBalancePair.get(id));
                    }
                    set.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        CachedTopList ctl2 = new CachedTopList(currency, amount, offset, System.currentTimeMillis());
        ctl2.setResults(resultPair);
        this.cachedTopList.put(currency.getUuid(), ctl2);
        return resultPair;
    }

    private Account returnAccountWithBalances(Account account) {
        if (account == null) {
            return null;
        }
        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "_balances WHERE account_id = ?");
            stmt.setString(1, account.getUuid().toString());
            ResultSet set = stmt.executeQuery();
            while (set.next()) {
                Currency currency = plugin.getCurrencyManager().getCurrency(UUID.fromString(set.getString("currency_id")));
                if (currency == null) {
                    continue;
                }
                account.modifyBalance(currency, set.getDouble("balance"), false);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return account;
    }

    @Override
    public Account loadAccount(String name) {
        Account account = null;

        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "_accounts WHERE nickname = ? LIMIT 1");
            stmt.setString(1, name);
            ResultSet set = stmt.executeQuery();
            if (set.next()) {
                account = new Account(UUID.fromString(set.getString("uuid")), set.getString("nickname"));
                account.setCanReceiveCurrency(set.getInt("payable") == 1);
            }
            set.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return this.returnAccountWithBalances(account);
    }

    @Override
    public Account loadAccount(UUID uuid) {
        Account account = null;

        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "_accounts WHERE uuid = ? LIMIT 1");
            stmt.setString(1, uuid.toString());
            ResultSet set = stmt.executeQuery();
            if (set.next()) {
                account = new Account(uuid, set.getString("nickname"));
                account.setCanReceiveCurrency(set.getInt("payable") == 1);
            }
            set.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return this.returnAccountWithBalances(account);
    }

    @Override
    public ArrayList<Account> getOfflineAccounts() {
        ArrayList<Account> accounts = new ArrayList<>();

        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "_accounts;");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                accounts.add(returnAccountWithBalances(loadAccount(UUID.fromString(rs.getString("uuid")))));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return accounts;
    }

    @Override
    public void createAccount(Account account) {
        final String SAVE_ACCOUNT = "INSERT INTO `" + getTablePrefix() + "_accounts` (`nickname`, `uuid`, `payable`) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`), `uuid` = VALUES(`uuid`), `payable` = VALUES(`payable`)";

        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(SAVE_ACCOUNT);
            stmt.setString(1, account.getDisplayName());
            stmt.setString(2, account.getUuid().toString());
            stmt.setInt(3, account.canReceiveCurrency() ? 1 : 0);
            stmt.execute();


            for (Currency currency : plugin.getCurrencyManager().getCurrencies()) {
                double balance = currency.getDefaultBalance();
                stmt = connection.prepareStatement("SELECT * FROM " + this.getTablePrefix() + "_balances WHERE account_id = ? AND currency_id = ? LIMIT 1");
                stmt.setString(1, account.getUuid().toString());
                stmt.setString(2, currency.getUuid().toString());
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    stmt = connection.prepareStatement("INSERT INTO " + this.getTablePrefix() + "_balances (account_id, currency_id, balance) VALUES (?, ?, ?)");
                    stmt.setString(1, account.getUuid().toString());
                    stmt.setString(2, currency.getUuid().toString());
                    stmt.setDouble(3, balance);
                    stmt.execute();
                }
                rs.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        plugin.getUpdateForwarder().sendUpdateMessage("account", account.getUuid().toString());
    }

    @Override
    public void saveAccount(Account account) {
        final String SAVE_ACCOUNT = "INSERT INTO `" + getTablePrefix() + "_accounts` (`nickname`, `uuid`, `payable`) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE `nickname` = ?, `payable` = ?";
        final String SAVE_BALANCES = "INSERT INTO `" + getTablePrefix() + "_balances` (`account_id`, `currency_id`, `balance`) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE `balance` = VALUES(`balance`)";

        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement(SAVE_ACCOUNT);
            stmt.setString(1, account.getDisplayName());
            stmt.setString(2, account.getUuid().toString());
            stmt.setInt(3, account.canReceiveCurrency() ? 1 : 0);
            stmt.setString(4, account.getDisplayName());
            stmt.setInt(5, account.canReceiveCurrency() ? 1 : 0);
            stmt.execute();

            for (Currency currency : plugin.getCurrencyManager().getCurrencies()) {
                double balance = account.getBalance(currency.getPlural());
                if (balance == -100) {
                    balance = currency.getDefaultBalance();
                }

                stmt = connection.prepareStatement(SAVE_BALANCES);
                stmt.setString(1, account.getUuid().toString());
                stmt.setString(2, currency.getUuid().toString());
                stmt.setDouble(3, balance);
                stmt.execute();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        plugin.getUpdateForwarder().sendUpdateMessage("account", account.getUuid().toString());
    }

    @Override
    public void deleteAccount(Account account) {
        try (Connection connection = getHikariSource().getConnection()) {
            PreparedStatement stmt = connection.prepareStatement("DELETE FROM " + this.getTablePrefix() + "_accounts WHERE uuid = ? LIMIT 1");
            stmt.setString(1, account.getUuid().toString());
            stmt.execute();
            stmt = connection.prepareStatement("DELETE FROM " + this.getTablePrefix() + "_balances WHERE account_id = ?");
            stmt.setString(1, account.getUuid().toString());
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
