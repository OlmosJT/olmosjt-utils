# DurationConstraint Validator

A custom Java Bean Validation (JSR 380) annotation, `@DurationConstraint`, designed for Spring Boot applications to validate the time period between two date/time fields within an object. It ensures that the duration falls within a specified minimum and maximum range, using a configurable time unit.

## Features

* **Class-Level Validation**: Apply this annotation to a class to validate two of its date/time fields.
* **Flexible Date Types**: Supports `java.time.LocalDate`, `java.time.LocalDateTime`, and `java.sql.Timestamp`.
* **Configurable Duration**:
    * Define minimum (`min`) and maximum (`max`) allowed duration.
    * Specify the time unit (`unit`) for duration calculation (e.g., `DAYS`, `HOURS`, `YEARS`) using `java.time.temporal.ChronoUnit`.
* **Two Validation Modes**:
    * `STRICT`: End date must be `<= startDate.plus(max, unit)` and `>= startDate.plus(min, unit)`.
       - **Example:**  
      If `startDate = 2025-05-16T10:00:00` and `max = 1 day`, then:
      ```
      ✅ endDate = 2025-05-17T10:00:00  
      ❌ endDate = 2025-05-17T10:00:01
      ```
    * `SLOPPY`: The actual number of full `ChronoUnit`s between start and end dates must be between `min` and `max`.
      - **Example** (`ChronoUnit.DAYS`, min=1, max=1):  
      `startDate = 2025-05-16T10:00:00`
      ```
      ✅ endDate = 2025-05-17T10:00:00 → full 1 day passed  
      ✅ endDate = 2025-05-17T23:59:59 → still just 1 day  
      ❌ endDate = 2025-05-18T00:00:00 → 2 full days
      ```
* **Customizable Field Names**: Specify the names of the start and end date fields (defaults to `startTime` and `endTime`).
* **Null Handling**: Option to allow (`allowNull = true`) or disallow (`allowNull = false`, default) null values for the date fields. If parameters are optional and given null, ten NullHandling logic works to pass validation.
* **Customizable Validation Messages**:
    * Supports standard Bean Validation message interpolation.
    * Provides a `messageTemplate` for general duration errors.
    * Provides a `startBeforeEndMessageTemplate` for errors where the start date is not before the end date.
    * Placeholders like `${startFieldName}`, `${endFieldName}`, `${min}`, `${max}`, `${unit}` can be used in message templates.
* **Graceful Error Handling**: Catches and logs reflection errors or issues with date calculations, providing informative validation messages.

## Requirements
* Java 17 or higher
* Spring Boot Validation (spring-boot-starter-validation) or any JSR 380 compliant implementation (e.g., Hibernate Validator).
* Jakarta Validation API (`jakarta.validation-api`).
* (Optional) Lombok for `@Slf4j` logging. If not using Lombok, replace `@Slf4j` with a standard logger initialization.

## How to Use

1.  **Add the Code**: Place the `DurationConstraint.java` file (containing both `@DurationConstraint` annotation and `DurationConstraintValidator` class) into your project, typically in a utility or validation package (e.g., `com.example.yourproject.utility.validation`). Ensure the package declaration in the file matches its location.

2.  **Apply the Annotation**: Annotate the class that contains the date fields you want to validate.

    ```java
    import jakarta.validation.Valid; // If Request DTO is nested
    import java.time.LocalDate;
    // Assuming DurationConstraint is in uz.tengebank.db.util
    import uz.tengebank.db.util.DurationConstraint;
    import java.time.temporal.ChronoUnit;

    @DurationConstraint(
        startFieldName = "contractStartDate",
        endFieldName = "contractEndDate",
        min = 0, // Minimum 0 days (end date cannot be before start date)
        max = 5, // Maximum 5 years
        unit = ChronoUnit.YEARS,
        mode = DurationConstraint.ValidationMode.STRICT, // Or SLOPPY
        messageTemplate = "The contract period between ${startFieldName} and ${endFieldName} must be from ${min} to ${max} ${unit}.",
        startBeforeEndMessageTemplate = "${startFieldName} must be earlier than ${endFieldName}."
    )
    @Getter @Setter 
    @AllArgsConstructor
    @NoArgsConstructor
    public class ContractDetails {
        private LocalDate contractStartDate;
        private LocalDateTime contractEndDate; // Different types are supported
    }
    ```

