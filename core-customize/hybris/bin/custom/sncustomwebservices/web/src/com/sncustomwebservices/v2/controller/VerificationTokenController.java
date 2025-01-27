/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package com.sncustomwebservices.v2.controller;


import de.hybris.platform.commercefacades.verificationtoken.VerificationTokenFacade;
import de.hybris.platform.commercefacades.verificationtoken.data.CreateVerificationTokenInputData;
import de.hybris.platform.commercewebservicescommons.annotation.SecurePortalUnauthenticatedAccess;
import de.hybris.platform.commercewebservicescommons.dto.verificationtoken.CreateVerificationTokenInputWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.verificationtoken.VerificationTokenWsDTO;
import de.hybris.platform.util.Config;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.annotation.Resource;
import java.util.Locale;

@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/verificationToken")
@Tag(name = "Verification Token")
public class VerificationTokenController extends BaseController {

    private static final String OTP_CUSTOMER_LOGIN_TOKEN_TTLSECONDS = "otp.customer.login.token.ttlseconds";
    private static final int OTP_CUSTOMER_LOGIN_TOKEN_TTLSECONDS_DEFAULT = 300;

    @Resource
    private VerificationTokenFacade verificationTokenFacade;

    @Resource
    private Validator createVerificationTokenInputValidator;
    @SecurePortalUnauthenticatedAccess
    @Secured({"ROLE_CLIENT", "ROLE_CUSTOMERGROUP"})
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    @ApiBaseSiteIdAndUserIdParam
    @Operation(operationId = "createVerificationToken", summary = "Creates verification token", description = "Creates verification token, and sends token code via designated channel e.g. email.")
    public VerificationTokenWsDTO createVerificationToken(
            @Parameter(description = "Object contains information for obtaining a verification token.", required = true) @RequestBody final CreateVerificationTokenInputWsDTO createVerificationTokenInputWsDTO) {
        validate(createVerificationTokenInputWsDTO, "createVerificationTokenInput", createVerificationTokenInputValidator);

        CreateVerificationTokenInputData input = convertToData(createVerificationTokenInputWsDTO);

        VerificationTokenWsDTO verificationTokenWsDTO = new VerificationTokenWsDTO();
        verificationTokenWsDTO.setTokenId(verificationTokenFacade.createVerificationToken(input));
        verificationTokenWsDTO.setExpiresIn(Config.getInt(OTP_CUSTOMER_LOGIN_TOKEN_TTLSECONDS, OTP_CUSTOMER_LOGIN_TOKEN_TTLSECONDS_DEFAULT));

        return verificationTokenWsDTO;
    }

    /**
     * Converts CreateVerificationTokenInputWsDTO to CreateVerificationTokenInputData
     *
     * @param input the input
     * @return CreateVerificationTokenInputData
     */
    private CreateVerificationTokenInputData convertToData(CreateVerificationTokenInputWsDTO input) {
        CreateVerificationTokenInputData createVerificationTokenInputData = new CreateVerificationTokenInputData();
        createVerificationTokenInputData.setPassword(input.getPassword());
        createVerificationTokenInputData.setPurpose(input.getPurpose().name());
        createVerificationTokenInputData.setLoginId(input.getLoginId().toLowerCase(Locale.getDefault()));
        createVerificationTokenInputData.setUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        return createVerificationTokenInputData;
    }

}
