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

public class ResetCommand extends PointsCommand {

    public ResetCommand() {
        super("reset", CommandManager.CommandAliases.RESET);
    }

    @Override
    public void execute(PlayerPoints plugin, CommandSender sender, String[] args) {
        LocaleManager localeManager = plugin.getManager(LocaleManager.class);
        if (args.length < 1) {
            localeManager.sendMessage(sender, "command-reset-usage");
            return;
        }

        plugin.getManager(DataManager.class).getUserData(args[0]).thenAccept(player -> {
            if (player == null) {
                localeManager.sendMessage(sender, "unknown-player", StringPlaceholders.single("player", args[0]));
                return;
            }

            if (plugin.getAPI().reset(player.getUuid())) {
                localeManager.sendMessage(sender, "command-reset-success", StringPlaceholders.builder("player", player.getName())
                        .addPlaceholder("currency", localeManager.getCurrencyName(0))
                        .build());

                plugin.getUserLogSQL().addLog(plugin.getManager(DataManager.class).getOnlineData(player.getUuid()), TransactionType.RESET, "reset by " + sender.getName(), 0);

            }

        });
    }

    @Override
    public List<String> tabComplete(PlayerPoints plugin, CommandSender sender, String[] args) {
        return args.length == 1 ? PointsUtils.getPlayerTabComplete(args[0]) : Collections.emptyList();
    }

}
