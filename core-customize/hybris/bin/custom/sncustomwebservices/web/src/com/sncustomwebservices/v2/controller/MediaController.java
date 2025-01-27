/**
 *
 */
package com.sncustomwebservices.v2.controller;

import de.hybris.platform.cmsfacades.dto.MediaFileDto;
import de.hybris.platform.cmsfacades.exception.ValidationException;
import de.hybris.platform.cmsfacades.media.MediaFacade;
import de.hybris.platform.cmsoccaddon.data.MediaWsDTO;
import de.hybris.platform.cmswebservices.data.MediaData;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.sncustomwebservices.facades.CustomProductFacade;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * @author Anand.Mund
 *
 */

@Controller
@Tag(name = "Media")
@RequestMapping(value = "/{baseSiteId}/media")
@CacheControl(directive = CacheControlDirective.PRIVATE)
public class MediaController extends BaseController {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaController.class);

	@Resource
	private MediaFacade mediaFacade;
	@Resource
	private DataMapper dataMapper;
	@Resource
	private CustomProductFacade oicProductFacade;


	@RequestMapping(value = "/uploadImage", method = RequestMethod.POST/*, consumes = MediaType.MULTIPART_FORM_DATA_VALUE*/)
	@ResponseStatus(value = HttpStatus.CREATED)
	@ResponseBody
	@Operation(operationId = "uploadImage", summary = "upload an Image")
	@ApiBaseSiteIdParam
	public MediaWsDTO uploadMediaImage(
			@Parameter(description = "The MediaData containing the data for the associated media item to be created.", required = true)
			@ModelAttribute("media") final MediaData media,
			@Parameter(description = "The unique identifier of the product for which to link the new image.", required = true)
			@RequestParam("productId")	final String productId,
			@Parameter(description = "The file representing the actual binary contents of the media to be created.", required = true)
			@RequestParam(required = true, value = "file_field_1")
			final MultipartFile multiPart
	/* final HttpServletRequest httpRequest, final HttpServletResponse httpResponse */) throws IOException
	{

		if (media.getAltText() == null || media.getAltText().isEmpty()){
			media.setAltText(media.getCode());
		}
		if (media.getDescription() == null || media.getDescription().isEmpty()){
			media.setDescription(media.getCode() + media.getMime());
		}

		de.hybris.platform.cmsfacades.data.MediaData newMedia = null;

		try{
			final de.hybris.platform.cmsfacades.data.MediaData convertedMediaData =
					getDataMapper().map(media, de.hybris.platform.cmsfacades.data.MediaData.class);


			if (multiPart.getInputStream().available() != 0 && !multiPart.getOriginalFilename().equalsIgnoreCase("file_field_1") && multiPart.getSize()>9){

				if (multiPart.getContentType().equals("application/pdf")){
					throw new RequestParameterException("Please select image file..");//WebServiceIOException("Please select image file..");

				}else {
					LOGGER.info("Uploading Image..");

					newMedia = getMediaFacade().addMedia(convertedMediaData, getFile(multiPart, multiPart.getInputStream()));

					oicProductFacade.updateImageForProduct(productId, newMedia);

					return getDataMapper().map(newMedia, MediaWsDTO.class);
				}

			}else {
				throw new RequestParameterException("Image file is missing..");//WebServiceIOException("Image file is missing..");
			}

		}catch (final ValidationException e){
			LOGGER.info("Validation exception", e);
			throw new WebserviceValidationException(e.getValidationObject());
		}
	}

	public MediaFileDto getFile(final MultipartFile file, final InputStream inputStream){

		final MediaFileDto mediaFile = new MediaFileDto();
		mediaFile.setInputStream(inputStream);
		mediaFile.setName(file.getOriginalFilename());
		mediaFile.setSize(file.getSize());
		mediaFile.setMime(file.getContentType());
		return mediaFile;
	}

	/**
	 * @return the mediaFacade
	 */
	public MediaFacade getMediaFacade()	{
		return mediaFacade;
	}

	/**
	 * @param mediaFacade the mediaFacade to set
	 */
	public void setMediaFacade(final MediaFacade mediaFacade){
		this.mediaFacade = mediaFacade;
	}

	/**
	 * @return the dataMapper
	 */
	@Override
	public DataMapper getDataMapper(){
		return dataMapper;
	}

	/**
	 * @param dataMapper the dataMapper to set
	 */
	@Override
	public void setDataMapper(final DataMapper dataMapper){
		this.dataMapper = dataMapper;
	}

	/**
	 * @return the oicProductFacade
	 */
	public CustomProductFacade getOicProductFacade(){
		return oicProductFacade;
	}

	/**
	 * @param oicProductFacade the oicProductFacade to set
	 */
	public void setOicProductFacade(final CustomProductFacade oicProductFacade){
		this.oicProductFacade = oicProductFacade;
	}




}
