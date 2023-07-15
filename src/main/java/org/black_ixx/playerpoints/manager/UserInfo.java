package org.black_ixx.playerpoints.manager;

import org.black_ixx.playerpoints.util.TransactionType;

import javax.annotation.Nullable;
import java.util.UUID;

public class UserInfo {
    private final String uuid;
    private final String username;
    private final double balance;
    private final double amount;
    private final TransactionType transactionType;
    private final String date;
    @Nullable
    private final String comment;

    public UserInfo(String uuid, String username, double balance, double amount,
                    TransactionType transactionType, String date, String comment) {
        this.uuid = uuid;
        this.username = username;
        this.balance = balance;
        this.amount = amount;
        this.transactionType = transactionType;
        this.date = date;
        this.comment = comment;
    }

    public String getUUID() {
        return this.uuid;
    }

    public String getUsername() {
        return this.username;
    }

    public double getBalance() {
        return this.balance;
    }

    public double getAmount() {
        return this.amount;
    }

    public TransactionType getTransactionType() {
        return this.transactionType;
    }

    @Nullable
    public String getComment() {
        return this.comment;
    }


}
