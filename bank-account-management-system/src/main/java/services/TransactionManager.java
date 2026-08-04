package services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import models.Transaction;
import utils.TableFormatter;

/** Stores and queries transactions for all accounts. */
public class TransactionManager {
    private List<Transaction> transactions = new ArrayList<>();

    /** @return true, since an ArrayList has no fixed capacity and a transaction is always accepted */
    public boolean addTransaction(Transaction transaction) {
        transactions.add(transaction);
        return true;
    }

    /** Prints the transaction history for the given account, newest first. */
    public void viewTransactionsByAccount(String accountNumber) {
        if (getTransactionCountByAccount(accountNumber) == 0) {
            printNoTransactionsMessage();
            return;
        }
        String[][] rows = buildTransactionRows(accountNumber);
        printTransactionTable(rows);
    }

    private void printNoTransactionsMessage() {
        String message = "No transactions recorded for this account.";
        String line = "─".repeat(message.length());
        System.out.println(line);
        System.out.println(message);
        System.out.println(line);
    }

    private String[][] buildTransactionRows(String accountNumber) {
        return newestFirstForAccount(accountNumber).stream()
                .map(this::formatTransactionRow)
                .toArray(String[][]::new);
    }

    private String[] formatTransactionRow(Transaction t) {
        String sign;
        if (t.getType().equalsIgnoreCase("deposit")) {
            sign = "+";
        } else {
            sign = "-";
        }
        return new String[] {
                t.getTransactionId(),
                t.getTimestamp(),
                t.getType().toUpperCase(),
                String.format("%s$%,.2f", sign, t.getAmount()),
                String.format("$%,.2f", t.getBalanceAfter())
        };
    }

    private void printTransactionTable(String[][] rows) {
        String[] headers = {"TXN ID", "DATE/TIME", "TYPE", "AMOUNT", "BALANCE"};
        int[] minWidths = {7, 19, 10, 11, 9};
        int[] widths = TableFormatter.columnWidths(headers, rows, minWidths);
        String rowFormat = TableFormatter.buildRowFormat(widths);
        String divider = TableFormatter.buildDivider(widths);

        System.out.println(divider);
        System.out.printf(rowFormat, (Object[]) headers);
        System.out.println(divider);
        for (String[] row : rows) {
            System.out.printf(rowFormat, (Object[]) row);
        }
        System.out.println(divider);
    }

    /** @return the total amount deposited into the given account */
    public double calculateTotalDeposits(String accountNumber) {
        return this.transactions.stream()
                .filter(t -> t.getAccountNumber().equals(accountNumber) && t.getType().equalsIgnoreCase("deposit"))
                .mapToDouble(Transaction::getAmount)
                .reduce(0.0, Double::sum);
    }

    /** @return the total amount withdrawn from the given account */
    public double calculateTotalWithdrawals(String accountNumber) {
        return this.transactions.stream()
                .filter(t -> t.getAccountNumber().equals(accountNumber) && t.getType().equalsIgnoreCase("withdrawal"))
                .mapToDouble(Transaction::getAmount)
                .reduce(0.0, Double::sum);
    }

    /** @return the total number of transactions recorded across all accounts */
    public int getTransactionCount() {
        return transactions.size();
    }

    /** @return every recorded transaction across all accounts, in the order they were recorded */
    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    /** @return the number of recorded transactions belonging to the given account */
    public int getTransactionCountByAccount(String accountNumber) {
        return (int) this.transactions.stream()
                .filter(t -> t.getAccountNumber().equals(accountNumber))
                .count();
    }

    /** @return the transactions belonging to the given account, newest first */
    public Transaction[] getTransactionsForAccount(String accountNumber) {
        return newestFirstForAccount(accountNumber).toArray(Transaction[]::new);
    }

    /** @return every transaction belonging to the given account, newest first */
    private List<Transaction> newestFirstForAccount(String accountNumber) {
        List<Transaction> matches = transactions.stream()
                .filter(t -> t.getAccountNumber().equals(accountNumber))
                .collect(Collectors.toList());
        Collections.reverse(matches);
        return matches;
    }

    /**
     * @param descending true for highest amount first, false for lowest amount first
     * @return every recorded transaction across all accounts, sorted by amount
     */
    public List<Transaction> getTransactionsSortedByAmount(boolean descending) {
        Comparator<Transaction> byAmount = Comparator.comparingDouble(Transaction::getAmount);
        return transactions.stream()
                .sorted(descending ? byAmount.reversed() : byAmount)
                .collect(Collectors.toList());
    }

    /**
     * @param descending true for most recent first, false for oldest first
     * @return every recorded transaction across all accounts, sorted by timestamp
     */
    public List<Transaction> getTransactionsSortedByDate(boolean descending) {
        Comparator<Transaction> byDate = Comparator.comparing(Transaction::getCreatedAt);
        return transactions.stream()
                .sorted(descending ? byDate.reversed() : byDate)
                .collect(Collectors.toList());
    }
}
