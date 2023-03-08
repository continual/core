package io.continual.services.model.impl.session;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelNotificationService;
import io.continual.services.model.core.ModelSchemaRegistry;
import io.continual.services.model.impl.common.BuiltinSchemaReg;
import io.continual.services.model.impl.delegator.DelegatingModel;
import io.continual.services.model.session.ModelSession;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.naming.Path;

public class StdModelSession implements ModelSession
{
	public StdModelSession ( StdModelSessionBuilder builder ) throws IamSvcException, BuildFailure
	{
		final Identity user = builder.getUser ();
		if ( user == null )
		{
			throw new NullPointerException ( "Session user may not be null" );
		}

		fModel = new DelegatingModel ( user.getId (), "{top}" );
		fNotifications = builder.getNotificationSvc ();
		fSchemas = new BuiltinSchemaReg ( 64 );

		// build user settings from user data and builder provided settings data
		final JSONObject userSettings = JsonVisitor.mapOfStringsToObject ( user.getAllUserData () )
			.put ( "userId", user.getId () )
		;

//		try
//		{
//			final ModelRequestContext mrc = fModel.getRequestContextBuilder ()
//				.forUser ( user )
//				.build ()
//			;
//			final Path userSettingsPath = Path.getRootPath()
//				.makeChildItem ( Name.fromString ( "settings" ) )
//				.makeChildItem ( Name.fromString ( user.getId () ) )
//			;
//			fModel.store ( mrc, userSettingsPath, userSettings );
//		}
//		catch ( ModelRequestException | ModelServiceException e )
//		{
//			throw new BuildFailure ( e );
//		}

		JsonVisitor.forEachElement ( builder.getSettings ().optJSONArray ( "models" ), new ArrayVisitor<JSONObject,BuildFailure> ()
		{
			@Override
			public boolean visit ( JSONObject model ) throws JSONException, BuildFailure
			{
				fModel.mount ( mountFromJson ( model, userSettings ) );
				return true;
			}
		} );
	}

	private StdMountTableEntry mountFromJson ( JSONObject mountConfig, JSONObject userSettings ) throws BuildFailure
	{
		final JSONObject userConfig = JsonUtil.clone ( mountConfig.getJSONObject ( "model" ) )
			.put ( "UserData", userSettings )
		;

		final Model m = Builder.withBaseClass ( Model.class )
			.withClassNameInData ()
			.usingData ( userConfig )
			.providingContext ( new ServiceContainer() )
			.build ()
		;
		return new StdMountTableEntry ( Path.fromString ( mountConfig.getString ( "path" ) ), m );
	}
	
	@Override
	public Model getModel ()
	{
		return fModel;
	}

	@Override
	public ModelSchemaRegistry getSchemaRegistry ()
	{
		return fSchemas;
	}

	@Override
	public ModelNotificationService getNotificationSvc ()
	{
		return fNotifications;
	}

	private final DelegatingModel fModel;
	private final ModelNotificationService fNotifications;
	private final BuiltinSchemaReg fSchemas;
}
