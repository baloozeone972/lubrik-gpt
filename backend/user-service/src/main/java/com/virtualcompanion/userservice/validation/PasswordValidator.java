package com.virtualcompanion.userservice.validation;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern NUMBER_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile(".*[!@#$%^&*(),.?\":{}|<>].*");
    @Value("${app.security.password-min-length:8}")
    private int minLength;
    @Value("${app.security.password-require-uppercase:true}")
    private boolean requireUppercase;
    @Value("${app.security.password-require-lowercase:true}")
    private boolean requireLowercase;
    @Value("${app.security.password-require-numbers:true}")
    private boolean requireNumbers;
    @Value("${app.security.password-require-special:true}")
    private boolean requireSpecial;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        context.disableDefaultConstraintViolation();

        if (password.length() < minLength) {
            context.buildConstraintViolationWithTemplate(
                    String.format("Password must be at least %d characters long", minLength)
            ).addConstraintViolation();
            return false;
        }

        if (requireUppercase && !UPPERCASE_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one uppercase letter"
            ).addConstraintViolation();
            return false;
        }

        if (requireLowercase && !LOWERCASE_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one lowercase letter"
            ).addConstraintViolation();
            return false;
        }

        if (requireNumbers && !NUMBER_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one number"
            ).addConstraintViolation();
            return false;
        }

        if (requireSpecial && !SPECIAL_PATTERN.matcher(password).matches()) {
            context.buildConstraintViolationWithTemplate(
                    "Password must contain at least one special character"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
