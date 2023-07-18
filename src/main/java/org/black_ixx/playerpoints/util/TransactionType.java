package org.black_ixx.playerpoints.util;

public enum TransactionType {
    GIVE,
    SET,
    RESET,
    RANDOM_GIVE,
    TAKE,
    NOT_ENOUGH_MONEY,
    UNKNOWN, EXCHANGE;

    public static TransactionType fromString(String str) {
        switch (str.toUpperCase()) {
            case "GIVE":
                return TransactionType.GIVE;
            case "SET":
                return TransactionType.SET;
            case "RESET":
                return TransactionType.RESET;
            case "RANDOM_GIVE":
                return TransactionType.RANDOM_GIVE;
            case "TAKE":
                return TransactionType.TAKE;

            default:
                return TransactionType.UNKNOWN;
        }
    }
}
