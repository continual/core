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

import java.util.LinkedList;

import io.continual.http.app.htmlForms.CHttpFormPostWrapper.ParseException;
import io.continual.http.service.framework.context.CHttpRequestContext;

/**
 * Information about a form field.
 */
public class CHttpFormFieldInfo
{
	/**
	 * Construct field info starting with a field name.
	 * @param fn
	 */
	public CHttpFormFieldInfo ( String fn )
	{
		fFieldName = fn;
		fSteps = new LinkedList<>();
	}

	/**
	 * validate form input for this field, given a request context, a form wrapper,
	 * and an exception to populate (which, if it has a size() &gt; 0, will be thrown by
	 * the validator).
	 * 
	 * @param context the request context
	 * @param form the form wrapper
	 * @param err the exception to populate with problems
	 * @throws ParseException 
	 */
	public void validate ( CHttpRequestContext context, CHttpFormPostWrapper form, CHttpInvalidFormException err ) throws ParseException
	{
		for ( CHttpFormValidationStep step : fSteps )
		{
			step.validate ( context, form, this, err );
		}
	}

	/**
	 * Setup validation with a validation step.
	 * @param step the validation step
	 * @return this
	 */
	public CHttpFormFieldInfo validateWith ( CHttpFormValidationStep step )
	{
		fSteps.add ( step );
		return this;
	}

	/**
	 * Note that this field is required. The error message is used when the field
	 * is missing to populate the validation exception.
	 * 
	 * @param errMsg the error message
	 * @return this
	 */
	public CHttpFormFieldInfo required ( final String errMsg )
	{
		return validateWith ( new CHttpFormValidationStep ()
		{
			@Override
			public void validate ( CHttpRequestContext context, CHttpFormPostWrapper form, CHttpFormFieldInfo field, CHttpInvalidFormException err ) throws ParseException
			{
				if ( !form.hasParameter ( fFieldName ) )
				{
					err.addProblem ( fFieldName, errMsg );
				}
				else
				{
					final String val = form.getValue ( fFieldName );
					if ( val != null && val.length () == 0 )
					{
						err.addProblem ( fFieldName, errMsg );
					}
				}
			}
		} );
	}

	/**
	 * Note that the field value must be one of the given values. The error message is used
	 * to populate the validation exception. This is a case-sensitive match.
	 * @param values
	 * @param errMsg
	 * @return this
	 */
	public CHttpFormFieldInfo oneOf ( final String[] values, final String errMsg )
	{
		return oneOf ( values, true, errMsg );
	}

	/**
	 * Note that the field value must be one of the given values. The error message is used
	 * to populate the validation exception. The comparison are case sensitive based on the
	 * caseSensitive argument.
	 * @param values
	 * @param caseSensitive
	 * @param errMsg
	 * @return this
	 */
	public CHttpFormFieldInfo oneOf ( final String[] values, final boolean caseSensitive, final String errMsg )
	{
		return validateWith ( new CHttpFormValidationStep ()
		{
			@Override
			public void validate ( CHttpRequestContext context, CHttpFormPostWrapper form, CHttpFormFieldInfo field, CHttpInvalidFormException err ) throws ParseException
			{
				if ( !form.hasParameter ( fFieldName ) )
				{
					err.addProblem ( fFieldName, errMsg );
				}
				else
				{
					final String val = form.getValue ( fFieldName );

					boolean found = false;
					for ( String v : values )
					{
						if (( caseSensitive && v.equals ( val ) ) ||
							( !caseSensitive && v.equalsIgnoreCase ( val ) ) )
						{
							found = true;
							break;
						}
					}
					
					if ( !found )
					{
						err.addProblem ( fFieldName, errMsg );
					}
				}
			}
		} );
	}

	/**
	 * Note that the field value must be one of the given values. The objects are
	 * converted to strings (via toString) before the comparison, and the comparison
	 * is case sensitive.
	 * 
	 * @param values
	 * @param errMsg
	 * @return this
	 */
	public CHttpFormFieldInfo oneOf ( final Object[] values, final String errMsg )
	{
		int current = 0;
		final String[] stringVals = new String [ values.length ];
		for ( Object o : values )
		{
			stringVals [ current++ ] = o.toString ();
		}
		return oneOf ( stringVals, errMsg );
	}

	/**
	 * Note that this field must match the given regular expression. The error message
	 * is used to populate the validation exception when the match fails.
	 * @param regex
	 * @param errMsg
	 * @return this
	 */
	public CHttpFormFieldInfo matches ( final String regex, final String errMsg )
	{
		return validateWith ( new CHttpFormValidationStep ()
		{
			@Override
			public void validate ( CHttpRequestContext context, CHttpFormPostWrapper form, CHttpFormFieldInfo field, CHttpInvalidFormException err ) throws ParseException
			{
				String value = form.getValue ( fFieldName );
				if ( value == null )
				{
					value = "";
				}
				if ( !value.matches ( regex ) )
				{
					err.addProblem ( fFieldName, errMsg );
				}
			}
		} );
	}

	/**
	 * Provide this field with a default value that's used when the form does not
	 * contain a value. Note that validation steps are run in the order they're
	 * created, so using required() before defaultValue() is probably not what
	 * you'd want.
	 * 
	 * @param defVal
	 * @return this
	 */
	public CHttpFormFieldInfo defaultValue ( final String defVal )
	{
		return validateWith ( new CHttpFormValidationStep ()
		{
			@Override
			public void validate ( CHttpRequestContext context, CHttpFormPostWrapper form, CHttpFormFieldInfo field, CHttpInvalidFormException err ) throws ParseException
			{
				if ( !form.hasParameter ( fFieldName ) ||
					( form.isFormField ( fFieldName ) && form.getValue ( fFieldName ).length () == 0 ) )
				{
					form.changeValue ( fFieldName, defVal );
				}
			}
		} );
	}

	/**
	 * Provide a default value for this field. Note that the object is converted to a string.
	 * (This is purely a convenience method.)
	 * @param o
	 * @return this
	 */
	public CHttpFormFieldInfo defaultValue ( final Object o )
	{
		return defaultValue ( o.toString () );
	}

	/**
	 * Note the field value must be one of a set of known boolean equivalent strings:
	 * 	true/false, yes/no, on/off, 1/0, and checked.
	 * @param errMsg
	 * @return
	 */
	public CHttpFormFieldInfo isBoolean ( final String errMsg )
	{
		return oneOf ( new String[] { "true", "false", "yes", "no", "on", "off", "1", "0", "checked" }, false, errMsg );
	}

	public final String fFieldName;
	private LinkedList<CHttpFormValidationStep> fSteps;
}
