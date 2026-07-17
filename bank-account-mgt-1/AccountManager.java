public class AccountManager {
    // Fixed capacity of 50; accountCount marks how many slots are used and the next free index
    private Account[] accounts = new Account[50];
    private int accountCount = 0;

    public boolean addAccount(Account account) {
        if (accountCount < this.accounts.length) {
            this.accounts[accountCount++] = account;
            return true;
        }
        return false;
    }

    // Linear search over the used portion
    /**
     * @throws InvalidAccountException if no account matches accountNumber
     */
    public Account findAccount(String accountNumber) throws InvalidAccountException {
        for (int i = 0; i < accountCount; i++) {
            if (accounts[i].getAccountNumber().equals(accountNumber)) {
                return accounts[i];
            }
        }
        throw new InvalidAccountException("Account not found: " + accountNumber);
    }

    public void viewAllAccounts() {
        if (accountCount == 0) {
            System.out.println("No accounts available.");
            return;
        }

        String[] headers = {"ACC NO", "CUSTOMER NAME", "TYPE", "BALANCE", "STATUS"};
        int[] minWidths = {8, 17, 17, 12, 6};

        // Collect each row's cell text so columns can be sized to fit the content
        String[][] rows = new String[accountCount][headers.length];
        for (int i = 0; i < accountCount; i++) {
            Account a = accounts[i];
            rows[i][0] = a.getAccountNumber();
            rows[i][1] = a.getCustomer().getName();
            rows[i][2] = a.getAccountType();
            rows[i][3] = String.format("$%,.2f", a.getBalance());
            rows[i][4] = a.getStatus();
        }

        int[] widths = columnWidths(headers, rows, minWidths);
        String rowFormat = buildRowFormat(widths);
        String divider = buildDivider(widths);

        System.out.println("\nACCOUNT LISTING");
        System.out.println(divider);
        System.out.printf(rowFormat, (Object[]) headers);
        System.out.println(divider);
        for (int i = 0; i < accountCount; i++) {
            System.out.printf(rowFormat, (Object[]) rows[i]);
            accounts[i].displayTypeSummaryLine();
            System.out.println(divider);
        }
    }

    public double getTotalBalance() {
        double totalBalance = 0;
        for (int i = 0; i < accountCount; i++) {
            totalBalance += accounts[i].getBalance();
        }
        return totalBalance;
    }

    public int getAccountCount() {
        return accountCount;
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

    // Builds a left-justified printf format such as "%-8s | %-17s | ... %n"
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
