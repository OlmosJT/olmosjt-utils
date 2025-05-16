package com.yourcompany.olmosjtutils.masking;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.spi.CurrencyNameProvider;

@Slf4j
@UtilityClass
public class MaskingUtils {
    private static final char DEFAULT_MASK_CHAR = '*';

    /**
     * Core masking logic. Masks a string by showing a specified number of characters
     * at the beginning (prefix) and at the end (suffix), replacing the characters
     * in between with a specified masking character.
     *
     * @param data The string to mask. Can be null or empty.
     * @param prefixLength The number of characters to leave unmasked at the beginning.
     * @param suffixLength The number of characters to leave unmasked at the end.
     * @param minLengthToMask The minimum length the string must have to apply the
     * prefix/suffix masking logic. If the string's length is
     * less than this value, the entire string is masked.
     * @param maskChar The character to use for masking the middle part.
     * @return The masked string. Returns the original string if it's null or empty.
     */
    public static String mask(String data, int prefixLength, int suffixLength, int minLengthToMask, char maskChar) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        int len = data.length();

        if (len < minLengthToMask) {
            // Data is too short for selective prefix/suffix masking as per minLengthToMask rule, so mask the entire string.
            return String.valueOf(maskChar).repeat(len);
        }

        // Ensure prefixLength and suffixLength are non-negative
        prefixLength = Math.max(0, prefixLength);
        suffixLength = Math.max(0, suffixLength);

        // Adjust prefixLength to not exceed string length
        int actualPrefixLength = Math.min(prefixLength, len);

        // Adjust suffixLength to not exceed the remaining length after considering the prefix,
        // and ensure it's not negative.
        int actualSuffixLength = Math.max(0, Math.min(suffixLength, len - actualPrefixLength));

        StringBuilder masked = new StringBuilder();
        masked.append(data, 0, actualPrefixLength);

        int maskedCharsCount = len - actualPrefixLength - actualSuffixLength;
        if (maskedCharsCount > 0) {
            masked.append(String.valueOf(maskChar).repeat(maskedCharsCount));
        }

        // Append suffix if its length is positive.
        // The substring starts from `len - actualSuffixLength`.
        if (actualSuffixLength > 0) {
            masked.append(data.substring(len - actualSuffixLength));
        }

