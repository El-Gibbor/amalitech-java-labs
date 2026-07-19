package services;

import models.Account;
import models.exceptions.InvalidAccountException;
import utils.TableFormatter;

/** Stores accounts and provides lookup, listing, and aggregate queries over them. */
public class AccountManager {
    // Fixed capacity of 50; accountCount marks how many slots are used and the next free index
    private Account[] accounts = new Account[50];
    private int accountCount = 0;

    /** @return true if the account was added, false if storage is full */
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

    /** Prints a formatted listing of every stored account. */
    public void viewAllAccounts() {
        if (accountCount == 0) {
            System.out.println("No accounts available.");
            return;
        }
        String[][] rows = buildAccountRows();
        printAccountTable(rows);
    }

    private String[][] buildAccountRows() {
        String[][] rows = new String[accountCount][5];
        for (int i = 0; i < accountCount; i++) {
            rows[i] = formatAccountRow(accounts[i]);
        }
        return rows;
    }

    private String[] formatAccountRow(Account a) {
        return new String[] {
                a.getAccountNumber(),
                a.getCustomer().getName(),
                a.getAccountType(),
                String.format("$%,.2f", a.getBalance()),
                a.getStatus()
        };
    }

    private void printAccountTable(String[][] rows) {
        String[] headers = {"ACC NO", "CUSTOMER NAME", "TYPE", "BALANCE", "STATUS"};
        int[] minWidths = {8, 17, 17, 12, 6};
        int[] widths = TableFormatter.columnWidths(headers, rows, minWidths);
        String rowFormat = TableFormatter.buildRowFormat(widths);
        String divider = TableFormatter.buildDivider(widths);

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

    /** @return the sum of every stored account's balance */
    public double getTotalBalance() {
        double totalBalance = 0;
        for (int i = 0; i < accountCount; i++) {
            totalBalance += accounts[i].getBalance();
        }
        return totalBalance;
    }

    /** @return the number of accounts currently stored */
    public int getAccountCount() {
        return accountCount;
    }
}
