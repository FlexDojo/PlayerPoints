package org.black_ixx.playerpoints;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;

import java.util.Arrays;
import java.util.List;

import io.lettuce.core.RedisClient;
import lombok.Getter;
import me.lokka30.treasury.api.common.service.ServiceRegistry;
import me.lokka30.treasury.api.economy.EconomyProvider;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.database.Queries;
import org.black_ixx.playerpoints.database.UserLogSQL;
import org.black_ixx.playerpoints.hook.PointsPlaceholderExpansion;
import org.black_ixx.playerpoints.listeners.VotifierListener;
import org.black_ixx.playerpoints.manager.*;
import org.black_ixx.playerpoints.manager.ConfigurationManager.Setting;
import org.black_ixx.playerpoints.redis.RedisImple;
import org.black_ixx.playerpoints.treasury.PlayerPointsTreasuryLayer;
import org.black_ixx.playerpoints.util.PointsUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

/**
 * Main plugin class for PlayerPoints.
 */
public class PlayerPoints extends RosePlugin {

    private static PlayerPoints instance;
    private PlayerPointsAPI api;
    private PlayerPointsVaultLayer vaultLayer;
    private PlayerPointsTreasuryLayer treasuryLayer;
    private UserLogSQL userLogSQL;
    @Getter
    private Queries queries;
    @Getter
    private RedisImple redis;
    private PointsPlaceholderExpansion placeholderExpansion;
    @Getter
    private Economy vaultHook;

    public PlayerPoints() {
        super(80745, 10234, ConfigurationManager.class, DataManager.class, LocaleManager.class, null);
        instance = this;
    }

    @Override
    public void enable() {
        this.api = new PlayerPointsAPI(this);
        this.queries = new Queries(this);

        this.userLogSQL = new UserLogSQL();
        userLogSQL.createUserLogTable();

        setupVault();

        getManager(DataManager.class).loadOnlinePlayers();

        if (Setting.VAULT.getBoolean() && Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            this.vaultLayer = new PlayerPointsVaultLayer(this);

            // Check valid values for the priorities
            ServicePriority priority = null;
            String desiredPriority = Setting.VAULT_PRIORITY.getString();
            for (ServicePriority value : ServicePriority.values()) {
                if (value.name().equalsIgnoreCase(desiredPriority)) {
                    priority = value;
                    break;
                }
            }

            if (priority == null) {
                this.getLogger().warning("vault-priority value in the config.yml is invalid, defaulting to Low.");
                priority = ServicePriority.Low;
            }

            Bukkit.getServicesManager().register(Economy.class, this.vaultLayer, this, priority);
        }

        redis = new RedisImple(RedisClient.create(Setting.REDIS_URI.getString()), 15, this);

        if (Setting.TREASURY.getBoolean() && Bukkit.getPluginManager().isPluginEnabled("Treasury")) {
            this.treasuryLayer = new PlayerPointsTreasuryLayer(this);

            // Check valid values for the priorities
            me.lokka30.treasury.api.common.service.ServicePriority priority = null;
            String desiredPriority = Setting.TREASURY_PRIORITY.getString();
            for (me.lokka30.treasury.api.common.service.ServicePriority value : me.lokka30.treasury.api.common.service.ServicePriority.values()) {
                if (value.name().equalsIgnoreCase(desiredPriority)) {
                    priority = value;
                    break;
                }
            }

            if (priority == null) {
                this.getLogger().warning("treasury-priority value in the config.yml is invalid, defaulting to LOW.");
                priority = me.lokka30.treasury.api.common.service.ServicePriority.LOW;
            }

            ServiceRegistry.INSTANCE.registerService(EconomyProvider.class, new PlayerPointsTreasuryLayer(this), this.getName(), priority);
        }


        Bukkit.getScheduler().runTask(this, () -> {
            // Register placeholders, if applicable
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                placeholderExpansion = new PointsPlaceholderExpansion(this);
                placeholderExpansion.register();
            }

            // Register votifier listener, if applicable
            if (Setting.VOTE_ENABLED.getBoolean()) {
                Plugin votifier = Bukkit.getPluginManager().getPlugin("Votifier");
                if (votifier != null) {
                    Bukkit.getPluginManager().registerEvents(new VotifierListener(this), this);
                } else {
                    this.getLogger().warning("The hook for Votifier was enabled, but it does not appear to be installed.");
                }
            }
        });

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            queries.getAllNames().thenAccept(names -> {
                PointsUtils.getCachedNames().clear();
                PointsUtils.getCachedNames().addAll(names);
            });
        }, 20, 20 * 60 * 5);
    }

    @Override
    public void disable() {
        if (this.vaultLayer != null)
            Bukkit.getServicesManager().unregister(Economy.class, this.vaultLayer);

        if (this.treasuryLayer != null)
            ServiceRegistry.INSTANCE.unregister(EconomyProvider.class, this.treasuryLayer);

        if (Setting.BUNGEECORD_SEND_UPDATES.getBoolean()) {
            Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
            Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        }

        if(placeholderExpansion!=null) {
            placeholderExpansion.unregister();
        }
    }

    @Override
    public void reload() {
        super.reload();
        PointsUtils.setCachedValues(this);
    }

    @Override
    protected List<Class<? extends Manager>> getManagerLoadPriority() {
        return Arrays.asList(
                CommandManager.class,
                LeaderboardManager.class
        );
    }

    public static PlayerPoints getInstance() {
        return instance;
    }

    /**
     * Get the plugin's API.
     *
     * @return API instance.
     */
    public PlayerPointsAPI getAPI() {
        return this.api;
    }

    public UserLogSQL getUserLogSQL() {
        return this.userLogSQL;
    }

    private void setupVault() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        vaultHook = rsp.getProvider();
    }

}
