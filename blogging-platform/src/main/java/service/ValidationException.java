package service;

/** Thrown when input fails a business rule before it ever reaches the database. */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
