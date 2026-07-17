/** Base type for all bank accounts owned by a {@link Customer}. */
public abstract class Account implements Transactable {
    private String accountNumber;
    private Customer customer;
    private double balance;
    private String status;

    private static int accountCounter = 0;

    /** Creates an account with an auto-generated account number. */
    public Account(Customer customer, double balance) {
        this.accountNumber = String.format("ACC%03d", ++accountCounter);
        this.customer = customer;
        this.balance = balance;
        this.status = "Active";
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    /** @throws IllegalArgumentException if accountNumber is null or blank */
    public void setAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be null or empty.");
        }
        this.accountNumber = accountNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public double getBalance() {
        return balance;
    }

    /** Sets the balance directly, bypassing deposit/withdraw validation. */
    protected void setBalance(double balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /** Prints this account's full details to the console. */
    public abstract void displayAccountDetails();

    /** @return the account type name, e.g. "Savings" or "Checking" */
    public abstract String getAccountType();

    /** Prints this account's type-specific summary line in the listing table. */
    protected abstract void displayTypeSummaryLine();

    /** Shared validation for {@link #deposit(double)} and {@link #withdraw(double)}. */
    protected boolean isValidAmount(double amount) {
        return amount > 0;
    }

    /** @return true if the deposit succeeded, false if amount was invalid */
    public boolean deposit(double amount) {
        if (!isValidAmount(amount)) {
            return false;
        }
        setBalance(getBalance() + amount);
        return true;
    }

    /** @return true if the withdrawal succeeded, false if amount was invalid or exceeded balance */
    public boolean withdraw(double amount) {
        if (!isValidAmount(amount) || amount > getBalance()) {
            return false;
        }
        setBalance(getBalance() - amount);
        return true;
    }

    /** Dispatches to {@link #deposit(double)} or {@link #withdraw(double)} by type. */
    @Override
    public boolean processTransaction(double amount, String type) {
        if ("deposit".equalsIgnoreCase(type)) {
            return deposit(amount);
        } else if ("withdrawal".equalsIgnoreCase(type)) {
            return withdraw(amount);
        }
        return false;
    }
}
