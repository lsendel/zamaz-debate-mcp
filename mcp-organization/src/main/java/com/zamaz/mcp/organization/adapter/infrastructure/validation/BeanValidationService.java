package com.zamaz.mcp.organization.adapter.infrastructure.validation;

import com.zamaz.mcp.common.application.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bean Validation implementation of ValidationService.
 * Uses JSR-303 Bean Validation for object validation.
 */
@Component
@RequiredArgsConstructor
public class BeanValidationService implements ValidationService {
    
    private final Validator validator;
    
    @Override
    public Map<String, List<String>> validate(Object object) {
        var violations = validator.validate(object);
        
        return violations.stream()
            .collect(Collectors.groupingBy(
                violation -> violation.getPropertyPath().toString(),
                Collectors.mapping(
                    ConstraintViolation::getMessage,
                    Collectors.toList()
                )
            ));
    }
    
    @Override
    public void validateOrThrow(Object object) {
        var errors = validate(object);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
    
    @Override
    public boolean isValid(Object object) {
        return validator.validate(object).isEmpty();
    }
}