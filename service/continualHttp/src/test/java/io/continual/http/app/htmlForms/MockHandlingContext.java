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

import io.continual.http.service.framework.CHttpConnection;
import io.continual.http.service.framework.CHttpServlet;
import io.continual.http.service.framework.context.CHttpRequestContext;

public class MockHandlingContext<C extends CHttpConnection> extends CHttpRequestContext
{
	public MockHandlingContext ()
	{
		super ( new mockWebServlet (), null, null, null, null, null );
	}
	
	private static class mockWebServlet extends CHttpServlet
	{
		public mockWebServlet ()
		{
		}
		private static final long serialVersionUID = 1L;
	}
}
