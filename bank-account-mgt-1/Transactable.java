public interface Transactable {
    /**
     * @throws InvalidAmountException if amount is not greater than zero
     * @throws InsufficientFundsException if amount exceeds the current balance
     * @throws OverdraftExceededException if amount exceeds an allowed overdraft limit
     */
    void processTransaction(double amount, String type)
            throws InvalidAmountException, InsufficientFundsException, OverdraftExceededException;
}
