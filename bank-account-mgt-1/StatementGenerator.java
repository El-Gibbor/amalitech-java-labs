/**
 * Builds and prints an account statement, combining an account's current
 * state with its transaction history from a {@link TransactionManager}.
 */
public class StatementGenerator {
    private final TransactionManager transactionManager;

    public StatementGenerator(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /** Prints a formatted statement for the given account. */
    public void generateStatement(Account account) {
        System.out.println(
                "\nAccount: " + account.getCustomer().getName() + " (" + account.getAccountType() + ")");
        System.out.printf("Current Balance: $%,.2f%n", account.getBalance());

        System.out.println("\nTransactions:");
        Transaction[] transactions = transactionManager.getTransactionsForAccount(account.getAccountNumber());
        if (transactions.length == 0) {
            printNoTransactionsMessage();
        } else {
            printTransactionTable(transactions);
            printNetChange(account.getAccountNumber());
        }

        System.out.println("\n✓ Statement generated successfully.");
    }

    private void printNoTransactionsMessage() {
        String message = "No transactions recorded for this account.";
        String line = "─".repeat(message.length());
        System.out.println(line);
        System.out.println(message);
        System.out.println(line);
    }

    private void printTransactionTable(Transaction[] transactions) {
        String[] headers = {"TXN ID", "TYPE", "AMOUNT", "BALANCE"};
        int[] minWidths = {6, 10, 11, 9};
        String[][] rows = new String[transactions.length][headers.length];
        for (int i = 0; i < transactions.length; i++) {
            rows[i] = formatStatementRow(transactions[i]);
        }

        int[] widths = TableFormatter.columnWidths(headers, rows, minWidths);
        String rowFormat = TableFormatter.buildRowFormat(widths);
        String divider = TableFormatter.buildDivider(widths);

        System.out.println(divider);
        for (String[] row : rows) {
            System.out.printf(rowFormat, (Object[]) row);
        }
        System.out.println(divider);
    }

    private String[] formatStatementRow(Transaction t) {
        String sign;
        if (t.getType().equalsIgnoreCase("deposit")) {
            sign = "+";
        } else {
            sign = "-";
        }
        return new String[] {
                t.getTransactionId(),
                t.getType().toUpperCase(),
                String.format("%s$%,.2f", sign, t.getAmount()),
                String.format("$%,.2f", t.getBalanceAfter())
        };
    }

    private void printNetChange(String accountNumber) {
        double totalDeposits = transactionManager.calculateTotalDeposits(accountNumber);
        double totalWithdrawals = transactionManager.calculateTotalWithdrawals(accountNumber);
        double netChange = totalDeposits - totalWithdrawals;
        String sign;
        if (netChange >= 0) {
            sign = "+";
        } else {
            sign = "-";
        }
        System.out.printf("Net Change: %s$%,.2f%n", sign, Math.abs(netChange));
    }
}
