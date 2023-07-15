package org.black_ixx.playerpoints.database;

import dev.rosewood.rosegarden.manager.AbstractDataManager;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.manager.UserInfo;
import org.black_ixx.playerpoints.manager.data.UserData;
import org.black_ixx.playerpoints.util.TransactionType;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class UserLogSQL {

    public void addLog(UserInfo userInfo) {
        CompletableFuture.runAsync(() -> {
            AbstractDataManager dataManager = PlayerPoints.getInstance().getManager(AbstractDataManager.class);
            dataManager.getDatabaseConnector().connect(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO user_logs (id, uuid, player, balance, amount, operation, comment) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                    statement.setInt(1, 0);
                    statement.setString(2, userInfo.getUUID());
                    statement.setString(3, userInfo.getUsername());
                    statement.setDouble(4, userInfo.getBalance());
                    statement.setDouble(5, userInfo.getAmount());
                    statement.setString(6, userInfo.getTransactionType().toString());
                    statement.setString(7, userInfo.getComment());
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public void addLog(UserData data, TransactionType transactionType, String comment, double amount) {
        CompletableFuture.runAsync(() -> {
            AbstractDataManager dataManager = PlayerPoints.getInstance().getManager(AbstractDataManager.class);
            dataManager.getDatabaseConnector().connect(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO user_logs (uuid, player, balance, amount, operation, comment) VALUES (?, ?, ?, ?, ?, ?)")) {
                    statement.setString(1, data.getUuid().toString());
                    statement.setString(2, data.getName());
                    statement.setDouble(3, data.getPoints());
                    statement.setDouble(4, amount);
                    statement.setString(5, transactionType.toString());
                    statement.setString(6, comment);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public void createUserLogTable() {
        CompletableFuture.runAsync(() -> {
            AbstractDataManager dataManager = PlayerPoints.getInstance().getManager(AbstractDataManager.class);
            dataManager.getDatabaseConnector().connect(connection -> {
                try (PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS user_logs (id INT PRIMARY KEY AUTO_INCREMENT, " +
                        "uuid VARCHAR(50), " +
                        "player VARCHAR(50), " +
                        "balance DOUBLE, " +
                        "amount DOUBLE, " +
                        "operation VARCHAR(50), " +
                        "`date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " + // Modified syntax for the `date` column
                        "comment VARCHAR(50))"
                )){
                    statement.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        });
    }
}


