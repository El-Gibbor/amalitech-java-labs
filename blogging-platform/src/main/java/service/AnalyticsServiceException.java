package service;

/**
 * Wraps a lower level persistence failure, a SQLException, behind a message a
 * Controller can show directly, without the Controller ever needing to know
 * SQL or JDBC exist.
 */
public class AnalyticsServiceException extends RuntimeException {
    public AnalyticsServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
