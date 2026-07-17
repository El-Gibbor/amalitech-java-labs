public abstract class Account implements Transactable {
    private String accountNumber;
    private Customer customer;
    private double balance;
    private String status;

    private static int accountCounter = 0;

    public Account(Customer customer, double balance) {
        this.accountNumber = String.format("ACC%03d", ++accountCounter);
        this.customer = customer;
        this.balance = balance;
        this.status = "Active"; 
    }

    public String getAccountNumber() {
        return accountNumber;
    }

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

    // protected: subclasses may set balance directly (e.g. overdraft)
    protected void setBalance(double balance) {
        this.balance = balance;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public abstract void displayAccountDetails();
    public abstract String getAccountType();

    // Subclass prints its own type-specific summary sub-line for the listing table
    protected abstract void displayTypeSummaryLine();

    /**
     * @throws InvalidAmountException if amount is not greater than zero
     */
    public void deposit(double amount) throws InvalidAmountException {
        if (amount <= 0) {
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
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than 0.");
        }
        if (amount > getBalance()) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Current balance: " + getBalance());
        }
        setBalance(getBalance() - amount);
    }

    // Implements transaction contract
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
