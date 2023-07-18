package org.black_ixx.playerpoints.commands;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.manager.CommandManager;
import org.black_ixx.playerpoints.manager.DataManager;
import org.black_ixx.playerpoints.manager.LocaleManager;
import org.black_ixx.playerpoints.util.PointsUtils;
import org.black_ixx.playerpoints.util.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Collections;
import java.util.List;

public class ExchangeCommand extends PointsCommand {

    private Economy economy;

    public ExchangeCommand() {
        super("exchange", CommandManager.CommandAliases.EXCHANGE);
        setupEconomy();
    }


    @Override
    public void execute(PlayerPoints plugin, CommandSender sender, String[] args) {
        LocaleManager localeManager = plugin.getManager(LocaleManager.class);
        if (args.length < 2) {
            localeManager.sendMessage(sender, "command-execute-usage");
            return;
        }

        plugin.getManager(DataManager.class).getUserData(args[0]).thenAccept(player -> {
            if (player == null) {
                localeManager.sendMessage(sender, "unknown-player", StringPlaceholders.single("player", args[0]));
                return;
            }

            int amount;

            double balance = economy.getBalance(Bukkit.getOfflinePlayer(player.getUuid()));

            Player onlineTarget = Bukkit.getPlayer(player.getUuid());

            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    localeManager.sendMessage(sender, "invalid-amount");
                    return;
                }
                if (balance < amount) {
                    localeManager.sendMessage(onlineTarget, "command-exchange-not-enough", StringPlaceholders.builder("player", player.getName())
                            .addPlaceholder("amount-vault", formatAmount(amount) + " " + localeManager.getExchangeCurrencyName(amount))
                            .build());

                    localeManager.sendMessage(sender, "command-exchange-not-enough-console", StringPlaceholders.builder("player", player.getName())
                            .addPlaceholder("amount-vault", formatAmount(amount) + " " + localeManager.getExchangeCurrencyName(amount))
                            .addPlaceholder("target", player.getName())
                            .build());
                    return;
                }
            } catch (NumberFormatException e) {
                localeManager.sendMessage(sender, "invalid-amount");
                return;
            }

            int fluxAmount = convertToFlux(amount);


            if (plugin.getAPI().exchangeCommand(player.getUuid(), fluxAmount)) {

                economy.withdrawPlayer(Bukkit.getOfflinePlayer(player.getUuid()), amount);


                // Send message to sender
                localeManager.sendMessage(sender, "command-exchange-success-console", StringPlaceholders.builder("player", player.getName())
                        .addPlaceholder("amount-vault", formatAmount(amount) + " " + localeManager.getExchangeCurrencyName(amount))
                        .addPlaceholder("amount-flux", fluxAmount + " " + localeManager.getCurrencyName(amount))
                        .addPlaceholder("target", player.getName())
                        .build());

                // Send message to target
                localeManager.sendMessage(onlineTarget, "command-exchange-success", StringPlaceholders.builder("player", player.getName())
                        .addPlaceholder("amount-vault", formatAmount(amount) + " " + localeManager.getExchangeCurrencyName(amount))
                        .addPlaceholder("amount-flux", fluxAmount + " " + localeManager.getCurrencyName(amount))
                        .build());


                plugin.getUserLogSQL().addLog(plugin.getManager(DataManager.class).getOnlineData(player.getUuid()), TransactionType.EXCHANGE, "Exchanged " + amount + " corone for " + fluxAmount + " flux", fluxAmount);


            } else {

                localeManager.sendMessage(sender, "command-exchange-not-enough-console", StringPlaceholders.builder("player", player.getName())
                        .addPlaceholder("amount-vault", formatAmount(amount) + " " + localeManager.getExchangeCurrencyName(amount))
                        .addPlaceholder("target", player.getName())
                        .build());

                localeManager.sendMessage(onlineTarget, "command-exchange-not-enough", StringPlaceholders.builder("player", player.getName())
                        .addPlaceholder("amount-vault", formatAmount(amount) + " " + localeManager.getExchangeCurrencyName(amount))
                        .build());

            }
        });
    }

    @Override
    public List<String> tabComplete(PlayerPoints plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            return PointsUtils.getPlayerTabComplete(args[0]);
        } else if (args.length == 2) {
            return Collections.singletonList("<corone>");
        } else {
            return Collections.emptyList();
        }
    }

    public int convertToFlux(double amount) {
        double operation = (amount / 10000 / 5000);
        double toFlux = operation * 100;
        return (int) toFlux;
    }

    private void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
    }

    // format amount 1.000.000 to M, K, B etc
    public String formatAmount(double amount) {
        if (amount >= 1000000000) {
            return (amount / 1000000000) + "B";
        } else if (amount >= 1000000) {
            return (amount / 1000000) + "M";
        } else if (amount >= 1000) {
            return (amount / 1000) + "K";
        } else {
            return String.valueOf(amount);
        }
    }


}
