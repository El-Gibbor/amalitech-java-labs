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
    protected void displayTypeSummaryLine() {
        System.out.printf("         | Overdraft Limit: $%,.2f | Monthly Fee: $%,.2f%n",
                overdraftLimit, monthlyFee);
    }

    /** @return true if the withdrawal succeeded, false if amount was invalid or exceeded the overdraft limit */
    @Override
    public boolean withdraw(double amount) {
        if (!isValidAmount(amount) || exceedsOverdraftLimit(amount)) {
            return false;
        }
        setBalance(getBalance() - amount);
        return true;
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
