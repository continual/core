package io.continual.services.model.impl.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;

import io.continual.resources.ResourceLoader;
import io.continual.services.model.core.ModelSchema;
import io.continual.services.model.core.ModelSchemaRegistry;
import io.continual.services.model.core.data.ModelDataObjectAccess;
import io.continual.services.model.core.data.ModelDataToJson;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.collections.LruCache;
import io.continual.util.data.json.CommentedJsonTokener;

public class BuiltinSchemaReg implements ModelSchemaRegistry
{
	public BuiltinSchemaReg ( long schemaCacheSize )  
	{
		fSchemaCache = new LruCache<> ( schemaCacheSize );
	}

	@Override
	public ModelSchema getSchema ( String name ) throws ModelServiceException
	{
		JsonModelSchema schema = fSchemaCache.get ( name );
		if ( schema == null )
		{
			final JSONObject schemaObj = load ( name, "io.continual.services.model.types.basic." + name );
			if ( schemaObj != null )
			{
				schema = new JsonModelSchema ( schemaObj );
				fSchemaCache.put ( name, schema );
			}
		}
		return schema;
	}

	private final LruCache<String,JsonModelSchema> fSchemaCache;

	private JSONObject load ( String... names )
	{
		for ( String name : names )
		{
			try
			{
				final InputStream is = new ResourceLoader ()
					.named ( name )
					.usingStandardSources ( false )
					.load ()
				;
				if ( is != null )
				{
					return new JSONObject ( new CommentedJsonTokener ( is ) );
				}
			}
			catch ( JSONException | IOException e )
			{
				// continue
			}
		}
		return null;
	}
	
	private class JsonModelSchema implements ModelSchema
	{
		public JsonModelSchema ( JSONObject schemaJson )
		{
			fSchema = SchemaLoader.load ( schemaJson );
		}
		
		@Override
		public ValidationResult isValid ( ModelDataObjectAccess object ) throws ModelServiceException
		{
			try
			{
				fSchema.validate ( ModelDataToJson.translate ( object ) );
				return ModelSchema.buildPassingResult ();
			}
			catch ( ValidationException e )
			{
				return new ValidationResult ()
				{
					@Override
					public boolean isValid () { return false; }

					@Override
					public List<String> getProblems ()
					{
						return e.getAllMessages ();
					}
				};
			}
		}

		private final Schema fSchema;
	}
}
