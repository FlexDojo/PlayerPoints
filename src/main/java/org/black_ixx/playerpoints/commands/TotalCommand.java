package org.black_ixx.playerpoints.commands;

import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.manager.CommandManager;
import org.black_ixx.playerpoints.manager.DataManager;
import org.black_ixx.playerpoints.manager.LocaleManager;
import org.black_ixx.playerpoints.util.PointsUtils;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TotalCommand extends PointsCommand{
    public TotalCommand() {
        super("total", CommandManager.CommandAliases.TOTAL);
    }


    @Override
    public void execute(PlayerPoints plugin, CommandSender sender, String args[]) {
        if (args.length > 0) {
            // Handle the case where arguments are provided, but for /flux look, no arguments are needed
            sender.sendMessage("Usage: /flux total");
            return;
        }

        LocaleManager localeManager = plugin.getManager(LocaleManager.class);

        plugin.getManager(DataManager.class).getAllPoints().thenAccept(data -> {
            int totalPoints = data[0];
            int totalPlayers = data[1];

            localeManager.sendMessage(sender, "command-total-success", StringPlaceholders
                    .builder("currency", localeManager.getCurrencyName(totalPoints))
                    .addPlaceholder("amount", PointsUtils.formatPoints(totalPoints))
                    .addPlaceholder("players", String.valueOf(totalPlayers))
                    .build());
        });
    }


    @Override
    public List<String> tabComplete(PlayerPoints plugin, CommandSender sender, String[] args) {
        return args.length == 0 ? Collections.emptyList() : null;
    }

}
