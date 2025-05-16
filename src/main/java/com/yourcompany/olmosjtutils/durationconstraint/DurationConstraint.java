package com.yourcompany.olmosjtutils.durationconstraint;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = DurationConstraintValidator.class)
@Target({TYPE})
@Retention(RUNTIME)
public @interface DurationConstraint {

    // Enum for validation mode
    enum ValidationMode {
        STRICT, // End date must be <= start.plus(duration, unit)
        SLOPPY  // Uses ChronoUnit.between(), counts full completed units
    }

    // Default message uses the template key for flexibility
    String message() default "{uz.tengebank.validation.constraints.DurationConstraint.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * The name of the field representing the start date/time.
     */
    String startFieldName() default "startTime";

    /**
     * The name of the field representing the end date/time.
     */
    String endFieldName() default "endTime";

    /**
     * The minimum allowed duration unit between start and end.
     * Defaults to 0, ensuring the end date/time is not before the start date/time.
     * If set to a positive value, enforces a minimum duration.
     */
    long min() default 0; // Default to 0 to enforce end >= start

    /**
     * The maximum allowed duration unit between start and end.
     * Defaults to 1 (representing days when unit is YEARS).
     * If unit is changed, this value should typically be overridden to represent the desired maximum in the new unit.
     */
    long max() default 1; // Default to approx 1 year

    /**
     * The ChronoUnit to use for calculating and comparing the duration.
     * Defaults to YEARS.
     */
    ChronoUnit unit() default ChronoUnit.YEARS;

    /**
     * Whether to consider the constraint valid if either start or end date/time is null.
     * If true, null values are considered valid. If false (default), both must be non-null for validation.
     */
    boolean allowNull() default false; // Changed default to false, requiring both fields

    /**
     * The validation mode to use.
     * STRICT: Enforces that the endDate is not after startDate.plus(max, unit) and not before startDate.plus(min, unit).
     * SLOPPY: Enforces that ChronoUnit.between(startDate, endDate) is between min and max.
     * Defaults to SLOPPY.
     */
    ValidationMode mode() default ValidationMode.SLOPPY;

    /**
     * The validation message template. Can include placeholders like
     * {startFieldName}, {endFieldName}, {min}, {max}, {unit}.
     */
    String messageTemplate() default "Duration between ${startFieldName} and ${endFieldName} must be between ${min} and ${max} ${unit}s.";

    /**
     * A specific message template used when the end date/time is before the start date/time.
     * Defaults to "Start date must be before end date".
     */
    String startBeforeEndMessageTemplate() default "Start date (${startFieldName}) must be before end date (${endFieldName}).";

}

@Slf4j
class DurationConstraintValidator implements ConstraintValidator<DurationConstraint, Object> {

    private String startFieldName;
    private String endFieldName;
    private long minDuration;
    private long maxDuration;
    private ChronoUnit unit;
    private boolean allowNull;
    private String messageTemplate;
    private String startBeforeEndMessageTemplate;
    private DurationConstraint.ValidationMode validationMode;

    // Regex to find placeholders like ${fieldName}
    private static final Pattern MESSAGE_PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{(\\w+)\\}");


    @Override
    public void initialize(DurationConstraint constraint) {
        this.startFieldName = constraint.startFieldName();
        this.endFieldName = constraint.endFieldName();
        this.minDuration = constraint.min();
        this.maxDuration = constraint.max();
        this.unit = constraint.unit();
        this.allowNull = constraint.allowNull();
        this.messageTemplate = constraint.messageTemplate();
        this.startBeforeEndMessageTemplate = constraint.startBeforeEndMessageTemplate();
        this.validationMode = constraint.mode();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            // Class-level constraint, null object is generally considered valid
            return true;
        }

        Object startValue;
        Object endValue;

        try {
            Field startField = value.getClass().getDeclaredField(startFieldName);
            startField.setAccessible(true);
            startValue = startField.get(value);

            Field endField = value.getClass().getDeclaredField(endFieldName);
            endField.setAccessible(true);
            endValue = endField.get(value);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Handle reflection errors gracefully.
            log.error(e.getMessage(), e);
            // Indicate failure if fields aren't accessible or don't exist
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Internal validation error: Could not access date/time fields (" +
                            startFieldName + ", " + endFieldName + ").")
                    .addConstraintViolation();
            return false;
        }

        if (startValue == null || endValue == null) {
            return allowNull; // Return based on allowNull setting if either is null
        }

        Temporal startDate = convertToTemporal(startValue); // returns null if unsupported type was found
        Temporal endDate = convertToTemporal(endValue); // returns null if unsupported type was found

        if (startDate == null || endDate == null) {
            // This implies an unsupported type was found for a non-null field value
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Unsupported date/time type for fields (" +
                            startFieldName + ": " + (startValue != null ? startValue.getClass().getName() : "null") + ", " +
                            endFieldName + ": " + (endValue != null ? endValue.getClass().getName() : "null") + ").")
                    .addConstraintViolation();
            return false;
        }

        // --- Specific Check: Start Date must be before End Date ---
        // Convert to LocalDateTime for reliable comparison across different Temporal types
        LocalDateTime startForComparison;
        LocalDateTime endForComparison;

        try {
            startForComparison = convertToLocalDateTime(startDate);
            endForComparison = convertToLocalDateTime(endDate);
        } catch (IllegalArgumentException e) {
            // Should not happen with supported types, but as a safeguard
            log.error(e.getMessage(), e);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Internal validation error: Could not convert date/time fields for comparison.")
                    .addConstraintViolation();
            return false;
        }

