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

import io.continual.http.app.htmlForms.CHttpFormPostWrapper.ParseException;
import io.continual.http.service.framework.context.CHttpRequestContext;

/**
 * A form validation step.
 */
public interface CHttpFormValidationStep
{
	/**
	 * <p>Given a context, form, and field information, decide if the value is valid. If it is not,
	 * add an error listing for the field to the supplied validation error. (Once all
	 * validation steps are complete, if the error object contains any errors, it's thrown as
	 * an exception, indicating a problem with the form submission.)</p>
	 * 
	 * <p>For form-level validation, the field argument is null.</p>
	 * 
	 * @param context
	 * @param form
	 * @param field Field info, or null for form-level validation.
	 * @param err
	 * @throws ParseException 
	 */
	void validate ( CHttpRequestContext context, CHttpFormPostWrapper form, CHttpFormFieldInfo field, CHttpInvalidFormException err ) throws ParseException;
}
