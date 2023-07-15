package org.black_ixx.playerpoints.commands;

import dev.rosewood.rosegarden.utils.StringPlaceholders;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.manager.CommandManager;
import org.black_ixx.playerpoints.manager.DataManager;
import org.black_ixx.playerpoints.manager.LocaleManager;
import org.black_ixx.playerpoints.manager.UserInfo;
import org.black_ixx.playerpoints.models.Tuple;
import org.black_ixx.playerpoints.util.PointsUtils;
import org.black_ixx.playerpoints.util.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

public class SetCommand extends PointsCommand {

    public SetCommand() {
        super("set", CommandManager.CommandAliases.SET);
    }

    @Override
    public void execute(PlayerPoints plugin, CommandSender sender, String[] args) {
        LocaleManager localeManager = plugin.getManager(LocaleManager.class);
        if (args.length < 2) {
            localeManager.sendMessage(sender, "command-set-usage");
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
                if (amount < 0) {
                    localeManager.sendMessage(sender, "invalid-amount");
                    return;
                }
            } catch (NumberFormatException e) {
                localeManager.sendMessage(sender, "invalid-amount");
                return;
            }

            if (plugin.getAPI().set(player.getUuid(), amount)) {
                localeManager.sendMessage(sender, "command-set-success", StringPlaceholders.builder("player", player.getName())
                        .addPlaceholder("currency", localeManager.getCurrencyName(amount))
                        .addPlaceholder("amount", PointsUtils.formatPoints(amount))
                        .build());

                plugin.getUserLogSQL().addLog(plugin.getManager(DataManager.class).getOnlineData(player.getUuid()), TransactionType.SET, "set by " + sender.getName(), amount);

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
