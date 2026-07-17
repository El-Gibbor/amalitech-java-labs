/** Thrown when a deposit or withdrawal amount is not greater than zero. */
public class InvalidAmountException extends Exception {
    public InvalidAmountException(String message) {
        super(message);
    }
}
