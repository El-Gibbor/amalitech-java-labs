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

    /**
     * @throws InvalidAmountException if amount is not greater than zero
     * @throws InsufficientFundsException if amount would breach the minimum balance, or
     *         otherwise exceeds the current balance
     * @throws OverdraftExceededException never thrown here; declared only because
     *         super.withdraw's signature reserves it
     */
    @Override
    public void withdraw(double amount)
            throws InvalidAmountException, InsufficientFundsException, OverdraftExceededException {
        if (wouldBreachMinimumBalance(amount)) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Withdrawal would breach the minimum balance of " + minimumBalance);
        }
        super.withdraw(amount);
    }

    private boolean wouldBreachMinimumBalance(double amount) {
        return getBalance() - amount < minimumBalance;
    }

    /** @return the interest earned on the current balance at this account's rate */
    public double calculateInterest() {
        return getBalance() * interestRate;
    }
}
