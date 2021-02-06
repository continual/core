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
import java.util.List;
import java.util.Map;

import io.continual.util.collections.MultiMap;

/**
 * An exception signaling that a form is invalid. It carries specific problems
 * (and warnings) by field name, and separately for the overall form. It's intended
 * to be caught and used to report problems back to the user.
 */
public class CHttpInvalidFormException extends Exception
{
    public CHttpInvalidFormException ()
	{
		super ( "Form validation errors" );
		fFieldProblems = new MultiMap<>();
		fFieldWarnings = new MultiMap<>();
		fFormProblems = new LinkedList<>();
	}

	/**
	 * Add a general problem.
	 * @param problem
	 */
	public CHttpInvalidFormException addProblem ( String problem )
	{
		fFormProblems.add ( problem );
		return this;
	}

	/**
	 * Add a problem with a specific field.
	 * @param field
	 * @param problem
	 */
	public CHttpInvalidFormException addProblem ( String field, String problem )
	{
		fFieldProblems.put ( field, problem );
		return this;
	}

	/**
	 * Add a warning for a specific field.
	 * @param field
	 * @param problem
	 */
	public CHttpInvalidFormException addWarning ( String field, String problem )
	{
		fFieldWarnings.put ( field, problem );
		return this;
	}

	/**
	 * Copy problems from another invalid form exception.
	 * @param that
	 */
	public CHttpInvalidFormException addProblemsFrom ( CHttpInvalidFormException that )
	{
		fFormProblems.addAll ( that.getFormProblems () );
		fFieldProblems.putAll ( that.getFieldProblems () );
		return this;
	}

	/**
	 * Get the count of problems.
	 * @return the count of problems.
	 */
	public int size ()
	{
		return fFieldProblems.size () + fFormProblems.size ();
	}

	/**
	 * Get all field problems.
	 * @return a map from field name to a list of problem strings
	 */
	public Map<String,List<String>> getFieldProblems ()
	{
		return fFieldProblems.getValues ();
	}

	/**
	 * Get problems on a particular field.
	 * @param field
	 * @return a list of 0 or more problems.
	 */
    public List<String> getProblemsOn ( String field )
	{
		final LinkedList<String> list = new LinkedList<>();
		final List<String> vals = fFieldProblems.get ( field );
		if ( vals != null )
		{
			list.addAll ( vals );
		}
		return list;
	}
	
	/**
	 * Get all field warnings.
	 * @return a map from field name to a list of warning strings
	 */
	public Map<String,List<String>> getFieldWarnings ()
	{
		return fFieldWarnings.getValues ();
	}

	/**
	 * Get the form-level problems.
	 * @return a list of form problems
	 */
	public List<String> getFormProblems ()
	{
		return fFormProblems;
	}

	private final LinkedList<String> fFormProblems; 
	private MultiMap<String,String> fFieldProblems;
	private MultiMap<String,String> fFieldWarnings;
	private static final long serialVersionUID = 1L;
}
