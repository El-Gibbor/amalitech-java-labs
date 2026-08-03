import java.util.Scanner;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import models.Account;
import models.CheckingAccount;
import models.Customer;
import models.PremiumCustomer;
import models.RegularCustomer;
import models.SavingsAccount;
import models.Transaction;
import models.exceptions.InsufficientFundsException;
import models.exceptions.InvalidAccountException;
import models.exceptions.InvalidAmountException;
import models.exceptions.OverdraftExceededException;
import services.AccountManager;
import services.StatementGenerator;
import services.TransactionManager;

public class Main {
    public static void main(String[] args) {
        AccountManager accountManager = new AccountManager();
        TransactionManager transactionManager = new TransactionManager();
        StatementGenerator statementGenerator = new StatementGenerator(transactionManager);

        // Seed starter accounts so View Accounts has data on first launch
        seedAccounts(accountManager);

        String title = "BANK ACCOUNT MANAGEMENT SYSTEM";
        int width = title.length() + 6; // padding on each side
        String topBottom = "═".repeat(width - 2);

        Scanner scanner = new Scanner(System.in);
        boolean programRunning = true;

        while (programRunning) {
            System.out.println("\n╔" + topBottom + "╗");
            System.out.println("║  " + title + "  ║");
            System.out.println("╚" + topBottom + "╝");

            System.out.println("\nMain Menu:");
            System.out.println("-----------");
            System.out.println("1. Manage Accounts");
            System.out.println("2. Perform Transactions");
            System.out.println("3. Generate Account Statements");
            System.out.println("4. Run Tests");
            System.out.println("5. Exit\n");

            int choice = readInt(scanner, "Enter your choice: ");

            switch (choice) {
                case 1:
                    manageAccountsMenu(scanner, accountManager);
                    break;
                case 2:
                    performTransactionsMenu(scanner, accountManager, transactionManager);
                    break;
                case 3:
                    generateAccountStatement(scanner, accountManager, statementGenerator);
                    break;
                case 4:
                    runTests(scanner);
                    break;
                case 5:
                    programRunning = false;
                    System.out.println("\nThank you for using Bank Account Management System!\n");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }

        scanner.close();
    }

    // Submenu: create or view accounts, looping until the user chooses to go back
    private static void manageAccountsMenu(Scanner scanner, AccountManager accountManager) {
        boolean inSubmenu = true;
        while (inSubmenu) {
            System.out.println("\nMANAGE ACCOUNTS");
            System.out.println("1. Create Account");
            System.out.println("2. View All Accounts");
            System.out.println("3. Back to Main Menu\n");

            int choice = readIntInRange(scanner, "Enter your choice: ", 1, 3);

            switch (choice) {
                case 1:
                    createAccount(scanner, accountManager);
                    break;
                case 2:
                    viewAllAccounts(scanner, accountManager);
                    break;
                case 3:
                    inSubmenu = false;
                    break;
            }
        }
    }

    private static void createAccount(Scanner scanner, AccountManager accountManager) {
        System.out.println("\nACCOUNT CREATION");
        System.out.println("─────────────────────────────────────────────\n");

        System.out.print("Enter customer name: ");
        String name = scanner.nextLine();

        int age = readIntInRange(scanner, "Enter customer age: ", 1, 120);

        System.out.print("Enter customer contact: ");
        String contact = scanner.nextLine();

        System.out.print("Enter customer address: ");
        String address = scanner.nextLine();

        System.out.println("\nCustomer type:");
        System.out.println("1. Regular Customer (Standard banking services)");
        System.out.println("2. Premium Customer (Enhanced benefits, min balance $10,000)");
        int customerType = readIntInRange(scanner, "\nSelect type (1-2): ", 1, 2);

        Customer customer;
        if (customerType == 2) {
            customer = new PremiumCustomer(name, age, contact, address);
        } else {
            customer = new RegularCustomer(name, age, contact, address);
        }

        System.out.println("\nAccount type:");
        System.out.println("1. Savings Account (Interest: 3.5%, Min Balance: $500)");
        System.out.println("2. Checking Account (Overdraft: $1,000, Monthly Fee: $10)");
        int accountType = readIntInRange(scanner, "\nSelect type (1-2): ", 1, 2);

        double initialDeposit = readPositiveDouble(scanner, "\nEnter initial deposit amount: $");

        Account account;
        if (accountType == 2) {
            account = new CheckingAccount(customer, initialDeposit);
        } else {
            account = new SavingsAccount(customer, initialDeposit);
        }

        boolean added = accountManager.addAccount(account);
        if (!added) {
            System.out.println("\nAn account with this account number already exists. Could not create account.");
            return;
        }

        System.out.println("\n[OK] Account created successfully!");
        account.displayAccountDetails();

        System.out.print("\nPress Enter to continue... ");
        scanner.nextLine();
    }

    private static void viewAllAccounts(Scanner scanner, AccountManager accountManager) {
        accountManager.viewAllAccounts();
        System.out.println("\nTotal Accounts: " + accountManager.getAccountCount());
        System.out.printf("Total Bank Balance: $%,.2f%n", accountManager.getTotalBalance());
        System.out.print("\nPress Enter to continue... ");
        scanner.nextLine();
    }

    // Submenu: process a transaction or view history, looping until the user chooses to go back
    private static void performTransactionsMenu(Scanner scanner, AccountManager accountManager,
            TransactionManager transactionManager) {
        boolean inSubmenu = true;
        while (inSubmenu) {
            System.out.println("\nPERFORM TRANSACTIONS");
            System.out.println("1. Process Transaction");
            System.out.println("2. View Transaction History");
            System.out.println("3. Back to Main Menu\n");

            int choice = readIntInRange(scanner, "Enter your choice: ", 1, 3);

            switch (choice) {
                case 1:
                    processTransaction(scanner, accountManager, transactionManager);
                    break;
                case 2:
                    viewTransactionHistory(scanner, accountManager, transactionManager);
                    break;
                case 3:
                    inSubmenu = false;
                    break;
            }
        }
    }

    private static void processTransaction(Scanner scanner, AccountManager accountManager,
            TransactionManager transactionManager) {
        System.out.println("\nPROCESS TRANSACTION");
        System.out.println("─────────────────────────────────────────────\n");

        System.out.print("Enter Account Number: ");
        String accountNumber = scanner.nextLine();

        try {
            Account account = accountManager.findAccount(accountNumber);

            System.out.println("\nAccount Details:");
            System.out.println("Customer: " + account.getCustomer().getName());
            System.out.println("Account Type: " + account.getAccountType());
            System.out.printf("Current Balance: $%,.2f%n", account.getBalance());

            System.out.println("\nTransaction type:");
            System.out.println("1. Deposit");
            System.out.println("2. Withdrawal");
            int transactionType = readIntInRange(scanner, "\nSelect type (1-2): ", 1, 2);

            String type;
            if (transactionType == 2) {
                type = "Withdrawal";
            } else {
                type = "Deposit";
            }

            double amount = readPositiveDouble(scanner, "\nEnter amount: $");

            double previousBalance = account.getBalance();
            account.processTransaction(amount, type);
            double newBalance = account.getBalance();

            Transaction transaction = new Transaction(account.getAccountNumber(), type, amount, newBalance);

            System.out.println("\nTRANSACTION CONFIRMATION");
            System.out.println("─────────────────────────────────────────────");
            System.out.println("Transaction ID: " + transaction.getTransactionId());
            System.out.println("Account: " + account.getAccountNumber());
            System.out.println("Type: " + type.toUpperCase());
            System.out.printf("Amount: $%,.2f%n", amount);
            System.out.printf("Previous Balance: $%,.2f%n", previousBalance);
            System.out.printf("New Balance: $%,.2f%n", newBalance);
            System.out.println("Date/Time: " + transaction.getTimestamp());
            System.out.println("─────────────────────────────────────────────");

            System.out.print("\nConfirm transaction? (Y/N): ");
            String confirm = scanner.nextLine();

            if (confirm.equalsIgnoreCase("Y")) {
                transactionManager.addTransaction(transaction);
                System.out.println("\n[OK] Transaction completed successfully!");
            } else {
                // undo the balance change; the transaction is not recorded
                if (type.equalsIgnoreCase("Deposit")) {
                    account.withdraw(amount);
                } else {
                    account.deposit(amount);
                }
                System.out.println("\nTransaction cancelled. No changes were saved.");
            }
        } catch (InvalidAccountException e) {
            System.out.println("\n❌ Error: " + e.getMessage());
        } catch (InvalidAmountException e) {
            System.out.println("\n❌ Error: " + e.getMessage());
        } catch (InsufficientFundsException e) {
            System.out.println("\n❌ Transaction Failed: " + e.getMessage());
        } catch (OverdraftExceededException e) {
            System.out.println("\n❌ Transaction Failed: " + e.getMessage());
        }

        System.out.print("\nPress Enter to continue... ");
        scanner.nextLine();
    }

    private static void viewTransactionHistory(Scanner scanner, AccountManager accountManager,
            TransactionManager transactionManager) {
        System.out.println("\nVIEW TRANSACTION HISTORY");
        System.out.println("─────────────────────────────────────────────\n");

        System.out.print("Enter Account Number: ");
        String accountNumber = scanner.nextLine();

        try {
            Account account = accountManager.findAccount(accountNumber);

            System.out.println(
                    "\nAccount: " + account.getAccountNumber() + " - " + account.getCustomer().getName());
            System.out.println("Account Type: " + account.getAccountType());
            System.out.printf("Current Balance: $%,.2f%n", account.getBalance());

            double totalDeposits = transactionManager.calculateTotalDeposits(account.getAccountNumber());
            double totalWithdrawals = transactionManager.calculateTotalWithdrawals(account.getAccountNumber());
            boolean hasTransactions = totalDeposits > 0 || totalWithdrawals > 0;

            if (!hasTransactions) {
                System.out.println();
                transactionManager.viewTransactionsByAccount(account.getAccountNumber());
            } else {
                System.out.println("\nTRANSACTION HISTORY");
                transactionManager.viewTransactionsByAccount(account.getAccountNumber());

                double netChange = totalDeposits - totalWithdrawals;
                String sign;
                if (netChange >= 0) {
                    sign = "+";
                } else {
                    sign = "-";
                }
                System.out.println();
                System.out.println("Total Transactions: "
                        + transactionManager.getTransactionCountByAccount(account.getAccountNumber()));
                System.out.printf("Total Deposits: $%,.2f%n", totalDeposits);
                System.out.printf("Total Withdrawals: $%,.2f%n", totalWithdrawals);
                System.out.printf("Net Change: %s$%,.2f%n", sign, Math.abs(netChange));
            }
        } catch (InvalidAccountException e) {
            System.out.println("\n❌ Error: " + e.getMessage());
        }

        System.out.print("\nPress Enter to continue... ");
        scanner.nextLine();
    }

    private static void generateAccountStatement(Scanner scanner, AccountManager accountManager,
            StatementGenerator statementGenerator) {
        System.out.println("\nGENERATE ACCOUNT STATEMENT");
        System.out.println("─────────────────────────────────────────────\n");

        System.out.print("Enter Account Number: ");
        String accountNumber = scanner.nextLine();

        try {
            Account account = accountManager.findAccount(accountNumber);
            statementGenerator.generateStatement(account);
        } catch (InvalidAccountException e) {
            System.out.println("\n❌ Error: " + e.getMessage());
        }

        System.out.print("\nPress Enter to continue... ");
        scanner.nextLine();
    }

    private static void runTests(Scanner scanner) {
        System.out.println("\nRUN TESTS");
        System.out.println("─────────────────────────────────────────────\n");
        System.out.println("Running tests with JUnit...\n");

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        DiscoverySelectors.selectClass("models.AccountTest"),
                        DiscoverySelectors.selectClass("services.TransactionManagerTest"),
                        DiscoverySelectors.selectClass("models.exceptions.ExceptionTest"))
                .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(new ConsoleTestListener(), summaryListener);
        launcher.execute(request);

        TestExecutionSummary summary = summaryListener.getSummary();
        long totalTests = summary.getTestsFoundCount();
        long passedTests = summary.getTestsSucceededCount();

        System.out.println();
        if (passedTests == totalTests) {
            System.out.println("[OK] All " + totalTests + " tests passed successfully!");
        } else {
            System.out.println("❌ " + passedTests + " of " + totalTests + " tests passed.");
        }

        System.out.print("\nPress Enter to continue... ");
        scanner.nextLine();
    }

