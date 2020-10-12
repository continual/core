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

package io.continual.services.model.api.endpoints;

import java.io.IOException;

import org.json.JSONException;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelService;


/**
 * The date API provides the current servers date/time
 */
public class IndexApi extends ModelApiContextHelper
{
	public IndexApi ( ModelService ms )
	{
		super ( ms );
	}

	public void getModelIndexes ( CHttpRequestContext context, final String acctId, final String modelName ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
				return null;
			}
		} );
	}

	public void getModelIndex ( CHttpRequestContext context, final String acctId, final String modelName, final String indexName ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
				return null;
			}
		} );
	}

	public void createModelIndex ( CHttpRequestContext context, final String acctId, final String modelName, final String indexName ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
				return null;
			}
		} );
	}

	public void dropModelIndex ( CHttpRequestContext context, final String acctId, final String modelName, final String indexName ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, acctId, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				sendStatusCodeAndMessage ( context, HttpStatusCodes.k501_notImplemented, "not implemented" );
				return null;
			}
		} );
	}
}