3.  **Enable Validation**: Ensure that validation is triggered, for example, by using `@Valid` in your Spring MVC controller:

    ```java
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RestController;
    import jakarta.validation.Valid;

    @RestController
    public class ContractController {

        @PostMapping("/contracts")
        public ResponseEntity<String> createContract(@Valid @RequestBody ContractDetails contractDetails) {
            // Process valid contract
            return ResponseEntity.ok("Contract is valid.");
        }
    }
    ```

## Configuration Parameters (`@DurationConstraint`)

| Parameter                     | Type                                  | Default Value                                                    | Description                                                                                                                                                              |
| :---------------------------- | :------------------------------------ | :--------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `message`                     | `String`                              | `"{uz.tengebank.validation.constraints.DurationConstraint.message}"` | The key for the validation message. Resolved via `ValidationMessages.properties` or overridden directly.                                                               |
| `groups`                      | `Class<?>[]`                          | `{}`                                                             | Validation groups.                                                                                                                                                       |
| `payload`                     | `Class<? extends Payload>[]`         | `{}`                                                             | Payload for the constraint.                                                                                                                                              |
| `startFieldName`              | `String`                              | `"startTime"`                                                    | The name of the field representing the start date/time.                                                                                                                  |
| `endFieldName`                | `String`                              | `"endTime"`                                                      | The name of the field representing the end date/time.                                                                                                                    |
| `min`                         | `long`                                | `0`                                                              | Minimum allowed duration units. If `0`, end date/time must not be before start.                                                                                          |
| `max`                         | `long`                                | `1`                                                              | Maximum allowed duration units. *Note: Default is `1` with `ChronoUnit.YEARS`. Adjust if changing `unit`.* |
| `unit`                        | `ChronoUnit`                          | `ChronoUnit.YEARS`                                               | The time unit to use for calculating duration (e.g., `DAYS`, `MONTHS`, `YEARS`, `HOURS`).                                                                                 |
| `allowNull`                   | `boolean`                             | `false`                                                          | If `true`, validation passes if either start or end date/time is null. If `false`, both must be non-null.                                                               |
| `mode`                        | `DurationConstraint.ValidationMode`   | `ValidationMode.SLOPPY`                                          | `STRICT`: `endDate` must be within `startDate.plus(min, unit)` and `startDate.plus(max, unit)`. `SLOPPY`: `ChronoUnit.between(startDate, endDate)` is between `min` and `max`. |
| `messageTemplate`             | `String`                              | `"Duration between ${startFieldName} and ${endFieldName} must be between ${min} and ${max} ${unit}s."` | Template for the general duration validation error message.                                                                                                         |
| `startBeforeEndMessageTemplate` | `String`                              | `"Start date (${startFieldName}) must be before end date (${endFieldName})."` | Template for the error message when the start date is not before the end date.                                                                                       |

## Supported Date/Time Types

The `startFieldName` and `endFieldName` can point to fields of the following types:

* `java.time.LocalDate`
* `java.time.LocalDateTime`
* `java.sql.Timestamp` (converted to `LocalDateTime` for comparison)

If other types are used, or if fields are non-null but their types are not supported, a validation error regarding "Unsupported date/time type" will be generated.

## Validation Modes Explained

### `SLOPPY` (Default)

* Calculates the duration using `unit.between(startDate, endDate)`. This method typically counts the number of *full* completed units.
* For example, if `unit` is `ChronoUnit.DAYS`, `SLOPPY` mode checks if the number of full days between `startDate` and `endDate` is within the `[min, max]` range.
* The check `startDate.isAfter(endDate)` is performed first. If true, validation fails with `startBeforeEndMessageTemplate`.

