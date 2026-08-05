package utils;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Centralizes the regex patterns used to validate console input formats. */
public final class ValidationUtils {
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("ACC\\d{3}");

    // Bank-Account-III.md's literal pattern, ^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$, does not
    // actually require a dot in the domain, so it would accept "john.smith@bank" even though
    // the spec's own worked example shows that exact input being rejected. Tightened here so
    // the domain must end in at least one ".label", matching the spec's illustrated behavior.
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");

    // optional leading '+', then digits, optionally split into hyphen-separated groups,
    // e.g. "+1-555-0101" or "5550101"; Bank-Account-III.md does not specify an exact phone
    // pattern, so this is designed to accept the shape already used in the seed data
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]+(-[0-9]+)*$");

    /** True if the input matches ACC followed by exactly three digits, e.g. "ACC003". */
    public static final Predicate<String> IS_VALID_ACCOUNT_NUMBER =
            input -> input != null && ACCOUNT_NUMBER_PATTERN.matcher(input).matches();

    /** True if the input has the shape local-part@domain, e.g. "name@example.com". */
    public static final Predicate<String> IS_VALID_EMAIL =
            input -> input != null && EMAIL_PATTERN.matcher(input).matches();

    /** True if the input is digits, optionally hyphen-grouped and optionally '+' prefixed. */
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
