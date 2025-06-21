package com.virtualcompanion.userservice.validation;

interface Adult {
    String message() default "Must be at least {minimumAge} years old";
    int minimumAge() default 18;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