### `STRICT`

* Ensures that `endDate` is not *before* `startDate.plus(min, unit)`.
* Ensures that `endDate` is not *after* `startDate.plus(max, unit)`.
* This mode performs a more precise point-in-time check.
* For example, if `startDate` is `2023-01-01`, `min` is `0`, `max` is `1`, and `unit` is `ChronoUnit.YEARS`:
    * The `endDate` must be on or after `2023-01-01`.
    * The `endDate` must be on or before `2024-01-01` (inclusive of `2024-01-01T00:00:00` if time components are involved).
* The check `startDate.isAfter(endDate)` is performed first. If true, validation fails with `startBeforeEndMessageTemplate`.

## Customizing Messages

You can customize validation messages in a few ways:

1.  **Override `message` parameter**:
    ```java
    @DurationConstraint(message = "The selected period is not valid.")
    ```

2.  **Use `messageTemplate` and `startBeforeEndMessageTemplate`**:
    These allow for dynamic messages with placeholders:
    * `${startFieldName}`: The name of your start date field.
    * `${endFieldName}`: The name of your end date field.
    * `${min}`: The configured minimum duration.
    * `${max}`: The configured maximum duration.
    * `${unit}`: The configured `ChronoUnit` (e.g., "years", "days").

    ```java
    @DurationConstraint(
        messageTemplate = "Period for ${startFieldName} to ${endFieldName} must be ${min}-${max} ${unit}.",
        startBeforeEndMessageTemplate = "Error: ${startFieldName} cannot be after ${endFieldName}."
    )
    ```

3.  **Using `ValidationMessages.properties`**:
    If you keep the default `message()` value (`"{uz.tengebank.validation.constraints.DurationConstraint.message}"`), you can define this key in your `src/main/resources/ValidationMessages.properties` file:
    ```properties
    com.yourorganization.validation.constraints.DurationConstraint.message=The duration between {startFieldName} and {endFieldName} must be between {min} and {max} {unit} (using default key).
    ```
    Note: When using this method, the placeholders in the properties file should use `{}` as per Bean Validation standards, not `${}`. The internal `resolveMessageTemplate` uses `${}` for `messageTemplate` and `startBeforeEndMessageTemplate` parameters directly. If the main `message` key points to a string that itself contains these `${}` placeholders and is *not* one of the specific template parameters, those won't be resolved by the custom resolver. It's generally better to use `messageTemplate` for full control over placeholder resolution with `${}`.

## Logging

The `DurationConstraintValidator` uses `@Slf4j` for logging internal errors (e.g., reflection issues, date calculation problems). Ensure you have SLF4J configured in your project if you wish to see these logs.

## Example: Validating an Event DTO

```java
package com.example.dto;

import uz.tengebank.db.util.DurationConstraint; // Adjust import
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@DurationConstraint(
    startFieldName = "eventStart",
    endFieldName = "eventEnd",
    min = 1,
    max = 72,
    unit = ChronoUnit.HOURS,
    allowNull = false,
    mode = DurationConstraint.ValidationMode.STRICT,
    messageTemplate = "Event duration must be between ${min} and ${max} <span class="math-inline">\{unit\}s\.",
startBeforeEndMessageTemplate \= "Event start \(</span>{startFieldName}) must be before event end (${endFieldName})."
)
public class EventDTO {

    private LocalDateTime eventStart;
    private LocalDateTime eventEnd;

    // Getters and Setters
    public LocalDateTime getEventStart() {
        return eventStart;
    }

    public void setEventStart(LocalDateTime eventStart) {
        this.eventStart = eventStart;
    }

    public LocalDateTime getEventEnd() {
        return eventEnd;
    }

    public void setEventEnd(LocalDateTime eventEnd) {
        this.eventEnd = eventEnd;
    }
}
