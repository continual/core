package io.continual.services.model.impl.json;

import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONObject;

import io.continual.iam.access.AccessControlList;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ItemRenderer;
import io.continual.util.naming.Path;

public class CommonJsonDbObjectContainer
{
	public static final String kContainerType = "objectContainer";

	public static CommonDataTransfer createObjectContainer ( Path path, Collection<Path> containedPaths )
	{
		final JSONObject data = new JSONObject ();
		data.put ( "objects", JsonVisitor.collectionToArray ( containedPaths, new ItemRenderer<Path,String> ()
		{
			@Override
			public String render ( Path containedPath )
			{
				return containedPath.toString ().substring ( 1 );
			}
		} ) );

		final JSONObject meta = new JSONObject ()
			.put ( CommonModelObjectMetadata.kMeta_AclTag, AccessControlList.createOpenAcl ().asJson () )
			.put ( CommonModelObjectMetadata.kMeta_LockedTypes, new JSONArray ().put ( kContainerType ) )
		;

		final JSONObject topLevel = new JSONObject ()
			.put ( CommonDataTransfer.kDataTag, data )
			.put ( CommonDataTransfer.kMetaTag, meta )
		;

		return new CommonDataTransfer ( path, topLevel );
	}
}
