package io.continual.services.model.impl.json;

import org.json.JSONObject;

import io.continual.services.model.impl.json.CommonJsonDbObject.Builder.Constructor;
import io.continual.util.naming.Path;

public class CommonJsonDbAlias extends CommonJsonDbObject
{
	public static CommonJsonDbAlias createObjectContainer ( String id, Path path )
	{
		return new Builder<CommonJsonDbAlias> ()
			.withId ( id )
			.withData ( new JSONObject ()
				.put ( "path", path.toString () ), true )
			.withType ( CommonJsonDbObject.kStdType_Alias )
			.constructUsing ( new Constructor<CommonJsonDbAlias> ()
			{
				@Override
				public CommonJsonDbAlias construct ( String id, JSONObject rawData )
				{
					return new CommonJsonDbAlias ( id, rawData );
				}
			} )
			.build ()
		;
	}

	private CommonJsonDbAlias ( String id, JSONObject rawData )
	{
		super ( id, rawData );
	}
}
