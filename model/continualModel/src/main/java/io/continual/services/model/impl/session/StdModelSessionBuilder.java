package io.continual.services.model.impl.session;

import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.exceptions.IamSvcException;
import io.continual.iam.identity.Identity;
import io.continual.services.model.core.ModelNotificationService;
import io.continual.services.model.session.ModelSession;
import io.continual.services.model.session.ModelSessionBuilder;
import io.continual.util.data.json.JsonUtil;

public class StdModelSessionBuilder implements ModelSessionBuilder
{
	@Override
	public ModelSessionBuilder forUser ( Identity user )
	{
		fUser = user;
		return this;
	}

	@Override
	public ModelSessionBuilder readingSettingsFrom ( JSONObject data )
	{
		fSettings = JsonUtil.clone ( data );
		return this;
	}

	public ModelSessionBuilder withNotificationsTo ( ModelNotificationService svc )
	{
		fNotifications = svc;
		return this;
	}
	
	@Override
	public ModelSession build () throws IamSvcException, BuildFailure
	{
		return new StdModelSession ( this );
	}

	Identity getUser () { return fUser; }
	JSONObject getSettings () { return fSettings; }
	ModelNotificationService getNotificationSvc () { return fNotifications; } 

	private Identity fUser = null;
	private JSONObject fSettings = new JSONObject ();
	private ModelNotificationService fNotifications = new NoopNotifier ();
}
