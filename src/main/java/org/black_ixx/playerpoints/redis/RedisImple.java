package org.black_ixx.playerpoints.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.lettuce.core.RedisClient;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.manager.DataManager;
import org.black_ixx.playerpoints.util.TransactionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RedisImple extends RedisAbstract {

    private final Gson gson;
    private final PlayerPoints plugin;

    public RedisImple(RedisClient lettuceRedisClient, int size, PlayerPoints plugin) {
        super(lettuceRedisClient, size);
        gson = new Gson();
        this.plugin = plugin;
        subscribe();
    }

    public void publish(String player, double balance, TransactionType type) {
        JsonObject json = new JsonObject();
        json.addProperty("player", player);
        json.addProperty("balance", balance);
        json.addProperty("type", type.name());
        getConnectionAsync(c -> c.publish(RedisKeys.PLAYER_POINTS.getKey(), json.toString()));
    }

    public void subscribe() {
        getPubSubConnection(c -> {
            c.addListener(new RedisPubSub<>() {
                @Override
                public void message(String channel, String message) {
                    JsonObject json = gson.fromJson(message, JsonObject.class);
                    String type = json.get("type").getAsString();
                    String player = json.get("player").getAsString();
                    double balance = json.get("balance").getAsDouble();

                    Player p = Bukkit.getPlayer(player);

                    if(p == null) return;

                    plugin.getManager(DataManager.class).getOnlineData(p).setPoints(balance);

                    System.out.println("Received message: " + message);
                }
            });
            c.async().subscribe(RedisKeys.PLAYER_POINTS.getKey());
        });
    }
}
