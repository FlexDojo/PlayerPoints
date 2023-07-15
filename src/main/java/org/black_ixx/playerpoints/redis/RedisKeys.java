package org.black_ixx.playerpoints.redis;


public enum RedisKeys {


    PLAYER_POINTS("player-points");


    private final String keyName;

    /**
     * @param keyName the name of the key
     */
    RedisKeys(final String keyName) {
        this.keyName = keyName;
    }

    /**
     * Use {@link #toString} instead of this method
     *
     * @return the name of the key
     */
    public String getKey() {
        return keyName;
    }

}