        return masked.toString();
    }

    /**
     * Overloaded version of {@link #mask(String, int, int, int, char)} that uses
     * the default mask character '*'.
     *
     * @param data The string to mask.
     * @param prefixLength The number of characters to leave unmasked at the beginning.
     * @param suffixLength The number of characters to leave unmasked at the end.
     * @param minLengthToMask The minimum length for applying prefix/suffix masking.
     * @return The masked string.
     */
    public static String mask(String data, int prefixLength, int suffixLength, int minLengthToMask) {
        return mask(data, prefixLength, suffixLength, minLengthToMask, DEFAULT_MASK_CHAR);
    }

    // --- Specific Masking Methods (based on your TaxSalaryRequest DTO) ---

    /**
     * Masks a Pinfl (e.g., for Uzbekistan, typically 14 digits).
     * Shows first 4 and last 3 characters if length meets minLengthToMask.
     * Example: "1234*******567"
     */
    public static String maskPinfl(String pinfl) {
        return mask(pinfl, 4, 3, 8); // Shows 7 chars, masks at least 1 if len=8
    }

    /**
     * Masks a TIN (Taxpayer Identification Number, e.g., for Uzbekistan, 9 digits).
     * Shows first 3 and last 2 characters if length meets minLengthToMask.
     * Example: "123****45"
     */
    public static String maskTin(String tin) {
        return mask(tin, 3, 2, 6); // Shows 5 chars, masks at least 1 if len=6
    }

    /**
     * Masks a passport series (e.g., "AA").
     * Shows the first character if length meets minLengthToMask.
     * Example: "A*" for a 2-character series.
     */
    public static String maskPassportSeries(String series) {
        return mask(series, 1, 0, 2); // Shows 1 char, masks at least 1 if len=2
    }

    /**
     * Masks a passport number (e.g., for Uzbekistan, 7 digits).
     * Shows first 2 and last 2 characters if length meets minLengthToMask.
     * Example: "12***34"
     */
    public static String maskPassportNumber(String number) {
        return mask(number, 2, 2, 5); // Shows 4 chars, masks at least 1 if len=5
    }

    // --- Additional Common Masking Method Suggestions ---

    /**
     * Masks an email address.
     * Shows the first character of the local part, then asterisks, followed by the full domain name.
     * If the local part is 1 character or less, it's shown as is with the domain.
     * Example: "j***@example.com", "jo***@example.com", "a@example.com"
     *
     * @param email The email address to mask.
     * @return The masked email address, or the original string if null/empty or format is unexpected.
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) { // No '@', or email starts with '@' (not a typical valid email for this masking)
            // Fallback to a generic mask for the whole string if it's very short or unparseable as an email.
            return mask(email, 1, 1, 3); // Show first and last char if length is 3 or more
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex); // Includes the '@'

        if (localPart.length() <= 1) {
            // If local part is very short (e.g., "a@example.com"), don't mask it further.
            return localPart + domainPart;
        }

        // Mask the local part: show its first character, then mask the rest.
        // minLengthToMask is 2, so if localPart is "ab", it becomes "a*".
        String maskedLocalPart = mask(localPart, 1, 0, 2);
        return maskedLocalPart + domainPart;
    }

    /**
     * Masks a phone number, typically showing only the last few digits.
     * This is a basic example; international phone numbers can be complex.
     * Example: "********1234" (shows last 4 digits if length >= 7)
     *
     * @param phoneNumber The phone number to mask.
     * @return The masked phone number, or the original string if null/empty.
     */
    public static String maskPhoneNumber(String phoneNumber) {
        // This example masks everything except the last 4 digits.
        // It requires the phone number to be at least 7 characters long for this specific rule.
        return mask(phoneNumber, 0, 4, 7);
    }

    /**
     * Masks a BigDecimal monetary amount by returning a generic placeholder string.
     * This is useful for logs or UI displays where the actual amount should not be shown.
     *
     * @param amount The BigDecimal amount to mask.
     * @return A placeholder string like "[PROTECTED AMOUNT]" or null if the input is null.
     */
    public static String maskAmountSimple(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return "[PROTECTED AMOUNT]";
    }

    /**
     * Masks a BigDecimal monetary amount by returning a placeholder string
     * prefixed with the currency symbol for the given locale.
     *
     * @param amount The BigDecimal amount to mask.
     * @param locale The locale to determine the currency symbol. If null, default locale is used.
     * @return A placeholder string like "$ ***.**" or "€ ***.**", or null if the input amount is null.
     */
    public static String maskAmountWithCurrency(BigDecimal amount, Locale locale) {
        if (amount == null) {
            return null;
        }
        if (locale == null) {
            locale = Locale.getDefault(); // Or a specific default like Locale.US
        }
        try {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);
            String currencySymbol = currencyFormat.getCurrency().getSymbol(locale);
            // You can choose a fixed placeholder format
            return currencySymbol + " ***.**";
        } catch (Exception e) {
            // Fallback in case of issues with locale or currency formatting
            return "[PROTECTED AMOUNT]";
        }
    }

    public static String maskAmountWithCurrency(final BigDecimal amount, final String currencyCode) {
        if (amount == null) {
            return null;
        }
        if (currencyCode == null) {
            return "[PROTECTED AMOUNT]";
        }
        try {
            String currencySymbol = Currency.getInstance(currencyCode).getSymbol();
            // You can choose a fixed placeholder format
            return currencySymbol + " ***.**";
        } catch (Exception e) {
            // Fallback in case of issues with locale or currency formatting
            return "[PROTECTED AMOUNT]";
        }
    }



}
