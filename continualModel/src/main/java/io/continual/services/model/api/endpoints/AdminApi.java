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
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.http.service.framework.context.CHttpRequestContext;
import io.continual.http.util.http.standards.HttpStatusCodes;
import io.continual.services.model.core.exceptions.ModelServiceRequestException;
import io.continual.services.model.service.ModelAccount;
import io.continual.services.model.service.ModelService;
import io.continual.util.data.json.CommentedJsonTokener;
import io.continual.util.data.json.JsonVisitor;

public class AdminApi extends ModelApiContextHelper
{
	public AdminApi ( ModelService ms )
	{
		super ( ms );
	}

	public void getAccountList ( CHttpRequestContext context ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, null, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				final ModelService ms = modelApiContext.getModelService ();
				final List<String> accts = ms.getAccounts (
					modelApiContext.getModelRequestContext ()
				);

				// return the result
				return new JSONObject ()
					.put ( "status", HttpStatusCodes.k200_ok )
					.put ( "accounts", JsonVisitor.listToArray ( accts ) )
					.toString ()
				;
			}
		} );
	}

	public void getAccount ( CHttpRequestContext context, final String acctId ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, null, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				final ModelService ms = modelApiContext.getModelService ();
				final ModelAccount ma = ms.getAccount ( modelApiContext.getModelRequestContext (), acctId );

				if ( ma == null )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "the account does not exist" );
					return null;
				}
				
				// return the result
				return new JSONObject ()
					.put ( "account", ma.toJson () )
					.toString ()
				;
			}
		} );
	}

	public void createAccount ( CHttpRequestContext context, final String acctId ) throws IOException, ModelServiceRequestException
	{
		handleModelAdminRequest ( context, null, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				final ModelService ms = modelApiContext.getModelService ();
//				final ModelAccount ma = ms.getAccount ( modelApiContext.getModelRequestContext (), acctId );
//				if ( ma != null )
//				{
//					sendStatusCodeAndMessage ( context, HttpStatusCodes.k406_notAcceptable, "The account exists. Use PATCH or explicitly DELETE it." );
//					return null;
//				}

				final JSONObject inbound = new JSONObject ( new CommentedJsonTokener ( context.request ().getBodyStream () ) );
				final String ownerId = inbound.optString ( "owner", acctId );

				// create the account
				final ModelAccount acctData = ms.createAccount ( modelApiContext.getModelRequestContext (), acctId, ownerId );

				if ( acctData == null )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k500_internalServerError, "the account was not created" );
					return null;
				}
				
				// return the result
				return new JSONObject ()
					.put ( "account", acctData.toJson () )
					.toString ()
				;
			}
		} );
	}

	public void patchAccount ( CHttpRequestContext context, final String acctId ) throws IOException, ModelServiceRequestException
	{
		handleModelRequest ( context, null, null, new ModelApiHandler ()
		{
			@Override
			public String handle ( ModelApiContext modelApiContext )
				throws IOException, JSONException, ModelServiceRequestException
			{
				final ModelService ms = modelApiContext.getModelService ();
				final ModelAccount ma = ms.getAccount ( modelApiContext.getModelRequestContext (), acctId );

				if ( ma == null )
				{
					sendStatusCodeAndMessage ( context, HttpStatusCodes.k404_notFound, "the account does not exist" );
					return null;
				}

				// FIXME:...
				
				// return the result
				return new JSONObject ()
					.put ( "account", ma.toJson () )
					.toString ()
				;
			}
		} );
	}
}
