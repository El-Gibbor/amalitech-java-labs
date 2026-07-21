package models.exceptions;

/** Thrown when a withdrawal exceeds the funds available in the account. */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
