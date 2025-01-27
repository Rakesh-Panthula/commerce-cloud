/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.verificationtoken.VerificationTokenFacade;
import de.hybris.platform.commercefacades.verificationtoken.data.CreateVerificationTokenInputData;
import de.hybris.platform.commercewebservicescommons.dto.verificationtoken.CreateVerificationTokenInputWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.verificationtoken.VerificationPurposeWsDTOType;
import de.hybris.platform.commercewebservicescommons.dto.verificationtoken.VerificationTokenWsDTO;
import de.hybris.platform.util.Config;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class VerificationTokenControllerTest {

    private static String FAKE_TOKEN_ID = "<LGN[nZbnrnhMWy2uBbzKWU/SQRVBZ7mJaiXX9/87PegvovM=]>";

    @Mock
    private VerificationTokenFacade verificationTokenFacade;

    @Mock
    private Validator createVerificationTokenInputValidator;

    @InjectMocks
    private VerificationTokenController verificationTokenController;

    @Before
    public void setUp() {
        when(verificationTokenFacade.createVerificationToken(any(CreateVerificationTokenInputData.class))).thenReturn(FAKE_TOKEN_ID);
    }

    @Test
    public void shouldCreateVerificationToken() {
        CreateVerificationTokenInputWsDTO createVerificationTokenInputWsDTO = buildCreateVerificationTokenInputWsDTO(VerificationPurposeWsDTOType.LOGIN, "test@sap.com", "1234");

        try( var config = mockStatic(Config.class);
             var securityContextHolder = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = Mockito.mock(SecurityContext.class, RETURNS_DEEP_STUBS);
            when(securityContext.getAuthentication().getName()).thenReturn("test@sap.com");

            config.when(() -> Config.getInt("otp.customer.login.token.ttlseconds", 300)).thenReturn(300);
            securityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            VerificationTokenWsDTO verificationTokenWsDTO = verificationTokenController.createVerificationToken(createVerificationTokenInputWsDTO);
            assertNotNull(verificationTokenWsDTO);
            assertEquals(FAKE_TOKEN_ID, verificationTokenWsDTO.getTokenId());
            assertEquals(300, verificationTokenWsDTO.getExpiresIn().intValue());
        }
    }

    @Test(expected = WebserviceValidationException.class)
    public void shouldThrowErrorWhenValidationFails() {
        CreateVerificationTokenInputWsDTO input = buildCreateVerificationTokenInputWsDTO(VerificationPurposeWsDTOType.LOGIN, "test@sap.com", null);

        doAnswer(invocationOnMock -> {
            final Errors errors = invocationOnMock.getArgument(1);
            errors.rejectValue("loginId", "field.required");
            return null;
        }).when(createVerificationTokenInputValidator).validate(eq(input), any());

        verificationTokenController.createVerificationToken(input);
    }

    private CreateVerificationTokenInputWsDTO buildCreateVerificationTokenInputWsDTO(VerificationPurposeWsDTOType purpose, String loginId, String password) {
        CreateVerificationTokenInputWsDTO createVerificationTokenInputWsDTO = new CreateVerificationTokenInputWsDTO();
        createVerificationTokenInputWsDTO.setPurpose(purpose);
        createVerificationTokenInputWsDTO.setLoginId(loginId);
        createVerificationTokenInputWsDTO.setPassword(password);
        return createVerificationTokenInputWsDTO;
    }

}