package org.black_ixx.playerpoints.database;

import dev.rosewood.rosegarden.manager.AbstractDataManager;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.manager.data.UserData;
import org.black_ixx.playerpoints.models.SortedPlayer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Queries {

    private final PlayerPoints plugin;
    private final ExecutorService pool;

    public Queries(PlayerPoints plugin) {
        this.plugin = plugin;
        this.pool = Executors.newFixedThreadPool(15);
        createTable();
    }

    private void createTable() {
        CompletableFuture.runAsync(() -> {
            plugin.getManager(AbstractDataManager.class).getDatabaseConnector().connect(c -> {
                try (PreparedStatement statement = c.prepareStatement("CREATE TABLE IF NOT EXISTS `userdata` (`uuid` VARCHAR(50) PRIMARY KEY, `name` VARCHAR(50), `points` DOUBLE)")) {
                    statement.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, pool);
    }

    public CompletableFuture<List<String>> getAllNames() {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            plugin.getManager(AbstractDataManager.class).getDatabaseConnector().connect(c -> {
                try (PreparedStatement statement = c.prepareStatement("SELECT `name` FROM `userdata`")) {
                    ResultSet resultSet = statement.executeQuery();
                    List<String> names = new ArrayList<>();
                    while (resultSet.next()) {
                        names.add(resultSet.getString("name"));
                    }
                    future.complete(names);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, pool);
        return future;
    }

    public CompletableFuture<UserData> loadUser(UUID uuid) {
        CompletableFuture<UserData> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            plugin.getManager(AbstractDataManager.class).getDatabaseConnector().connect(c -> {
                try (PreparedStatement statement = c.prepareStatement("SELECT * FROM `userdata` WHERE `uuid` = ?")) {
                    statement.setString(1, uuid.toString());
                    ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        future.complete(new UserData(
                                UUID.fromString(resultSet.getString("uuid")),
                                resultSet.getString("name"),
                                resultSet.getDouble("points")
                        ));
                    } else {
                        future.complete(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        }, pool);
        return future;
    }

    public CompletableFuture<UserData> loadUser(String name) {
        CompletableFuture<UserData> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            plugin.getManager(AbstractDataManager.class).getDatabaseConnector().connect(c -> {
                try (PreparedStatement statement = c.prepareStatement("SELECT * FROM `userdata` WHERE `name` = ?")) {
                    statement.setString(1, name);
                    ResultSet resultSet = statement.executeQuery();

                    if (resultSet.next()) {
                        future.complete(new UserData(
                                UUID.fromString(resultSet.getString("uuid")),
                                resultSet.getString("name"),
                                resultSet.getDouble("points")
                        ));
                    } else {
                        future.complete(null);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        }, pool);
        return future;
    }

    public CompletableFuture<Void> updateUserData(UUID uuid, String newName) {
        return CompletableFuture.runAsync(() -> {
            plugin.getManager(AbstractDataManager.class).getDatabaseConnector().connect(c -> {
                try (PreparedStatement statement = c.prepareStatement("UPDATE `userdata` SET `name` = ? WHERE `uuid` = ?")) {
                    statement.setString(1, newName);
                    statement.setString(2, uuid.toString());
                    statement.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, pool);
    }

    public void createData(UserData data) {
        CompletableFuture.runAsync(() -> {
            plugin.getManager(AbstractDataManager.class).getDatabaseConnector().connect(c -> {
                try (PreparedStatement statement = c.prepareStatement("INSERT INTO `userdata` (`uuid`, `name`, `points`) VALUES (?, ?, ?)")) {
                    statement.setString(1, data.getUuid().toString());
                    statement.setString(2, data.getName());
                    statement.setDouble(3, data.getPoints());
                    statement.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, pool);
    }

    public void updateData(UserData data) {
        CompletableFuture.runAsync(() -> {
            plugin.getManager(AbstractDataManager.class).getDatabaseConnector().connect(c -> {
                try (PreparedStatement statement = c.prepareStatement("UPDATE `userdata` SET `name` = ?, `points` = ? WHERE `uuid` = ?")) {
                    statement.setString(1, data.getName());
                    statement.setDouble(2, data.getPoints());
                    statement.setString(3, data.getUuid().toString());
                    statement.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, pool);
    }

    public void giveAll(double amount) {
        CompletableFuture.runAsync(() -> {
            plugin.getManager(AbstractDataManager.class).getDatabaseConnector().connect(c -> {
                try (PreparedStatement statement = c.prepareStatement("UPDATE `userdata` SET `points` = `points` + ?")) {
                    statement.setDouble(1, amount);
                    statement.executeUpdate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }, pool);
    }

    public CompletableFuture<List<SortedPlayer>> getTop(int amount) {
        CompletableFuture<List<SortedPlayer>> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> plugin.getManager(AbstractDataManager.class).getDatabaseConnector().connect(c -> {
            try (PreparedStatement statement = c.prepareStatement("SELECT * FROM `userdata` ORDER BY `points` DESC LIMIT ?")) {
                statement.setInt(1, amount);
                ResultSet resultSet = statement.executeQuery();

                List<SortedPlayer> sortedPlayers = new ArrayList<>();
                while (resultSet.next()) {
                    sortedPlayers.add(new SortedPlayer(
                            UUID.fromString(resultSet.getString("uuid")),
                            resultSet.getString("name"),
                            (int) resultSet.getDouble("points")
                    ));
                }
                future.complete(sortedPlayers);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }), pool);
        return future;
    }


}
