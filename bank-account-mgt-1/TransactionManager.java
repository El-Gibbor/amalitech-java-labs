public class TransactionManager {
    // Fixed capacity of 200; transactionCount marks how many slots are used and the next free index
    private Transaction[] transactions = new Transaction[200];
    private int transactionCount = 0;

    public boolean addTransaction(Transaction transaction) {
        if (transactionCount < this.transactions.length) {
            this.transactions[transactionCount++] = transaction;
            return true;
        }
        return false;
    }

    public void viewTransactionsByAccount(String accountNumber) {
        int count = 0;
        for (int i = 0; i < transactionCount; i++) {
            if (transactions[i].getAccountNumber().equals(accountNumber)) {
                count++;
            }
        }

        if (count == 0) {
            String message = "No transactions recorded for this account.";
            String line = "─".repeat(message.length());
            System.out.println(line);
            System.out.println(message);
            System.out.println(line);
            return;
        }

        String[] headers = {"TXN ID", "DATE/TIME", "TYPE", "AMOUNT", "BALANCE"};
        int[] minWidths = {7, 19, 10, 11, 9};
        String[][] rows = new String[count][headers.length];

        // walk backwards so the newest transaction fills the first row
        int r = 0;
        for (int i = transactionCount - 1; i >= 0; i--) {
            if (transactions[i].getAccountNumber().equals(accountNumber)) {
                Transaction t = transactions[i];
                String sign;
                if (t.getType().equalsIgnoreCase("deposit")) {
                    sign = "+";
                } else {
                    sign = "-";
                }
                rows[r][0] = t.getTransactionId();
                rows[r][1] = t.getTimestamp();
                rows[r][2] = t.getType().toUpperCase();
                rows[r][3] = String.format("%s$%,.2f", sign, t.getAmount());
                rows[r][4] = String.format("$%,.2f", t.getBalanceAfter());
                r++;
            }
        }

        int[] widths = columnWidths(headers, rows, minWidths);
        String rowFormat = buildRowFormat(widths);
        String divider = buildDivider(widths);

        System.out.println(divider);
        System.out.printf(rowFormat, (Object[]) headers);
        System.out.println(divider);
        for (String[] row : rows) {
            System.out.printf(rowFormat, (Object[]) row);
        }
        System.out.println(divider);
    }

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

    public int getTransactionCount() {
        return transactionCount;
    }

    // Count how many recorded transactions belong to a single account
    public int getTransactionCountByAccount(String accountNumber) {
        int count = 0;
        for (int i = 0; i < transactionCount; i++) {
            if (transactions[i].getAccountNumber().equals(accountNumber)) {
                count++;
            }
        }
        return count;
    }

    // Column width = the larger of its minimum and the widest cell (header or any row)
    private static int[] columnWidths(String[] headers, String[][] rows, int[] minWidths) {
        int[] widths = new int[headers.length];
        for (int c = 0; c < headers.length; c++) {
            widths[c] = Math.max(minWidths[c], headers[c].length());
            for (String[] row : rows) {
                if (row[c].length() > widths[c]) {
                    widths[c] = row[c].length();
                }
            }
        }
        return widths;
    }

    // Builds a left-justified printf format such as "%-7s | %-19s | ... %n"
    private static String buildRowFormat(int[] widths) {
        StringBuilder format = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            if (c > 0) {
                format.append(" | ");
            }
            format.append("%-").append(widths[c]).append("s");
        }
        format.append("%n");
        return format.toString();
    }

    // A horizontal rule as wide as the whole table (columns plus the " | " separators)
    private static String buildDivider(int[] widths) {
        int total = 0;
        for (int width : widths) {
            total += width;
        }
        total += 3 * (widths.length - 1);
        return "─".repeat(total);
    }
}
