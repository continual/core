package io.continual.services.model.impl.json;

import org.json.JSONObject;

import io.continual.services.model.core.ModelObjectMetadata;
import io.continual.services.model.core.data.JsonObjectAccess;
import io.continual.services.model.core.data.ModelDataObjectAccess;
import io.continual.services.model.core.data.ModelDataToJson;
import io.continual.services.model.impl.json.CommonJsonDbModel.ModelDataTransfer;
import io.continual.util.data.json.JsonUtil;
import io.continual.util.naming.Path;

public class CommonDataTransfer implements CommonJsonDbModel.ModelDataTransfer
{
	public static JSONObject toDataObject ( ModelObjectMetadata meta, ModelDataObjectAccess data )
	{
		return new JSONObject ()
			.put ( kDataTag, ModelDataToJson.translate ( data ) )
			.put ( kMetaTag, meta.toJson () )
		;
	}

	public static JSONObject toDataObject ( ModelDataTransfer o )
	{
		return toDataObject ( o.getMetadata (), o.getObjectData () );
	}

	public CommonDataTransfer ( Path p, JSONObject fullObjectData )
	{
		if ( fullObjectData == null ) fullObjectData = new JSONObject ();

		final JSONObject objData = fullObjectData.optJSONObject ( kDataTag );
		fData = ( objData == null ? new JSONObject () : JsonUtil.clone ( objData ) );

		final JSONObject meta = fullObjectData.optJSONObject ( kMetaTag );
		if ( meta != null )
		{
			fMetadata = CommonModelObjectMetadata.asCloneOfData ( meta );
		}
		else
		{
			fMetadata = new CommonModelObjectMetadata (); 
		}
	}

	@Override
	public ModelObjectMetadata getMetadata ()
	{
		return fMetadata;
	}

	@Override
	public ModelDataObjectAccess getObjectData ()
	{
		return new JsonObjectAccess ( fData );
	}

	private final JSONObject fData;
	private final CommonModelObjectMetadata fMetadata;

	public static final String kDataTag = "data";
	public static final String kMetaTag = "meta";
}
