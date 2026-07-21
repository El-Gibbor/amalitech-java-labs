package models.exceptions;

/** Thrown when a given account number does not match any stored account. */
public class InvalidAccountException extends Exception {
    public InvalidAccountException(String message) {
        super(message);
    }
}
