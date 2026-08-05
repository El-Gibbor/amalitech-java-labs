package services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import models.Account;
import models.CheckingAccount;
import models.Customer;
import models.PremiumCustomer;
import models.RegularCustomer;
import models.SavingsAccount;
import models.Transaction;

/**
 * Saves and loads accounts and transactions as pipe-delimited text files, using the NIO
 * {@link Files} and {@link Paths} APIs. Each account line embeds its owning customer's
 * details, since the project stores only one file per record type, not a separate
 * customers file; account-type-specific defaults (interest rate, overdraft limit, and so
 * on) are not persisted, since {@link SavingsAccount} and {@link CheckingAccount} always
 * reconstruct them as the same fixed constants regardless.
 */
public class FilePersistenceService {
    private static final Path ACCOUNTS_FILE = Paths.get("data", "accounts.txt");
    private static final Path TRANSACTIONS_FILE = Paths.get("data", "transactions.txt");
    private static final String DELIMITER = "|";
    private static final String DELIMITER_REGEX = "\\|";

    /** Writes every stored account to {@code data/accounts.txt}, one line per account. */
    public void saveAccounts(AccountManager accountManager) {
        List<String> lines = accountManager.getAllAccounts().stream()
                .map(this::formatAccountLine)
                .collect(Collectors.toList());
        writeLines(ACCOUNTS_FILE, lines, "accounts");
    }

    /** Writes every recorded transaction to {@code data/transactions.txt}, one line per transaction. */
    public void saveTransactions(TransactionManager transactionManager) {
        List<String> lines = transactionManager.getAllTransactions().stream()
                .map(this::formatTransactionLine)
                .collect(Collectors.toList());
        writeLines(TRANSACTIONS_FILE, lines, "transactions");
    }

    private void writeLines(Path file, List<String> lines, String description) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
            System.out.println("[OK] " + lines.size() + " " + description + " saved to " + file);
        } catch (IOException e) {
            System.out.println("Could not save " + description + " to " + file + ": " + e.getMessage());
        }
    }

    /**
     * Loads every account from {@code data/accounts.txt} into {@code accountManager}. If the
     * file does not exist yet, for example on the very first run, this does nothing rather
     * than failing.
     */
    public void loadAccounts(AccountManager accountManager) {
        if (!Files.exists(ACCOUNTS_FILE)) {
            System.out.println("No existing accounts file found at " + ACCOUNTS_FILE + "; starting empty.");
            return;
        }
        try (Stream<String> lines = Files.lines(ACCOUNTS_FILE, StandardCharsets.UTF_8)) {
            int[] count = {0};
            lines.filter(line -> !line.isBlank())
                    .map(this::parseAccountLine)
                    .forEach(account -> {
                        accountManager.addAccount(account);
                        count[0]++;
                    });
            System.out.println("[OK] " + count[0] + " accounts loaded from " + ACCOUNTS_FILE);
        } catch (IOException | RuntimeException e) {
            System.out.println("Could not load accounts from " + ACCOUNTS_FILE + ": " + e.getMessage());
        }
    }

    /**
     * Loads every transaction from {@code data/transactions.txt} into {@code transactionManager}.
     * If the file does not exist yet, this does nothing rather than failing.
     */
    public void loadTransactions(TransactionManager transactionManager) {
        if (!Files.exists(TRANSACTIONS_FILE)) {
            System.out.println("No existing transactions file found at " + TRANSACTIONS_FILE + "; starting empty.");
            return;
        }
        try (Stream<String> lines = Files.lines(TRANSACTIONS_FILE, StandardCharsets.UTF_8)) {
            int[] count = {0};
            lines.filter(line -> !line.isBlank())
                    .map(this::parseTransactionLine)
                    .forEach(transaction -> {
                        transactionManager.addTransaction(transaction);
                        count[0]++;
                    });
            System.out.println("[OK] " + count[0] + " transactions loaded from " + TRANSACTIONS_FILE);
        } catch (IOException | RuntimeException e) {
            System.out.println("Could not load transactions from " + TRANSACTIONS_FILE + ": " + e.getMessage());
        }
    }

    // accountType|accountNumber|balance|status|customerType|customerId|name|age|contact|address
    private String formatAccountLine(Account account) {
        Customer customer = account.getCustomer();
        return String.join(DELIMITER,
                account.getAccountType(),
                account.getAccountNumber(),
                String.valueOf(account.getBalance()),
                account.getStatus(),
                customer.getCustomerType(),
                customer.getCustomerId(),
                customer.getName(),
                String.valueOf(customer.getAge()),
                customer.getContact(),
                customer.getAddress());
    }

    private Account parseAccountLine(String line) {
        String[] f = line.split(DELIMITER_REGEX, -1);
        String accountType = f[0];
        String accountNumber = f[1];
        double balance = Double.parseDouble(f[2]);
        String status = f[3];
        String customerType = f[4];
        String customerId = f[5];
        String name = f[6];
        int age = Integer.parseInt(f[7]);
        String contact = f[8];
        String address = f[9];

        Customer customer;
        if ("Premium".equals(customerType)) {
            customer = new PremiumCustomer(customerId, name, age, contact, address);
        } else {
            customer = new RegularCustomer(customerId, name, age, contact, address);
        }
        Customer.ensureCounterAtLeast(numericSuffix(customerId));

        Account account;
        if ("Checking".equals(accountType)) {
            account = new CheckingAccount(accountNumber, customer, balance, status);
        } else {
            account = new SavingsAccount(accountNumber, customer, balance, status);
        }
        Account.ensureCounterAtLeast(numericSuffix(accountNumber));

        return account;
    }

    // transactionId|accountNumber|type|amount|balanceAfter|createdAt(ISO-8601)
    private String formatTransactionLine(Transaction transaction) {
        return String.join(DELIMITER,
                transaction.getTransactionId(),
                transaction.getAccountNumber(),
                transaction.getType(),
                String.valueOf(transaction.getAmount()),
                String.valueOf(transaction.getBalanceAfter()),
                transaction.getCreatedAt().toString());
    }

    private Transaction parseTransactionLine(String line) {
        String[] f = line.split(DELIMITER_REGEX, -1);
        String transactionId = f[0];
        String accountNumber = f[1];
        String type = f[2];
        double amount = Double.parseDouble(f[3]);
        double balanceAfter = Double.parseDouble(f[4]);
        LocalDateTime createdAt = LocalDateTime.parse(f[5]);

        Transaction.ensureCounterAtLeast(numericSuffix(transactionId));
        return new Transaction(transactionId, accountNumber, type, amount, balanceAfter, createdAt);
    }

    // strips a 3-letter prefix such as "ACC" or "TXN", leaving the numeric sequence value
    private static int numericSuffix(String id) {
        return Integer.parseInt(id.substring(3));
    }
}
