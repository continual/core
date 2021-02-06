package io.continual.services.processor.engine.library.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONObject;

import io.continual.services.processor.config.readers.ConfigLoadContext;
import io.continual.services.processor.service.SimpleProcessingService;
import io.continual.util.data.json.CommentedJsonTokener;

public class SimpleKeyStore extends SimpleProcessingService
{
	public SimpleKeyStore ( ConfigLoadContext sc, JSONObject config )
	{
		this ( new File ( config.getString ( "file" ) ) );
	}

	public SimpleKeyStore ( File storage )
	{
		fStorage = storage;
		readStorage ();
	}

	public String getString ( String key, String defval )
	{
		return fData.optString ( key, defval );
	}

	public void put ( String key, String val  )
	{
		fData.put ( key, val );
		writeStorage ();
	}

	public long getLong ( String key, long defval )
	{
		return fData.optLong ( key, defval );
	}

	public void put ( String key, long val  )
	{
		fData.put ( key, val );
		writeStorage ();
	}

	private void readStorage ()
	{
		try ( final FileInputStream fis = new FileInputStream ( fStorage ) )
		{
			fData = new JSONObject ( new CommentedJsonTokener ( fis )  );
		}
		catch ( IOException x )
		{
			fData = new JSONObject ();
		}
	}
	
	private void writeStorage ()
	{
		try ( final FileOutputStream fos = new FileOutputStream ( fStorage ) )
		{
			fos.write ( fData.toString ().getBytes () );
		}
		catch ( IOException x )
		{
			fData = new JSONObject ();
		}
	}

	private final File fStorage;
	private JSONObject fData;
}
