package com.reyzarium.eventforge.eventservice.api.validation;

import com.reyzarium.eventforge.eventservice.api.organizer.dto.CreateEventRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EventScheduleValidator implements ConstraintValidator<ValidEventSchedule, CreateEventRequest> {

    @Override
    public boolean isValid(CreateEventRequest request, ConstraintValidatorContext context) {
        if (request == null || request.startsAt() == null || request.endsAt() == null) {
            return true;
        }

        boolean valid = request.endsAt().isAfter(request.startsAt());
        if (!valid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("must be after startsAt")
                    .addPropertyNode("endsAt")
                    .addConstraintViolation();
        }
        return valid;
    }
}
