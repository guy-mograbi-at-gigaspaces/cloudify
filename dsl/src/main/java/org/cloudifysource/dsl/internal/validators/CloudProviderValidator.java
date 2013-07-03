package org.cloudifysource.dsl.internal.validators;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang.StringUtils;
import org.cloudifysource.dsl.DSLValidation;
import org.cloudifysource.dsl.cloud.CloudProvider;
import org.cloudifysource.dsl.internal.DSLValidationContext;
import org.cloudifysource.dsl.internal.DSLValidationException;
import org.openspaces.maven.support.OutputVersion;

import com.j_spaces.kernel.PlatformVersion;

public class CloudProviderValidator implements DSLValidator {

	private CloudProvider entity;

	@Override
	public void setDSLEntity(Object dslEntity) {
		this.entity = (CloudProvider) dslEntity;
		
	}
	
	@DSLValidation
	public void validateProviderName(final DSLValidationContext validationContext)
			throws DSLValidationException {

		if (StringUtils.isBlank(entity.getProvider())) {
			throw new DSLValidationException("Provider cannot be empty");
		}
	}

	@DSLValidation
	public void validateCloudifyUrl(final DSLValidationContext validationContext)
			throws DSLValidationException {

		/*String[] schema = {"http"};
		UrlValidator urlValidator = new UrlValidator(schema);
		if (!urlValidator.isValid(cloudifyUrl)) {
			throw new DSLValidationException("Invalid cloudify url: \"" + cloudifyUrl + "\"");
		}*/

		try {
	        new URI(entity.getCloudifyUrl());
		} catch (URISyntaxException e) {
			throw new DSLValidationException("Invalid cloudify url: \"" + entity.getCloudifyUrl() + "\"");
		}

		//TODO request "head" to see if the url is accessible. If not - warning.
	}

	@DSLValidation
	public void validateNumberOfManagementMachines(final DSLValidationContext validationContext)
			throws DSLValidationException {

		if (entity.getNumberOfManagementMachines() != 1 && entity.getNumberOfManagementMachines() != 2) {
			throw new DSLValidationException("Invalid numberOfManagementMachines: \"" + entity.getNumberOfManagementMachines()
					+ "\". Valid values are 1 or 2");
		}

		//TODO request "head" to see if the url is accessible. If not - warning.
	}

	@DSLValidation
	public void validateSshLoggingLevel(final DSLValidationContext validationContext)
			throws DSLValidationException {

		if (!entity.getSshLoggingLevel().matches("INFO|FINE|FINER|FINEST|WARNING|DEBUG")) {
			throw new DSLValidationException("sshLoggingLevel \"" + entity.getSshLoggingLevel() + "\" is invalid, "
					+ "supported values are: INFO, FINE, FINER, FINEST, WARNING, DEBUG");
		}
	}

	
	/**
	 * This is a unique situation: we need a dependency on openspaces to generate the 
	 * cloudify URL. in this case we assign the URL in this validation method.
	 * 
	 */
	@DSLValidation
	public void validateCloudifyUrlAccordingToPlatformVersion() {
		if (StringUtils.isEmpty(entity.getCloudifyUrl())) {
			String cloudifyUrlPattern;
			String productUri;
			String editionUrlVariable;

			if (PlatformVersion.getEdition().equalsIgnoreCase(PlatformVersion.EDITION_CLOUDIFY)) {
				productUri = "org/cloudifysource";
				editionUrlVariable = "cloudify";
				cloudifyUrlPattern = "http://repository.cloudifysource.org/"
						+ "%s/" + OutputVersion.computeCloudifyVersion() + "/gigaspaces-%s-"
						+ PlatformVersion.getVersion() + "-" + PlatformVersion.getMilestone()
						+ "-b" + PlatformVersion.getBuildNumber();
				entity.setCloudifyUrl(String.format(cloudifyUrlPattern, productUri, editionUrlVariable));
			} else {
				productUri = "com/gigaspaces/xap";
				editionUrlVariable = "xap-premium";
				cloudifyUrlPattern = "http://repository.cloudifysource.org/"
						+ "%s/" + OutputVersion.computeXapVersion() + "/gigaspaces-%s-"
						+ PlatformVersion.getVersion() + "-" + PlatformVersion.getMilestone()
						+ "-b" + PlatformVersion.getBuildNumber();
				entity.setCloudifyUrl(String.format(cloudifyUrlPattern, productUri, editionUrlVariable));
			}
		}
	}

}
