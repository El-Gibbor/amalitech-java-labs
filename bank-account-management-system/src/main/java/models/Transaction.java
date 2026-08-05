package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {
    private static int transactionCounter = 0;

    // all final: a completed transaction is an immutable record
    private final String transactionId;
    private final String accountNumber;
    private final double amount;
    private final String type; // "Deposit" or "Withdrawal"
    private final double balanceAfter;
    private final LocalDateTime createdAt;
    private final String timestamp;

    public Transaction(String accountNumber, String type, double amount, double balanceAfter) {
        this.transactionId = String.format("TXN%03d", ++transactionCounter);
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.type = type;
        this.balanceAfter = balanceAfter;
        this.createdAt = LocalDateTime.now();
        this.timestamp = createdAt.format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm a"));
    }

    // Getters for all fields
    public String getTransactionId() { return transactionId; }

    public String getAccountNumber() { return accountNumber; }

    public double getAmount() { return amount; }

    public String getType() { return type; }

    public double getBalanceAfter() { return balanceAfter; }

    public String getTimestamp() { return timestamp; }

    /** @return the exact instant the transaction was recorded, for chronological comparison */
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void displayTransactionDetails() {
        System.out.println("Transaction ID: " + transactionId);
        System.out.println("Account Number: " + accountNumber);
        System.out.println("Amount: " + amount);
        System.out.println("Type: " + type);
        System.out.println("Balance After Transaction: " + balanceAfter);
        System.out.println("Timestamp: " + timestamp);
    }
}
