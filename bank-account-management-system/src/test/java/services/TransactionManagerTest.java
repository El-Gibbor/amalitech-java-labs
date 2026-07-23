package services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import models.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionManagerTest {
    private TransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transactionManager = new TransactionManager();
    }

    @Test
    void addTransactionIncreasesTransactionCount() {
        transactionManager.addTransaction(new Transaction("ACC001", "Deposit", 500, 500));
        assertEquals(1, transactionManager.getTransactionCount());
    }

    @Test
    void getTransactionsForAccountReturnsNewestFirst() {
        Transaction first = new Transaction("ACC001", "Deposit", 500, 500);
        Transaction second = new Transaction("ACC001", "Withdrawal", 100, 400);
        transactionManager.addTransaction(first);
        transactionManager.addTransaction(second);

        Transaction[] history = transactionManager.getTransactionsForAccount("ACC001");

        assertEquals(second.getTransactionId(), history[0].getTransactionId());
        assertEquals(first.getTransactionId(), history[1].getTransactionId());
    }

    @Test
    void calculateTotalDepositsSumsOnlyDeposits() {
        transactionManager.addTransaction(new Transaction("ACC001", "Deposit", 500, 500));
        transactionManager.addTransaction(new Transaction("ACC001", "Withdrawal", 100, 400));

        assertEquals(500, transactionManager.calculateTotalDeposits("ACC001"));
    }

    @Test
    void calculateTotalWithdrawalsSumsOnlyWithdrawals() {
        transactionManager.addTransaction(new Transaction("ACC001", "Deposit", 500, 500));
        transactionManager.addTransaction(new Transaction("ACC001", "Withdrawal", 100, 400));

        assertEquals(100, transactionManager.calculateTotalWithdrawals("ACC001"));
    }

    @Test
    void calculateTotalInterestSumsOnlyInterest() {
        transactionManager.addTransaction(new Transaction("ACC001", "Deposit", 500, 500));
        transactionManager.addTransaction(new Transaction("ACC001", "Interest", 17.5, 517.5));

        assertEquals(17.5, transactionManager.calculateTotalInterest("ACC001"));
    }

    @Test
    void getTransactionCountByAccountFiltersByAccount() {
        transactionManager.addTransaction(new Transaction("ACC001", "Deposit", 500, 500));
        transactionManager.addTransaction(new Transaction("ACC002", "Deposit", 300, 300));

        assertEquals(1, transactionManager.getTransactionCountByAccount("ACC001"));
    }
}