    // Prints one line per test method as it finishes, matching the console's PASSED/FAILED format
    private static class ConsoleTestListener implements TestExecutionListener {
        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
            if (!testIdentifier.isTest()) {
                return; // skip containers (classes, the engine itself); only report leaf test methods
            }
            String outcome;
            if (result.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
                outcome = "PASSED";
            } else {
                outcome = "FAILED";
            }
            System.out.println("Test: " + testIdentifier.getDisplayName() + " .......... " + outcome);
        }
    }

    // re-prompts until the input is a valid integer (no crash on letters)
    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextInt()) {
                int value = scanner.nextInt();
                scanner.nextLine(); // consume the trailing newline
                return value;
            }
            scanner.nextLine(); // discard the invalid token
            System.out.println("Invalid input. Please enter a whole number.");
        }
    }

    // Reads an integer and re-prompts until it falls within [min, max]
    private static int readIntInRange(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            int value = readInt(scanner, prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.printf("Please enter a number between %d and %d.%n", min, max);
        }
    }

    // Reads a positive amount, rejecting non-numbers, zero or negative values
    private static double readPositiveDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            if (scanner.hasNextDouble()) {
                double value = scanner.nextDouble();
                scanner.nextLine();
                if (value > 0) {
                    return value;
                }
                System.out.println("Amount must be greater than 0.");
            } else {
                scanner.nextLine();
                System.out.println("Invalid input. Please enter a valid amount.");
            }
        }
    }

    // Bootstrap: five demo accounts (3 Savings, 2 Checking)
    private static void seedAccounts(AccountManager accountManager) {
        RegularCustomer customer1 = new RegularCustomer("John Smith", 35, "+1-555-0101", "12 Elm Street, Springfield");
        SavingsAccount account1 = new SavingsAccount(customer1, 5250);
        accountManager.addAccount(account1);

        RegularCustomer customer2 = new RegularCustomer("Sarah Johnson", 29, "+1-555-0102", "48 Oak Avenue, Springfield");
        CheckingAccount account2 = new CheckingAccount(customer2, 3450);
        accountManager.addAccount(account2);

        RegularCustomer customer3 = new RegularCustomer("Michael Chen", 41, "+1-555-0103", "7 Pine Road, Springfield");
        SavingsAccount account3 = new SavingsAccount(customer3, 15750);
        accountManager.addAccount(account3);

        RegularCustomer customer4 = new RegularCustomer("Emily Brown", 33, "+1-555-0104", "90 Maple Lane, Springfield");
        CheckingAccount account4 = new CheckingAccount(customer4, 890);
        accountManager.addAccount(account4);

        RegularCustomer customer5 = new RegularCustomer("David Wilson", 52, "+1-555-0105", "23 Birch Court, Springfield");
        SavingsAccount account5 = new SavingsAccount(customer5, 25300);
        accountManager.addAccount(account5);
    }

}
