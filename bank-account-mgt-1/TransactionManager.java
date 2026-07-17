/** Stores and queries transactions for all accounts, backed by a fixed-size array. */
public class TransactionManager {
    // Fixed capacity of 200; transactionCount marks how many slots are used and the next free index
    private Transaction[] transactions = new Transaction[200];
    private int transactionCount = 0;

    /** @return true if the transaction was added, false if storage is full */
    public boolean addTransaction(Transaction transaction) {
        if (transactionCount < this.transactions.length) {
            this.transactions[transactionCount++] = transaction;
            return true;
        }
        return false;
    }

    /** Prints the transaction history for the given account, newest first. */
    public void viewTransactionsByAccount(String accountNumber) {
        int count = getTransactionCountByAccount(accountNumber);
        if (count == 0) {
            printNoTransactionsMessage();
            return;
        }
        String[][] rows = buildTransactionRows(accountNumber, count);
        printTransactionTable(rows);
    }

    private void printNoTransactionsMessage() {
        String message = "No transactions recorded for this account.";
        String line = "─".repeat(message.length());
        System.out.println(line);
        System.out.println(message);
        System.out.println(line);
    }

    // walk backwards so the newest transaction fills the first row
    private String[][] buildTransactionRows(String accountNumber, int count) {
        String[][] rows = new String[count][5];
        int r = 0;
        for (int i = transactionCount - 1; i >= 0; i--) {
            if (transactions[i].getAccountNumber().equals(accountNumber)) {
                rows[r] = formatTransactionRow(transactions[i]);
                r++;
            }
        }
        return rows;
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
        double totalDeposits = 0;
        for (int i = 0; i < transactionCount; i++) {
            if (transactions[i].getAccountNumber().equals(accountNumber)
                    && transactions[i].getType().equalsIgnoreCase("deposit")) {
                totalDeposits += transactions[i].getAmount();
            }
        }
        return totalDeposits;
    }

    /** @return the total amount withdrawn from the given account */
    public double calculateTotalWithdrawals(String accountNumber) {
        double totalWithdrawals = 0;
        for (int i = 0; i < transactionCount; i++) {
            if (transactions[i].getAccountNumber().equals(accountNumber)
                    && transactions[i].getType().equalsIgnoreCase("withdrawal")) {
                totalWithdrawals += transactions[i].getAmount();
            }
        }
        return totalWithdrawals;
    }

    /** @return the total number of transactions recorded across all accounts */
    public int getTransactionCount() {
        return transactionCount;
    }

    /** @return the number of recorded transactions belonging to the given account */
    public int getTransactionCountByAccount(String accountNumber) {
        int count = 0;
        for (int i = 0; i < transactionCount; i++) {
            if (transactions[i].getAccountNumber().equals(accountNumber)) {
                count++;
            }
        }
        return count;
    }

    /** @return the transactions belonging to the given account, newest first */
    public Transaction[] getTransactionsForAccount(String accountNumber) {
        Transaction[] result = new Transaction[getTransactionCountByAccount(accountNumber)];
        int r = 0;
        for (int i = transactionCount - 1; i >= 0; i--) {
            if (transactions[i].getAccountNumber().equals(accountNumber)) {
                result[r] = transactions[i];
                r++;
            }
        }
        return result;
    }
}
