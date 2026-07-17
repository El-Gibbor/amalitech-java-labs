/** Thrown when a checking account withdrawal exceeds the overdraft limit. */
public class OverdraftExceededException extends Exception {
    public OverdraftExceededException(String message) {
        super(message);
    }
}
