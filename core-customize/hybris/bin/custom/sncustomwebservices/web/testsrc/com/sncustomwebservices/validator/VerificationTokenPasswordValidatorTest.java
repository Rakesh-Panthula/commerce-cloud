/**
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.validator;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercewebservicescommons.dto.verificationtoken.CreateVerificationTokenInputWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.verificationtoken.VerificationPurposeWsDTOType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class VerificationTokenPasswordValidatorTest {

    @InjectMocks
    private VerificationTokenPasswordValidator verificationTokenPasswordValidator;

    private Errors errors;

    @Before
    public void setUp() {
        verificationTokenPasswordValidator.setFieldPath("password");
    }

    @Test
    public void shouldNotRaiseErrors() {
        // given
        final CreateVerificationTokenInputWsDTO createVerificationTokenInput = new CreateVerificationTokenInputWsDTO();
        errors = new BeanPropertyBindingResult(createVerificationTokenInput, "input");
        createVerificationTokenInput.setPurpose(VerificationPurposeWsDTOType.LOGIN);
        createVerificationTokenInput.setPassword("1234");
        // when
        verificationTokenPasswordValidator.validate(createVerificationTokenInput, errors);
        // then
        assertFalse(errors.hasErrors());
    }

    @Test
    public void shouldRaiseErrorsWhenPasswordIsNullWithLoginPurpose() {
        // given
        final CreateVerificationTokenInputWsDTO createVerificationTokenInput = new CreateVerificationTokenInputWsDTO();
        errors = new BeanPropertyBindingResult(createVerificationTokenInput, "input");
        createVerificationTokenInput.setPurpose(VerificationPurposeWsDTOType.LOGIN);
        createVerificationTokenInput.setPassword(null);
        // when
        verificationTokenPasswordValidator.validate(createVerificationTokenInput, errors);
        // then
        assertTrue(errors.hasErrors());
        FieldError result = (FieldError) errors.getAllErrors().get(0);
        assertEquals("password", result.getField());
        assertEquals("This field is required.", result.getDefaultMessage());
    }

    @Test
    public void shouldRaiseErrorsWhenPasswordIsEmptyWithLoginPurpose() {
        // given
        final CreateVerificationTokenInputWsDTO createVerificationTokenInput = new CreateVerificationTokenInputWsDTO();
        errors = new BeanPropertyBindingResult(createVerificationTokenInput, "input");
        createVerificationTokenInput.setPurpose(VerificationPurposeWsDTOType.LOGIN);
        createVerificationTokenInput.setPassword("");
        // when
        verificationTokenPasswordValidator.validate(createVerificationTokenInput, errors);
        // then
        assertTrue(errors.hasErrors());
        FieldError result = (FieldError) errors.getAllErrors().get(0);
        assertEquals("password", result.getField());
        assertEquals("This field is required.", result.getDefaultMessage());
    }

    @Test
    public void shoulNotRaiseErrorsWhenPurposeIsNotLogin() {
        // given
        final CreateVerificationTokenInputWsDTO createVerificationTokenInput = new CreateVerificationTokenInputWsDTO();
        errors = new BeanPropertyBindingResult(createVerificationTokenInput, "input");
        createVerificationTokenInput.setPurpose(null);
        createVerificationTokenInput.setPassword("1234");
        // when
        verificationTokenPasswordValidator.validate(createVerificationTokenInput, errors);
        // then
        assertFalse(errors.hasErrors());
    }

}