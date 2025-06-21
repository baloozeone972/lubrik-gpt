package com.virtualcompanion.userservice.validation;

public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {
    
    private int minimumAge;
    
    @Override
    public void initialize(Adult constraintAnnotation) {
        this.minimumAge = constraintAnnotation.minimumAge();
    }
    
    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true; // Let @NotNull handle null validation
        }
        
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        return age >= minimumAge;
    }
}
