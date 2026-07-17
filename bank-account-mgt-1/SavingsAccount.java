/**
 * A savings account that earns interest and enforces a minimum balance
 * below which withdrawals are refused.
 */
public class SavingsAccount extends Account {
    private double interestRate;
    private double minimumBalance;

    /** Creates a savings account with the standard interest rate and minimum balance. */
    public SavingsAccount(Customer customer, double balance) {
        super(customer, balance);
        this.interestRate = 0.035;
        this.minimumBalance = 500;
    }

    @Override
    public void displayAccountDetails() {
        System.out.println("  Account Number: " + getAccountNumber());
        System.out.println("  Customer: " + getCustomer().getName() + " (" + getCustomer().getCustomerType() + ")");
        System.out.println("  Account Type: " + getAccountType());
        System.out.printf("  Balance: $%,.2f%n", getBalance());
        System.out.printf("  Interest Rate: %.1f%%%n", interestRate * 100);
        System.out.printf("  Minimum Balance: $%,.2f%n", minimumBalance);
        System.out.println("  Status: " + getStatus());
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }

    @Override
    protected void displayTypeSummaryLine() {
        System.out.printf("         | Interest Rate: %.1f%% | Min Balance: $%,.2f%n",
                interestRate * 100, minimumBalance);
    }

    /** @return true if the withdrawal succeeded, false if amount was invalid or would breach the minimum balance */
    @Override
    public boolean withdraw(double amount) {
        if (wouldBreachMinimumBalance(amount)) {
            return false;
        }
        return super.withdraw(amount);
    }

    private boolean wouldBreachMinimumBalance(double amount) {
        return getBalance() - amount < minimumBalance;
    }

    /** @return the interest earned on the current balance at this account's rate */
    public double calculateInterest() {
        return getBalance() * interestRate;
    }
}
