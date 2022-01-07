package io.continual.services.model.service;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;

public interface ModelSessionBuilder
{
	ModelSessionBuilder forUser ( Identity user );
	
	ModelSession build () throws IamSvcException, BuildFailure;
}
