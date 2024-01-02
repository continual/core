package io.continual.services.model.impl.json;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.json.JSONObject;

import io.continual.services.model.impl.json.CommonJsonDbObject.Builder.Constructor;
import io.continual.util.naming.Path;

public class CommonJsonDbObjectContainer extends CommonJsonDbObject
{
	public static CommonJsonDbObjectContainer createObjectContainer ( String id, Path containedPath )
	{
		return createObjectContainer ( id, Collections.singletonList ( containedPath ) );
	}

	public static CommonJsonDbObjectContainer createObjectContainer ( String id, Path... containedPaths )
	{
		return createObjectContainer ( id, Arrays.asList ( containedPaths ) );
	}

	public static CommonJsonDbObjectContainer createObjectContainer ( String id, Collection<Path> containedPaths )
	{
		return new Builder<CommonJsonDbObjectContainer> ()
			.withId ( id )
//			.withData ( new JSONObject ()
//				.put ( "objects", JsonVisitor.collectionToArray ( containedPaths, new ItemRenderer<Path,String> ()
//				{
//					@Override
//					public String render ( Path containedPath )
//					{
//						return containedPath.toString ().substring ( 1 );
//					}
//				} ) ), true )
			.withType ( CommonJsonDbObject.kStdType_ObjectContainer )
			.constructUsing ( new Constructor<CommonJsonDbObjectContainer> ()
			{
				@Override
				public CommonJsonDbObjectContainer construct ( String id, JSONObject rawData )
				{
					return new CommonJsonDbObjectContainer ( id, rawData );
				}
			} )
			.build ()
		;
	}

	private CommonJsonDbObjectContainer ( String id, JSONObject rawData )
	{
		super ( id, rawData );
	}
}
