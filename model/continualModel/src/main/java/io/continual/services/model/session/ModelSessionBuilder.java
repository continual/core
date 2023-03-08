package io.continual.services.model.session;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;

public interface ModelSessionBuilder
{
	/**
	 * Set the user for the model session
	 * @param user
	 * @return this session builder
	 */
	ModelSessionBuilder forUser ( Identity user );

	/**
	 * Set the settings for the session.
	 * @param data
	 * @return this session builder
	 */
	ModelSessionBuilder readingSettingsFrom ( JSONObject data );
	
	/**
	 * Build the session
	 * @return a session instance
	 * @throws IamSvcException
	 * @throws BuildFailure
	 */
	ModelSession build () throws IamSvcException, BuildFailure;
}
