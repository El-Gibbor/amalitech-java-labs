package models.exceptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import models.RegularCustomer;
import models.SavingsAccount;
import services.AccountManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExceptionTest {
    private SavingsAccount account;
    private AccountManager accountManager;

    @BeforeEach
    void setUp() {
        RegularCustomer customer = new RegularCustomer("Jane Doe", 30, "+1-555-0000", "1 Test Street");
        account = new SavingsAccount(customer, 1000);
        accountManager = new AccountManager();
    }

    @Test
    void invalidAmountExceptionStoresMessage() {
        InvalidAmountException exception = new InvalidAmountException("bad amount");
        assertEquals("bad amount", exception.getMessage());
    }

    @Test
    void insufficientFundsExceptionStoresMessage() {
        InsufficientFundsException exception = new InsufficientFundsException("not enough funds");
        assertEquals("not enough funds", exception.getMessage());
    }

    @Test
    void overdraftExceededExceptionStoresMessage() {
        OverdraftExceededException exception = new OverdraftExceededException("over the limit");
        assertEquals("over the limit", exception.getMessage());
    }

    @Test
    void invalidAccountExceptionStoresMessage() {
        InvalidAccountException exception = new InvalidAccountException("no such account");
        assertEquals("no such account", exception.getMessage());
    }

    @Test
    void customExceptionsAreCheckedNotUnchecked() {
        InvalidAmountException exception = new InvalidAmountException("x");
        assertTrue(exception instanceof Exception);
        assertFalse(RuntimeException.class.isAssignableFrom(exception.getClass()));
    }

    @Test
    void depositWithNonPositiveAmountThrowsInvalidAmountException() {
        assertThrows(InvalidAmountException.class, () -> account.deposit(0));
    }

    @Test
    void withdrawWithNonPositiveAmountThrowsInvalidAmountException() {
        assertThrows(InvalidAmountException.class, () -> account.withdraw(-50));
    }

    @Test
    void findAccountWithUnknownNumberThrowsInvalidAccountException() {
        assertThrows(InvalidAccountException.class, () -> accountManager.findAccount("ACC999"));
    }
}
