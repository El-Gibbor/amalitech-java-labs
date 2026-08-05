package utils;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Centralizes the regex patterns used to validate console input formats. */
public final class ValidationUtils {
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("ACC\\d{3}");

    // Requires a dot in the domain, e.g. "example.com"; the spec's literal pattern didn't.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");

    // Optional leading '+', then digits, optionally hyphen-grouped, e.g. "+1-555-0101".
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]+(-[0-9]+)*$");

    public static final Predicate<String> IS_VALID_ACCOUNT_NUMBER =
            input -> input != null && ACCOUNT_NUMBER_PATTERN.matcher(input).matches();

    public static final Predicate<String> IS_VALID_EMAIL =
            input -> input != null && EMAIL_PATTERN.matcher(input).matches();

    public static final Predicate<String> IS_VALID_PHONE_NUMBER =
            input -> input != null && PHONE_PATTERN.matcher(input).matches();

    private ValidationUtils() {
    }

    public static boolean isValidAccountNumber(String input) {
        return IS_VALID_ACCOUNT_NUMBER.test(input);
    }

    public static boolean isValidEmail(String input) {
        return IS_VALID_EMAIL.test(input);
    }

    public static boolean isValidPhoneNumber(String input) {
        return IS_VALID_PHONE_NUMBER.test(input);
    }
}
