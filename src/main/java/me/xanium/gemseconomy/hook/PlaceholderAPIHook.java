package me.xanium.gemseconomy.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.xanium.gemseconomy.GemsEconomy;
import me.xanium.gemseconomy.account.Account;
import me.xanium.gemseconomy.currency.Currency;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends PlaceholderExpansion {


    @Override
    public @NotNull String getIdentifier() {
        return "gemseconomy";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Xanium";
    }

    @Override
    public @NotNull String getVersion() {
        return "5.0";
    }
    @Override
    public String onRequest(OfflinePlayer player, String identifier){


        if (player == null) {
            return "";
        }
        Account a = GemsEconomy.getInstance().getAccountManager().getAccount(player.getUniqueId());
        Currency dc = GemsEconomy.getInstance().getCurrencyManager().getDefaultCurrency();
        identifier = identifier.toLowerCase();

        if(identifier.equalsIgnoreCase("balance_default")){
            String amount = "";
            return amount + Math.round(a.getBalance(dc));
        }else if(identifier.equalsIgnoreCase("balance_default_formatted")){
            return dc.format(a.getBalance(dc));
        }

        else if(identifier.startsWith("balance_") || !identifier.startsWith("balance_default")) {
            String[] currencyArray = identifier.split("_");
            Currency c = GemsEconomy.getInstance().getCurrencyManager().getCurrency(currencyArray[1]);
            if (identifier.equalsIgnoreCase("balance_" + currencyArray[1] + "_formatted")) {
                return c.format(a.getBalance(c));
            } else {
                String amount = "";
                return amount + Math.round(a.getBalance(c));
            }
        }

        return null;
    }
}
