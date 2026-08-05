package services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Account;
import models.exceptions.InsufficientFundsException;
import models.exceptions.InvalidAccountException;
import models.exceptions.InvalidAmountException;
import models.exceptions.OverdraftExceededException;
import utils.TableFormatter;

/** Stores accounts and provides lookup, listing, and aggregate queries over them. */
public class AccountManager {
    private Map<String, Account> accountsMap = new HashMap<>();

    /** @return true if the account was added, false if an account with the same account number already exists */
    public boolean addAccount(Account account) {
        Account previous = accountsMap.put(account.getAccountNumber(), account);
        return previous == null;
    }

    /**
     * @throws InvalidAccountException if no account matches accountNumber
     */
    public Account findAccount(String accountNumber) throws InvalidAccountException {
        Account account = accountsMap.get(accountNumber);
        if (account == null) {
            throw new InvalidAccountException("Account not found: " + accountNumber);
        }
        return account;
    }

    /**
     * Moves {@code amount} from the account identified by {@code fromAccountNumber} to the
     * account identified by {@code toAccountNumber}, subject to the same validation each
     * account already applies to a standalone {@link Account#withdraw(double)} and
     * {@link Account#deposit(double)}.
     *
     * @throws InvalidAccountException if either account number does not match a stored
     *         account, or if the two account numbers are the same
     * @throws InvalidAmountException if amount is not greater than zero
     * @throws InsufficientFundsException if the source account has insufficient funds, or
     *         (for a savings account) the withdrawal would breach its minimum balance
     * @throws OverdraftExceededException if the source account's overdraft limit would be
     *         exceeded
     */
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount)
            throws InvalidAccountException, InvalidAmountException, InsufficientFundsException,
            OverdraftExceededException {
        if (fromAccountNumber.equals(toAccountNumber)) {
            throw new InvalidAccountException("Cannot transfer to the same account: " + fromAccountNumber);
        }

        Account fromAccount = findAccount(fromAccountNumber);
        Account toAccount = findAccount(toAccountNumber);

        // withdraw() only returns normally once amount has passed isValidAmount, so the
        // deposit() below is guaranteed not to fail validation in turn.
        fromAccount.withdraw(amount);
        toAccount.deposit(amount);
    }

    /** Prints a formatted listing of every stored account. */
    public void viewAllAccounts() {
        if (accountsMap.isEmpty()) {
            System.out.println("No accounts available.");
            return;
        }
        String[][] rows = buildAccountRows();
        printAccountTable(rows);
    }

    private String[][] buildAccountRows() {
        String[][] rows = new String[accountsMap.size()][5];
        int index = 0;
        for (Account account : accountsMap.values()) {
            rows[index++] = formatAccountRow(account);
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
        int index = 0;
        for (Account account : accountsMap.values()) {
            System.out.printf(rowFormat, (Object[]) rows[index]);
            account.displayTypeSummaryLine();
            System.out.println(divider);
            index++;
        }
    }

    /** @return the sum of every stored account's balance */
    public double getTotalBalance() {
        return accountsMap.values().stream()
                .mapToDouble(Account::getBalance)
                .sum();
    }

    /** @return the number of accounts currently stored */
    public int getAccountCount() {
        return accountsMap.size();
    }

    /** @return every stored account, in no particular order */
    public List<Account> getAllAccounts() {
        return new ArrayList<>(accountsMap.values());
    }
}
