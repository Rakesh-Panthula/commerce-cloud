/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.platform.commercewebservicescommons.dto.verificationtoken.CreateVerificationTokenInputWsDTO;
import de.hybris.platform.webservicescommons.validators.FieldNotEmptyValidator;
import org.springframework.validation.Errors;

import static de.hybris.platform.commercewebservicescommons.dto.verificationtoken.VerificationPurposeWsDTOType.LOGIN;

/**
 * Validator to validate whether password is provided for certain scenarios.
 */
public class VerificationTokenPasswordValidator extends FieldNotEmptyValidator {

    /**
     * Checks if the given class is supported by this validator.
     * @param clazz the class that needs to be checked
     * @return true if the class is CreateVerificationTokenInputWsDTO.class, false otherwise
     */
    @Override
    public boolean supports(Class clazz) {
        return clazz.isAssignableFrom(CreateVerificationTokenInputWsDTO.class);
    }

    /**
     * Validates the given object. This method will be called by the Spring MVC, when a new object of the type of this validator is being validated.
     *
     * @param object the object that needs to be validated, which should be an instance of CreateVerificationTokenInputWsDTO
     * @param errors the errors object that should contain any validation errors
     */
    @Override
    public void validate(Object object, Errors errors) {
        CreateVerificationTokenInputWsDTO createVerificationTokenInput = (CreateVerificationTokenInputWsDTO) object;
        if (LOGIN == createVerificationTokenInput.getPurpose()) {
            super.validate(createVerificationTokenInput, errors);
        }
    }
}
