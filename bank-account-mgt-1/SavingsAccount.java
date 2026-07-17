public class SavingsAccount extends Account {
    private double interestRate;
    private double minimumBalance;

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

    // reject if min balance is not met, then let Account do the subtraction
    @Override
    public boolean withdraw(double amount) {
        if (getBalance() - amount < minimumBalance) {
            return false;
        }
        return super.withdraw(amount);
    }

    public double calculateInterest() {
        return getBalance() * interestRate;     
    }
}