        if (startForComparison.isAfter(endForComparison)) {
            // Use the specific message template for this case
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(resolveMessageTemplate(startBeforeEndMessageTemplate, value))
                    .addPropertyNode(startFieldName) // Associate with start field
                    .addConstraintViolation();
            return false;
        }
        // --- End Specific Check ---

        switch (this.validationMode) {
            case STRICT:
                // --- STRICT MODE ---
                // Minimum Duration Check (Strict interpretation)
                // This is relevant if minDurationValue > 0.
                if (this.minDuration > 0) {
                    LocalDateTime minAllowedEndDatePoint;
                    try {
                        minAllowedEndDatePoint = startForComparison.plus(this.minDuration, this.unit);
                    } catch (DateTimeException e) {
                        log.error("Error calculating min allowed end date point with unit {}.", this.unit, e);
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate("Internal validation error: Cannot calculate minimum duration with unit " + this.unit + ".")
                                .addConstraintViolation();
                        return false;
                    }
                    // endDate must be on or after the calculated minimum point.
                    if (endForComparison.isBefore(minAllowedEndDatePoint)) {
                        context.disableDefaultConstraintViolation();
                        context.buildConstraintViolationWithTemplate(resolveMessageTemplate(this.messageTemplate, value))
                                .addPropertyNode(this.startFieldName)
                                .addPropertyNode(this.endFieldName)
                                .addConstraintViolation();
                        return false;
                    }
                }
                // Maximum Duration Check (Strict interpretation)
                // endDate must be on or before startDate.plus(maxDurationValue, unit).
                // So, endDate cannot be *after* startDate.plus(maxDurationValue, unit).
                LocalDateTime maxAllowedEndDatePoint;
                try {
                    maxAllowedEndDatePoint = startForComparison.plus(this.maxDuration, this.unit);
                } catch (DateTimeException e) {
                    log.error("Error calculating max allowed end date point with unit {}.", this.unit, e);
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Internal validation error: Cannot calculate maximum duration with unit " + this.unit + ".")
                            .addConstraintViolation();
                    return false;
                }
                if (endForComparison.isAfter(maxAllowedEndDatePoint)) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(resolveMessageTemplate(this.messageTemplate, value))
                            .addPropertyNode(this.startFieldName)
                            .addPropertyNode(this.endFieldName)
                            .addConstraintViolation();
                    return false;
                }
                break;
            case SLOPPY:
                // --- SLOPPY MODE (Calculate duration using the requested unit) ---
                long duration;
                try {
                    duration = unit.between(startDate, endDate);
                } catch (Exception e) {
                    // Handle cases where the unit is not supported between the temporal types
                    log.error(e.getMessage(), e);
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Internal validation error: Cannot calculate duration using unit " + unit + " between the specified date/time types.")
                            .addConstraintViolation();
                    return false;
                }
                // Check min/max duration (this also catches duration < 0 if min is 0)
                if (duration < minDuration || duration > maxDuration) {
                    // Build custom violation message using the template
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(resolveMessageTemplate(messageTemplate, value))
                            .addPropertyNode(startFieldName) // Associate with start field
                            .addPropertyNode(endFieldName)
                            .addConstraintViolation();

                    return false;
                }
                break;
            default:
                // Should not happen if enum is exhaustive and mode is initialized
                log.warn("Unknown validation mode: {}", this.validationMode);
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Internal validation error: Unknown validation mode.")
                        .addConstraintViolation();
                return false;
        }

        return true; // All checks passed for the selected mode
    }

    // Helper method to convert supported Objects to Temporal
    private Temporal convertToTemporal(Object obj) {
        if (obj instanceof LocalDate) {
            return (LocalDate) obj;
        } else if (obj instanceof LocalDateTime) {
            return (LocalDateTime) obj;
        } else if (obj instanceof Timestamp) {
            return ((Timestamp) obj).toLocalDateTime(); // Convert Timestamp to LocalDateTime
        }
        // Return null for unsupported types
        return null;
    }

    // Helper method to convert Temporal objects to LocalDateTime for comparison
    private LocalDateTime convertToLocalDateTime(Temporal temporal) {
        if (temporal instanceof LocalDateTime) {
            return (LocalDateTime) temporal;
        } else if (temporal instanceof LocalDate) {
            // Convert LocalDate to LocalDateTime at the start of the day for comparison
            return ((LocalDate) temporal).atStartOfDay();
        }
        // Note: Timestamp is converted to LocalDateTime by convertToTemporal,
        // so we expect a LocalDateTime instance here if it originated as Timestamp.
        throw new IllegalArgumentException("Unsupported Temporal type for comparison: " + temporal.getClass().getName());
    }

    // Helper to resolve placeholders in message templates
    private String resolveMessageTemplate(String template, Object validatedObject) {
        Matcher matcher = MESSAGE_PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();

        Map<String, Object> values = new HashMap<>();
        values.put("startFieldName", startFieldName);
        values.put("endFieldName", endFieldName);
        values.put("min", minDuration);
        values.put("max", maxDuration);
        values.put("unit", unit.name().toLowerCase()); // Use lower case unit name

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            Object replacement = values.getOrDefault(placeholder, "${" + placeholder + "}"); // Keep placeholder if not found
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
