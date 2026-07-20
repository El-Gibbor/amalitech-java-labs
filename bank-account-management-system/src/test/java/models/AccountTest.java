package models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import models.exceptions.InsufficientFundsException;
import models.exceptions.InvalidAccountException;
import models.exceptions.InvalidAmountException;
import models.exceptions.OverdraftExceededException;
import services.AccountManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTest {
    private SavingsAccount account;
    private CheckingAccount checkingAccount;
    private AccountManager accountManager;

    @BeforeEach
    void setUp() {
        RegularCustomer customer = new RegularCustomer("Jane Doe", 30, "+1-555-0000", "1 Test Street");
        account = new SavingsAccount(customer, 1000);
        checkingAccount = new CheckingAccount(customer, 500);

        accountManager = new AccountManager();
        accountManager.addAccount(account);
        accountManager.addAccount(checkingAccount);
    }

    @Test
    void depositUpdatesBalance() throws InvalidAmountException {
        double startingBalance = account.getBalance();
        account.deposit(250);
        assertEquals(startingBalance + 250, account.getBalance());
    }

    @Test
    void withdrawBelowMinimumThrowsException() {
        assertThrows(InsufficientFundsException.class, () -> account.withdraw(600));
    }

    @Test
    void overdraftWithinLimitAllowed() throws InvalidAmountException, OverdraftExceededException {
        checkingAccount.withdraw(1200);
        assertEquals(-700, checkingAccount.getBalance());
    }

    @Test
    void overdraftExceedThrowsException() {
        assertThrows(OverdraftExceededException.class, () -> checkingAccount.withdraw(1600));
    }

    @Test
    void transferMovesBalanceBetweenAccounts()
            throws InvalidAccountException, InvalidAmountException, InsufficientFundsException,
            OverdraftExceededException {
        double fromStartingBalance = account.getBalance();
        double toStartingBalance = checkingAccount.getBalance();

        accountManager.transfer(account.getAccountNumber(), checkingAccount.getAccountNumber(), 300);

        assertEquals(fromStartingBalance - 300, account.getBalance());
        assertEquals(toStartingBalance + 300, checkingAccount.getBalance());
    }

    @Test
    void transferWithUnknownAccountThrowsException() {
        assertThrows(InvalidAccountException.class,
                () -> accountManager.transfer("ACC999", checkingAccount.getAccountNumber(), 300));
    }

    @Test
    void transferToSameAccountThrowsException() {
        assertThrows(InvalidAccountException.class,
                () -> accountManager.transfer(account.getAccountNumber(), account.getAccountNumber(), 300));
    }

    @Test
    void transferWithInsufficientFundsThrowsException() {
        // account is a SavingsAccount with balance 1000 and a $500 minimum;
        // withdrawing 600 would leave 400, breaching the minimum balance.
        assertThrows(InsufficientFundsException.class,
                () -> accountManager.transfer(account.getAccountNumber(), checkingAccount.getAccountNumber(), 600));
    }

    @Test
    void transferExceedingOverdraftThrowsException() {
        // checkingAccount is a CheckingAccount with balance 500 and a $1,000 overdraft limit,
        // so the most it can send is 1,500; used here as the source, account as destination.
        assertThrows(OverdraftExceededException.class,
                () -> accountManager.transfer(checkingAccount.getAccountNumber(), account.getAccountNumber(), 1600));
    }
}
