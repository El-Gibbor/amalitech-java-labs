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

    /**
     * @throws InvalidAmountException if amount is not greater than zero
     */
    public void deposit(double amount) throws InvalidAmountException {
        if (!isValidAmount(amount)) {
            throw new InvalidAmountException("Deposit amount must be greater than 0.");
        }
        setBalance(getBalance() + amount);
    }

    /**
     * @throws InvalidAmountException if amount is not greater than zero
     * @throws InsufficientFundsException if amount exceeds the current balance
     * @throws OverdraftExceededException reserved for subclasses that allow overdraft
     */
    public void withdraw(double amount)
            throws InvalidAmountException, InsufficientFundsException, OverdraftExceededException {
        if (!isValidAmount(amount)) {
            throw new InvalidAmountException("Withdrawal amount must be greater than 0.");
        }
        if (amount > getBalance()) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Current balance: " + getBalance());
        }
        setBalance(getBalance() - amount);
    }

    /** Dispatches to {@link #deposit(double)} or {@link #withdraw(double)} by type. */
    @Override
    public void processTransaction(double amount, String type)
            throws InvalidAmountException, InsufficientFundsException, OverdraftExceededException {
        if ("deposit".equalsIgnoreCase(type)) {
            deposit(amount);
        } else if ("withdrawal".equalsIgnoreCase(type)) {
            withdraw(amount);
        }
    }
}
