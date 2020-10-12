/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.http.app.htmlForms;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import io.continual.http.app.htmlForms.CHttpFormPostWrapper.ParseException;
import io.continual.http.service.framework.context.CHttpRequestContext;

/**
 * A form validation tool.
 */
public class CHttpFormValidator
{
	/**
	 * Construct an empty form validator.
	 */
	public CHttpFormValidator ()
	{
		this ( null );
	}

	/**
	 * Construct a form validator using another form validator as a starting point.
	 * This is useful for "wizard" UIs that carry form data along through each step.
	 * The validator for step 2 is based on the validator for step 1, step 3 is based
	 * on step 2, etc.
	 * 
	 * @param wrapped
	 */
	public CHttpFormValidator ( CHttpFormValidator wrapped )
	{
		fMap = new HashMap<>();
		fValidators = new LinkedList<>();

		if ( wrapped != null )
		{
			addValidation ( new wrapper ( wrapped ) );
		}
	}

	/**
	 * <p>Validate the given form (referenced by the form wrapper) using the validators
	 * registered on this object. Validation is done at the field level first, then
	 * at the form level.</p>
	 * 
	 * <p>Validation problems are collected into an exception. If the exception instance
	 * contains any errors after all validation steps complete, it's thrown.</p> 
	 * 
	 * @param context
	 * @param w
	 * @throws CHttpInvalidFormException if a validation step fails
	 */
	public void validate ( CHttpRequestContext context, CHttpFormPostWrapper w ) throws CHttpInvalidFormException
	{
		try
		{
			final CHttpInvalidFormException ve = new CHttpInvalidFormException ();
			for ( CHttpFormFieldInfo fi : fMap.values () )
			{
				fi.validate ( context, w, ve );
			}
			for ( CHttpFormValidationStep step : fValidators )
			{
				step.validate ( context, w, null, ve );
			}
			if ( ve.size () > 0 )
			{
				throw ve;
			}
		}
		catch ( ParseException e )
		{
			throw new CHttpInvalidFormException ().addProblem ( e.getMessage () );
		}
	}

	/**
	 * Get the fields known to this validator, along with their field info objects.
	 * @return a map from field name to field info
	 */
	public Map<String,CHttpFormFieldInfo> getFields ()
	{
		return fMap;
	}

	/**
	 * Get or create a field info object for a named field. Then add validation requirements
	 * to the field.
	 * @param name
	 * @return a field info object
	 */
	public CHttpFormFieldInfo field ( String name )
	{
		CHttpFormFieldInfo fi = fMap.get ( name );
		if ( fi == null )
		{
			fi = new CHttpFormFieldInfo ( name );
			fMap.put ( name, fi );
		}
		return fi;
	}

	/**
	 * Add a form-level validation step to this validator.
	 * @param step
	 */
	public void addValidation ( CHttpFormValidationStep step )
	{
		fValidators.add ( step );
	}

	private final HashMap<String,CHttpFormFieldInfo> fMap;
	private final LinkedList<CHttpFormValidationStep> fValidators;

	private class wrapper implements CHttpFormValidationStep
	{
		public wrapper ( CHttpFormValidator step )
		{
			fWrapped = step;
		}

		@Override
		public void validate ( CHttpRequestContext context, CHttpFormPostWrapper form, CHttpFormFieldInfo field, CHttpInvalidFormException err )
		{
			try
			{
				fWrapped.validate ( context, form );
			}
			catch ( CHttpInvalidFormException e )
			{
				err.addProblemsFrom ( e );
			}
		}

		private CHttpFormValidator fWrapped;
	}
}
