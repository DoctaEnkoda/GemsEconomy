/*
 * Copyright Xanium Development (c) 2013-2018. All Rights Reserved.
 * Any code contained within this document, and any associated APIs with similar branding
 * are the sole property of Xanium Development. Distribution, reproduction, taking snippets or claiming
 * any contents as your own will break the terms of the license, and void any agreements with you, the third party.
 * Thank you.
 */

package me.xanium.gemseconomy.listeners;

import me.xanium.gemseconomy.GemsEconomy;
import me.xanium.gemseconomy.account.Account;
import me.xanium.gemseconomy.currency.Currency;
import me.xanium.gemseconomy.event.RedisPubAPI;
import me.xanium.gemseconomy.file.F;
import me.xanium.gemseconomy.utils.SchedulerUtils;
import me.xanium.gemseconomy.utils.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class EconomyListener implements Listener {

    private final GemsEconomy plugin = GemsEconomy.getInstance();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) return;
        SchedulerUtils.runAsync(()->{
            Account acc = plugin.getAccountManager().getAccount(player.getUniqueId());
            if(acc == null)
                plugin.getAccountManager().createAccount(player.getName());
            acc = plugin.getAccountManager().getAccount(player.getUniqueId());
            if(!acc.getNickname().equals(player.getName()))
                acc.setNickname(player.getName());
            UtilServer.consoleLog("Account name changes detected, updating: " + player.getName());
            plugin.getDataStore().saveAccount(acc);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getAccountManager().removeAccount(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Caching
        SchedulerUtils.run(() -> {
            Account account = plugin.getAccountManager().getAccount(player.getUniqueId());
            if (account != null) {
                plugin.getAccountManager().add(account);
            }
        });

        SchedulerUtils.runLater(40L, () -> {
            if (plugin.getCurrencyManager().getDefaultCurrency() == null && (player.isOp() || player.hasPermission("gemseconomy.command.currency"))) {
                player.sendMessage(F.getPrefix() + "§cYou have not made a currency yet. Please do so by \"§e/currency§c\".");
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRedisApi(final RedisPubAPI api) {
        String value = api.getValueRedis();
        String[] redisSplit = value.split(":");
        String uuid = redisSplit[1];
        String amount = redisSplit[2];
        String uuidCurrency = redisSplit[3];
        String singularCurrency = redisSplit[4];
        String pluralCurrency = redisSplit[5];

        Currency currency = new Currency(UUID.fromString(uuidCurrency), singularCurrency, pluralCurrency);

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));

        SchedulerUtils.runAsync(()->{
            Account acc = plugin.getAccountManager().getAccount(offlinePlayer.getUniqueId());
            if(acc == null) {
                return; // Pas besoin de le remettre dans le cache
            }
            if(!acc.getNickname().equals(offlinePlayer.getName())) {
                acc.setNickname(offlinePlayer.getName());
            }
            acc.modifyBalance(currency, Double.parseDouble(amount), false);
            UtilServer.consoleLog("Account name changes detected, updating: " + offlinePlayer.getName());
            plugin.getDataStore().saveAccount(acc);
        });
    }
}

