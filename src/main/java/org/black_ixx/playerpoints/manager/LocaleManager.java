package org.black_ixx.playerpoints.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.AbstractLocaleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.minecart.CommandMinecart;

public class LocaleManager extends AbstractLocaleManager {

    public LocaleManager(RosePlugin rosePlugin) {
        super(rosePlugin);
    }

    public String getCurrencyName(int value) {
        if (value == 1 || value == -1) {
            return this.getLocaleMessage("currency-singular");
        } else {
            return this.getLocaleMessage("currency-plural");
        }
    }

    public String getExchangeCurrencyName(int value) {
        if (value == 1 || value == -1) {
            return this.getLocaleMessage("exchange-currency-singular");
        } else {
            return this.getLocaleMessage("exchange-currency-plural");
        }
    }

    @Override
    protected void handleMessage(CommandSender sender, String message) {
        if (!Bukkit.isPrimaryThread() && (sender instanceof BlockCommandSender || sender instanceof CommandMinecart)) {
            Bukkit.getScheduler().runTask(this.rosePlugin, () -> super.handleMessage(sender, message));
        } else {
            super.handleMessage(sender, message);
        }
    }

}
