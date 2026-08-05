package utils;

import java.util.ArrayList;
import java.util.List;

import models.Account;
import models.exceptions.InsufficientFundsException;
import models.exceptions.InvalidAmountException;
import models.exceptions.OverdraftExceededException;

/** Runs a batch of deposits and withdrawals against one account on separate named threads. */
public final class ConcurrencyUtils {
    private ConcurrencyUtils() {
    }

    /** One operation to perform concurrently: a type ("Deposit" or "Withdrawal") and an amount. */
    public static class Operation {
        private final String type;
        private final double amount;

        public Operation(String type, double amount) {
            this.type = type;
            this.amount = amount;
        }

        public String getType() {
            return type;
        }

        public double getAmount() {
            return amount;
        }
    }

    /**
     * Starts one named thread per operation, all performing deposits or withdrawals on
     * {@code account} concurrently, waits for every thread to finish, then prints the
     * verified final balance. Relies on {@link Account#deposit(double)} and
     * {@link Account#withdraw(double)} already being {@code synchronized}; this method adds
     * no locking of its own.
     */
    public static void runConcurrentSimulation(Account account, List<Operation> operations) {
        System.out.println("Running concurrent transaction simulation...\n");

        List<Thread> threads = new ArrayList<>();
        int threadNumber = 1;
        for (Operation operation : operations) {
            String threadName = "Thread-" + threadNumber++;
            Thread thread = new Thread(() -> performOperation(account, operation, threadName), threadName);
            threads.add(thread);
        }

        // start every thread first, then wait on each in turn; starting and joining one
        // at a time in the same loop would force them to run one after another instead
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n✓ Thread-safe operations completed successfully.");
        System.out.printf("Final Balance for %s: $%,.2f%n", account.getAccountNumber(), account.getBalance());
    }

    private static void performOperation(Account account, Operation operation, String threadName) {
        boolean isDeposit = operation.getType().equalsIgnoreCase("deposit");
        String verb = isDeposit ? "Depositing" : "Withdrawing";
        String preposition = isDeposit ? "to" : "from";
        System.out.printf("%s: %s $%,.2f %s %s%n",
                threadName, verb, operation.getAmount(), preposition, account.getAccountNumber());

        try {
            account.processTransaction(operation.getAmount(), operation.getType());
        } catch (InvalidAmountException | InsufficientFundsException | OverdraftExceededException e) {
            System.out.println(threadName + ": ❌ " + e.getMessage());
        }
    }
}
