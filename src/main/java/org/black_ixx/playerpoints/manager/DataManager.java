package org.black_ixx.playerpoints.manager;

import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.database.SQLiteConnector;
import dev.rosewood.rosegarden.manager.AbstractDataManager;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.database.migrations._1_Create_Tables;
import org.black_ixx.playerpoints.database.migrations._2_Add_Table_Username_Cache;
import org.black_ixx.playerpoints.manager.data.UserData;
import org.black_ixx.playerpoints.models.SortedPlayer;
import org.black_ixx.playerpoints.util.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DataManager extends AbstractDataManager implements Listener {

    private final PlayerPoints plugin;
    private final Map<UUID, UserData> userDataCache;



    public DataManager(RosePlugin plugin) {
        super(plugin);

        this.plugin = (PlayerPoints) plugin;

        this.userDataCache = new ConcurrentHashMap<>();


        Bukkit.getPluginManager().registerEvents(this, rosePlugin);

    }

    public void loadOnlinePlayers() {
        Bukkit.getOnlinePlayers().forEach(player -> loadUser(player.getUniqueId()).thenAccept(data -> {

            if (data == null) {
                data = new UserData(player.getUniqueId(), player.getName(), 0);
                plugin.getQueries().createData(data);
            }

            plugin.getLogger().info("Loaded " + data.getName() + " (" + data.getUuid() + ") with " + data.getPoints() + " points.");

            this.userDataCache.put(player.getUniqueId(), data);
        }));
    }

    @Override
    public void disable() {

        super.disable();
    }

    public UserData getOnlineData(Player player) {
        return this.userDataCache.get(player.getUniqueId());
    }

    public UserData getOnlineData(UUID uuid) {
        return this.userDataCache.getOrDefault(uuid, null);
    }

    public CompletableFuture<UserData> getUserData(UUID uuid) {
        if (this.userDataCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(this.userDataCache.get(uuid));
        } else {
            return loadUser(uuid);
        }
    }

    public CompletableFuture<UserData> getUserData(String name) {
        Optional<UserData> opt = userDataCache.values().stream().filter(d -> d.getName().equals(name)).findFirst();
        return opt.map(CompletableFuture::completedFuture).orElseGet(() -> loadUser(name));
    }

    public CompletableFuture<UserData> loadUser(UUID uuid) {
        return plugin.getQueries().loadUser(uuid);
    }

    public CompletableFuture<UserData> loadUser(String name) {
        return plugin.getQueries().loadUser(name);
    }

    public CompletableFuture<Boolean> setPoints(UUID uuid, double points) {
        plugin.getUserLogSQL().addLog(plugin.getManager(DataManager.class).getOnlineData(uuid), TransactionType.SET, (Thread.currentThread().getStackTrace()[5].getClassName()), points);
        return updatePoints(getUserData(uuid), points, 2);
    }

    public CompletableFuture<Boolean> setPointsCommand(UUID name, double points) {
        return updatePoints(getUserData(name), points, 2);
    }

    public CompletableFuture<Boolean> givePoints(UUID uuid, double points) {
        plugin.getUserLogSQL().addLog(plugin.getManager(DataManager.class).getOnlineData(uuid), TransactionType.GIVE, (Thread.currentThread().getStackTrace()[5].getClassName()), points);
        return updatePoints(getUserData(uuid), points, 0);
    }

    public CompletableFuture<Boolean> givePointsCommand(UUID uuid, double points) {
        return updatePoints(getUserData(uuid), points, 0);
    }


    public CompletableFuture<Boolean> givePoints(String name, double points) {
        return updatePoints(getUserData(name), points, 0);
    }

    public CompletableFuture<Boolean> takePoints(UUID uuid, double points) {
        plugin.getUserLogSQL().addLog(plugin.getManager(DataManager.class).getOnlineData(uuid), TransactionType.TAKE, (Thread.currentThread().getStackTrace()[5].getClassName()), points);
        return updatePoints(getUserData(uuid), points, 1);
    }

    public CompletableFuture<Boolean> takePointsCommand(UUID uuid, double points) {
        return updatePoints(getUserData(uuid), points, 1);
    }

    public CompletableFuture<Boolean> takePoints(String name, double points) {
        return updatePoints(getUserData(name), points, 1);
    }

    private CompletableFuture<Boolean> updatePoints(CompletableFuture<UserData> userData, double points, int action) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        if (userData == null) {
            future.complete(false);
            return future;
        }
        userData.thenAccept(data -> {
            if (data == null) {
                future.complete(false);
                return;
            }

            boolean online = Bukkit.getPlayer(data.getUuid()) != null;

            switch (action) {
                case 0 -> {
                    data.setPoints(data.getPoints() + points);
                    if (!online) plugin.getRedis().publish(data.getName(), data.getPoints(), TransactionType.GIVE);
                }
                case 1 -> {
                    data.setPoints(Math.max(0, data.getPoints() - points));
                    if (!online) plugin.getRedis().publish(data.getName(), data.getPoints(), TransactionType.TAKE);
                }
                case 2 -> {
                    data.setPoints(points);
                    if (!online) plugin.getRedis().publish(data.getName(), data.getPoints(), TransactionType.SET);
                }
            }
            plugin.getQueries().updateData(data);
            future.complete(true);
        });
        return future;
    }


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        loadUser(event.getUniqueId()).thenAccept(data -> {
            if (data == null) {
                data = new UserData(event.getUniqueId(), event.getName(), 0);
                plugin.getQueries().createData(data);
            }
            userDataCache.put(event.getUniqueId(), data);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        userDataCache.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Gets the effective amount of points that a player has (includes pending transactions)
     *
     * @param playerId The player ID to use to get the points
     * @return the effective points value
     */
    public int getEffectivePoints(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return 0;
        return (int) getOnlineData(player).getPoints();
    }


    /**
     * Adds a pending transaction to offset the player's points by a specified amount
     *
     * @param playerId The Player to offset the points of
     * @param amount   The amount to offset by
     * @return true if the transaction was successful, false otherwise
     */
    public boolean offsetPoints(UUID playerId, int amount) {
        int points = this.getEffectivePoints(playerId);
        if (points + amount < 0)
            return false;

        return true;
    }


    public boolean offsetAllPoints(int amount) {
        if (amount == 0)
            return true;

        plugin.getQueries().giveAll(amount);

        return true;
    }

    public List<SortedPlayer> getTopSortedPoints(Integer limit) {
        return plugin.getQueries().getTop(limit == null ? 10 : limit).join();
    }

    public Map<UUID, Long> getOnlineTopSortedPointPositions() {
        Map<UUID, Long> players = new HashMap<>();
        if (Bukkit.getOnlinePlayers().isEmpty())
            return players;

        Map<UUID, Integer> points = new HashMap<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UserData data = this.getOnlineData(player);
            if (data == null)
                continue;

            points.put(player.getUniqueId(), (int) data.getPoints());
        }

        points.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).forEachOrdered(entry -> players.put(entry.getKey(), points.size() - points.entrySet().stream().filter(e -> e.getValue() > entry.getValue()).count()));

        return players;
    }

    public void importData(SortedSet<SortedPlayer> data, Map<UUID, String> cachedUsernames) {
        this.databaseConnector.connect(connection -> {
            String purgeQuery = "DELETE FROM " + this.getPointsTableName();
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(purgeQuery);
            }

            String batchInsert = "INSERT INTO " + this.getPointsTableName() + " (" + this.getUuidColumnName() + ", points) VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(batchInsert)) {
                for (SortedPlayer playerData : data) {
                    statement.setString(1, playerData.getUniqueId().toString());
                    statement.setInt(2, playerData.getPoints());
                    statement.addBatch();
                }
                statement.executeBatch();
            }

            if (!cachedUsernames.isEmpty())
                this.updateCachedUsernames(cachedUsernames);
        });
    }

    public boolean importLegacyTable(String tableName) {
        AtomicBoolean value = new AtomicBoolean();
        this.databaseConnector.connect(connection -> {
            try {
                String selectQuery = "SELECT playername, points FROM " + tableName;
                Map<UUID, Integer> points = new HashMap<>();
                try (Statement statement = connection.createStatement()) {
                    ResultSet result = statement.executeQuery(selectQuery);
                    while (result.next()) {
                        UUID uuid = UUID.fromString(result.getString(1));
                        int pointValue = result.getInt(2);
                        points.put(uuid, pointValue);
                    }
                }

                boolean isSqlite = this.databaseConnector instanceof SQLiteConnector;
                String insertQuery;
                if (isSqlite) {
                    insertQuery = "REPLACE INTO " + this.getPointsTableName() + " (" + this.getUuidColumnName() + ", points) VALUES (?, ?)";
                } else {
                    insertQuery = "INSERT INTO " + this.getPointsTableName() + " (" + this.getUuidColumnName() + ", points) VALUES (?, ?) ON DUPLICATE KEY UPDATE points = ?";
                }

                try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                    for (Map.Entry<UUID, Integer> entry : points.entrySet()) {
                        statement.setString(1, entry.getKey().toString());
                        statement.setInt(2, entry.getValue());
                        if (!isSqlite)
                            statement.setInt(3, entry.getValue());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }

                value.set(true);
            } catch (Exception e) {
                value.set(false);
                e.printStackTrace();
            }
        });
        return value.get();
    }

    public void updateCachedUsernames(Map<UUID, String> cachedUsernames) {
        this.databaseConnector.connect(connection -> {
            String query;
            boolean isSqlite = this.databaseConnector instanceof SQLiteConnector;
            if (isSqlite) {
                query = "REPLACE INTO " + this.getTablePrefix() + "username_cache (uuid, username) VALUES (?, ?)";
            } else {
                query = "INSERT INTO " + this.getTablePrefix() + "username_cache (uuid, username) VALUES (?, ?) ON DUPLICATE KEY UPDATE username = ?";
            }

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                for (Map.Entry<UUID, String> entry : cachedUsernames.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setString(2, entry.getValue());
                    if (!isSqlite)
                        statement.setString(3, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        });
    }

    public String lookupCachedUsername(UUID uuid) {
        AtomicReference<String> value = new AtomicReference<>();
        this.databaseConnector.connect(connection -> {
            String query = "SELECT username FROM " + this.getTablePrefix() + "username_cache WHERE uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, uuid.toString());
                ResultSet result = statement.executeQuery();
                if (result.next())
                    value.set(result.getString(1));
            }
        });

        String name = value.get();
        if (name == null) {
            return "Unknown";
        } else {
            return name;
        }
    }

    public UUID lookupCachedUUID(String username) {
        AtomicReference<UUID> value = new AtomicReference<>();
        this.databaseConnector.connect(connection -> {
            String query = "SELECT uuid FROM " + this.getTablePrefix() + "username_cache WHERE username = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, username);
                ResultSet result = statement.executeQuery();
                if (result.next())
                    value.set(UUID.fromString(result.getString(1)));
            }
        });

        return value.get();
    }

    private String getPointsTableName() {
        if (ConfigurationManager.Setting.LEGACY_DATABASE_MODE.getBoolean()) {
            return ConfigurationManager.Setting.LEGACY_DATABASE_NAME.getString();
        } else {
            return super.getTablePrefix() + "points";
        }
    }

    private String getUuidColumnName() {
        if (ConfigurationManager.Setting.LEGACY_DATABASE_MODE.getBoolean()) {
            return "playername";
        } else {
            return "uuid";
        }
    }

    @Override
    public List<Class<? extends DataMigration>> getDataMigrations() {
        return Arrays.asList(
                _1_Create_Tables.class,
                _2_Add_Table_Username_Cache.class
        );
    }

    public boolean doesDataExist() {
        return true;
    }
}
