package models;

import models.exceptions.InvalidAmountException;
import models.exceptions.OverdraftExceededException;

/**
 * A checking account with no interest, an overdraft allowance up to a limit,
 * and a monthly fee that is waived for customers with waived fees.
 */
public class CheckingAccount extends Account {
    private double overdraftLimit;
    private double monthlyFee;

    /** Creates a checking account with the standard overdraft limit and monthly fee. */
    public CheckingAccount(Customer customer, double balance) {
        super(customer, balance);
        this.overdraftLimit = 1000;
        this.monthlyFee = 10;
    }

    @Override
    public void displayAccountDetails() {
        System.out.println("  Account Number: " + getAccountNumber());
        System.out.println("  Customer: " + getCustomer().getName() + " (" + getCustomer().getCustomerType() + ")");
        System.out.println("  Account Type: " + getAccountType());
        System.out.printf("  Balance: $%,.2f%n", getBalance());
        System.out.printf("  Overdraft Limit: $%,.2f%n", overdraftLimit);
        if (getCustomer().hasWaivedFees()) {
            System.out.println("  Monthly Fee: $0.00 (WAIVED - Premium Customer)");
        } else {
            System.out.printf("  Monthly Fee: $%,.2f%n", monthlyFee);
        }
        System.out.println("  Status: " + getStatus());
    }

    @Override
    public String getAccountType() {
        return "Checking";
    }

    @Override
    public void displayTypeSummaryLine() {
        System.out.printf("         | Overdraft Limit: $%,.2f | Monthly Fee: $%,.2f%n",
                overdraftLimit, monthlyFee);
    }

    /**
     * @throws InvalidAmountException if amount is not greater than zero
     * @throws OverdraftExceededException if amount exceeds the balance plus the overdraft limit
     */
    @Override
    public void withdraw(double amount) throws InvalidAmountException, OverdraftExceededException {
        if (!isValidAmount(amount)) {
            throw new InvalidAmountException("Withdrawal amount must be greater than 0.");
        }
        if (exceedsOverdraftLimit(amount)) {
            throw new OverdraftExceededException(
                    "Overdraft limit exceeded. Available to withdraw: " + (getBalance() + overdraftLimit));
        }
        setBalance(getBalance() - amount);
    }

    private boolean exceedsOverdraftLimit(double amount) {
        return amount > getBalance() + overdraftLimit;
    }

    /** @return the monthly fee charged, or 0 if waived for this customer */
    public double applyMonthlyFee() {
        if (getCustomer().hasWaivedFees()) {
            return 0;
        }
        setBalance(getBalance() - monthlyFee);
        return monthlyFee;
    }
}
