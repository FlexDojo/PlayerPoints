package org.black_ixx.playerpoints.commands;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import java.util.Collections;
import java.util.List;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.manager.CommandManager;
import org.black_ixx.playerpoints.manager.DataManager;
import org.black_ixx.playerpoints.manager.LocaleManager;
import org.black_ixx.playerpoints.util.PointsUtils;
import org.black_ixx.playerpoints.util.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveCommand extends PointsCommand {

    public GiveCommand() {
        super("give", CommandManager.CommandAliases.GIVE);
    }

    @Override
    public void execute(PlayerPoints plugin, CommandSender sender, String[] args) {
        LocaleManager localeManager = plugin.getManager(LocaleManager.class);
        if (args.length < 2) {
            localeManager.sendMessage(sender, "command-give-usage");
            return;
        }

        plugin.getManager(DataManager.class).getUserData(args[0]).thenAccept(player -> {
            if (player == null) {
                localeManager.sendMessage(sender, "unknown-player", StringPlaceholders.single("player", args[0]));
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                localeManager.sendMessage(sender, "invalid-amount");
                return;
            }

            if (amount <= 0) {
                localeManager.sendMessage(sender, "invalid-amount");
                return;
            }


            if (plugin.getAPI().giveCommand(player.getUuid(), amount)) {

                // Send message to receiver
                Player onlinePlayer = Bukkit.getPlayer(player.getUuid());

                if (onlinePlayer != null) {
                    localeManager.sendMessage(onlinePlayer, "command-give-received", StringPlaceholders.builder("amount", PointsUtils.formatPoints(amount))
                            .addPlaceholder("currency", localeManager.getCurrencyName(amount))
                            .build());
                }

                // Send message to sender
                localeManager.sendMessage(sender, "command-give-success", StringPlaceholders.builder("amount", PointsUtils.formatPoints(amount))
                        .addPlaceholder("currency", localeManager.getCurrencyName(amount))
                        .addPlaceholder("player", player.getName())
                        .build());

                // Log transaction
                plugin.getUserLogSQL().addLog(plugin.getManager(DataManager.class).getOnlineData(player.getUuid()), TransactionType.GIVE, "given by " + sender.getName(), amount);

            } else {
                localeManager.sendMessage(sender, "command-give-failed");
            }
        });
    }

    @Override
    public List<String> tabComplete(PlayerPoints plugin, CommandSender sender, String[] args) {
        if (args.length == 1) {
            return PointsUtils.getPlayerTabComplete(args[0]);
        } else if (args.length == 2) {
            return Collections.singletonList("<amount>");
        } else {
            return Collections.emptyList();
        }
    }

}
