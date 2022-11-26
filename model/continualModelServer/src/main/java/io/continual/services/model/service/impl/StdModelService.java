package io.continual.services.model.service.impl;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder;
import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.IamServiceManager;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelNotificationService;
import io.continual.services.model.core.ModelObject;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.ModelSchemaRegistry;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.common.BuiltinSchemaReg;
import io.continual.services.model.impl.delegator.DelegatingModel;
import io.continual.services.model.service.ModelService;
import io.continual.services.model.service.ModelSession;
import io.continual.services.model.service.ModelSessionBuilder;
import io.continual.util.collections.LruCache;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ArrayVisitor;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

public class StdModelService extends SimpleService implements ModelService
{
	public StdModelService ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		fAccts = sc.get ( config.optString ( "accounts", "accounts" ), IamServiceManager.class );
		if ( fAccts == null )
		{
			throw new BuildFailure ( "An accounts service is required. Set to 'accounts' the object name or use the default 'accounts'." );
		}

		fSettingsModel = sc.get ( config.optString ( "settings", "settingsModel" ), Model.class );
		if ( fSettingsModel == null )
		{
			throw new BuildFailure ( "A settings model is required. Set to 'settings' the object name, or define a 'settingsModel' instance." );
		}

		try
		{
			final String systemUser = config.optString ( "systemUser", "system" );
			
			final ModelRequestContext mrc = fSettingsModel.getRequestContextBuilder ()
				.forUser ( new CommonJsonIdentity ( systemUser, CommonJsonIdentity.initializeIdentity (), null ) )	// fake user acct
				.build ()
			;

			final Path gmPath = Path.fromString ( "/globalMounts" );
			if ( !fSettingsModel.exists ( mrc, gmPath ) )
			{
				fSettingsModel.store ( mrc, gmPath, new JSONObject () );
			}

			final ModelObject globalMounts = fSettingsModel.load ( mrc, gmPath );

			fGlobalMounts = new LinkedList<> ();
			JsonVisitor.forEachElement ( globalMounts.getData ().optJSONArray ( "globalMounts" ), new ArrayVisitor<JSONObject,BuildFailure> ()
			{
				@Override
				public boolean visit ( JSONObject globalMount ) throws JSONException, BuildFailure
				{
					fGlobalMounts.add ( globalMount );
					return true;
				}
			} );

			fGlobalSchemas = new BuiltinSchemaReg ( 1024 );

			// wire up notification service
			ModelNotificationService notifier = new NoopNotifier ();
			final String notifierName = config.optString ( "notifier", null );
			if ( notifierName != null )
			{
				notifier = sc.get ( notifierName, ModelNotificationService.class );
			}
			fNotifications = notifier;
			if ( fNotifications == null )	// set attempted and failed
			{
				throw new BuildFailure ( "No notification service connected." );
			}
		}
		catch ( ModelRequestException | ModelServiceException x )
		{
			throw new BuildFailure ( x );
		}

		fSessions = new LruCache<String,StdModelSession> ( config.optLong ( "sessionCacheSize", 1024 ) );
	}

	@Override
	public StdModelSessionBuilder sessionBuilder ()
	{
		return new StdModelSessionBuilder ();
	}

	private class StdModelSessionBuilder implements ModelSessionBuilder
	{
		@Override
		public ModelSessionBuilder forUser ( Identity user )
		{
			fUser = user;
			return this;
		}

		@Override
		public ModelSession build () throws IamSvcException, BuildFailure
		{
			if ( fUser == null )
			{
				throw new NullPointerException ( "user may not be null" );
			}
			
			StdModelSession result = fSessions.get ( fUser.getId () );
			if ( result == null )
			{
				result = new StdModelSession ( this );
				fSessions.put ( fUser.getId (), result );
			}
			return result;
		}

		private Identity fUser;
	}

	private class StdModelSession implements ModelSession
	{
		public StdModelSession ( StdModelSessionBuilder builder ) throws IamSvcException, BuildFailure
		{
			fModel = new DelegatingModel ( builder.fUser.getId (), "{top}" );

			// user config data
			final JSONObject userSettings = JsonVisitor.mapOfStringsToObject ( builder.fUser.getAllUserData () )
				.put ( "userId", builder.fUser.getId () )
			;

			final ModelRequestContext mrc = fSettingsModel.getRequestContextBuilder ()
				.forUser ( new CommonJsonIdentity ( "system", new JSONObject (), null ) )
				.build ()
			;
			final Path userSettingsPath = Path.getRootPath()
				.makeChildItem ( Name.fromString ( "userSettings" ) )
				.makeChildItem ( Name.fromString ( builder.fUser.getId () ) )
			;

			try
			{
				if ( !fSettingsModel.exists ( mrc, userSettingsPath ) )
				{
					fSettingsModel.store ( mrc, userSettingsPath, new JSONObject () );
				}
				final ModelObject userSettingsObject = fSettingsModel.load ( mrc, userSettingsPath );
				JsonVisitor.forEachElement ( userSettingsObject.getData ().optJSONArray ( "models" ), new ArrayVisitor<JSONObject,BuildFailure> ()
				{
					@Override
					public boolean visit ( JSONObject model ) throws JSONException, BuildFailure
					{
						fModel.mount ( mountFromJson ( model, userSettings ) );
						return true;
					}
				} );
			}
			catch ( ModelServiceException | ModelRequestException x )
			{
				throw new BuildFailure ( x );
			}

			// add the global mounts after the user mounts
			for ( JSONObject globalMountConfig : fGlobalMounts )
			{
				fModel.mount ( mountFromJson ( globalMountConfig, userSettings ) );
			}
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
			return fGlobalSchemas;
		}

		@Override
		public ModelNotificationService getNotificationSvc ()
		{
			return fNotifications;
		}

		private final DelegatingModel fModel;
	}

	private final IamServiceManager<?,?> fAccts;
	private final Model fSettingsModel;
	private final LinkedList<JSONObject> fGlobalMounts;
	private final BuiltinSchemaReg fGlobalSchemas;
	private final ModelNotificationService fNotifications;
	private final LruCache<String,StdModelSession> fSessions;
}
