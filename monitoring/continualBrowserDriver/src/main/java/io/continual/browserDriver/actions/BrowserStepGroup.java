package io.continual.browserDriver.actions;

import java.io.ByteArrayInputStream;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.browserDriver.ActionContext;
import io.continual.browserDriver.BrowserAction;
import io.continual.browserDriver.BrowserRunner;
import io.continual.builder.Builder.BuildFailure;
import io.continual.util.nv.NvReadable;

public class BrowserStepGroup extends StdBrowserAction implements BrowserAction
{
	public static BrowserStepGroup fromJson ( JSONObject t, NvReadable settings ) throws JSONException, BuildFailure
	{
		return new BrowserStepGroup ( t, settings );
	}

	public BrowserStepGroup ( JSONObject t, NvReadable settings ) throws JSONException, BuildFailure
	{
		super ( t, settings );

		fRunner = BrowserRunner.build (
			new ByteArrayInputStream ( t.toString ().getBytes () ),
			settings
		);
	}

	@Override
	public void act ( ActionContext ctx ) throws BrowserActionFailure
	{
		fRunner.run ( ctx );
	}

	private final BrowserRunner fRunner;
}
