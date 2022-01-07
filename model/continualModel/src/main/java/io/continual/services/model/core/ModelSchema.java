package io.continual.services.model.core;

import java.util.LinkedList;
import java.util.List;

import io.continual.services.model.core.exceptions.ModelServiceException;

public interface ModelSchema
{
	interface ValidationResult
	{
		boolean isValid ();
		List<String> getProblems ();
	}

	static ValidationResult buildPassingResult () { return new ValidationResult() {

		@Override
		public boolean isValid () { return true; }

		@Override
		public List<String> getProblems () { return new LinkedList<> (); }
	}; }
	
	ValidationResult isValid ( ModelObject object ) throws ModelServiceException;
}
