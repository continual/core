package io.continual.basesvcs.services.naming.std.memAndDisk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.basesvcs.services.naming.NamingIoException;
import io.continual.basesvcs.services.naming.std.BaseNameService;
import io.continual.basesvcs.services.storage.StorageInode;
import io.continual.util.naming.Path;
import io.continual.util.nv.NvReadable;

public class MemAndDiskNameService extends BaseNameService
{
	public static final String kSetting_BackingFileName = "names.memAndDisk.filename";
	private static final String kDefault_BackingFileName = "./data/names.txt";

	public static final String kSetting_FlushPeriod = "names.memAndDisk.flushPeriodMins";
	private static final long kDefault_FlushPeriod = 5;

	public MemAndDiskNameService ( NvReadable settings ) throws IOException
	{
		fMap = new HashMap<Path,StorageInode> ();
		fDirty = false;
		fFile = new File ( settings.getString ( kSetting_BackingFileName, kDefault_BackingFileName ) );

		if ( fFile.exists () )
		{
			loadFile ();
		}
		else
		{
			fFile.getParentFile().mkdirs ();
			final FileWriter fw = new FileWriter ( fFile );
			try
			{
				fw.write ( "" );
			}
			finally
			{
				fw.close ();
			}
		}

		// flush to disk periodically
		final long flushPeriodMins = settings.getLong ( kSetting_FlushPeriod, kDefault_FlushPeriod );
		fExec = Executors.newScheduledThreadPool ( 1 );
		fExec.scheduleAtFixedRate (
			new Runnable ()
			{
				@Override
				public void run ()
				{
					try
					{
						storeFile ();
					}
					catch ( IOException e )
					{
						log.warn ( "Problem flusing to file: " + e.getMessage(), e );
					}
				}
			},
			flushPeriodMins, flushPeriodMins, TimeUnit.MINUTES );
	}

	@Override
	public synchronized StorageInode lookup ( Path nodeId ) throws NamingIoException
	{
		return fMap.get ( nodeId );
	}

	@Override
	public synchronized Set<Path> getChildren ( Path path ) throws NamingIoException
	{
		// FIXME: this is really weak!
		
		final TreeSet<Path> result = new TreeSet<Path> ();
		for ( Path p : fMap.keySet () )
		{
			final Path pp = p.getParentPath ();
			if ( pp != null && pp.equals ( path ) )
			{
				result.add ( p );
			}
		}
		return result;
	}

	@Override
	public synchronized void store ( Path nodeId, StorageInode inode ) throws NamingIoException
	{
		fMap.put ( nodeId, inode );
		fDirty = true;
	}

	@Override
	public synchronized void remove ( Path nodeId ) throws NamingIoException
	{
		if ( null != fMap.remove ( nodeId ) )
		{
			fDirty = true;
		}
	}

	@Override
	protected synchronized void onStopRequested ()
	{
		fExec.shutdown ();
		try
		{
			storeFile ();
		}
		catch ( IOException e )
		{
			log.warn ( "Problem flushing to file: " + e.getMessage(), e );
		}
	}

	private final HashMap<Path,StorageInode> fMap;
	private boolean fDirty;
	private final File fFile;
	private final ScheduledExecutorService fExec;

	private static final Logger log = LoggerFactory.getLogger ( MemAndDiskNameService.class );

	private void loadFile () throws IOException
	{
		final BufferedReader br = new BufferedReader ( new FileReader ( fFile ) );
		try
		{
			String line = null;
			while ( (line = br.readLine ()) != null )
			{
				final JSONObject o = new JSONObject ( line );
				final Path p = Path.fromString ( o.getString ( "path" ) );
				final JSONObject node = o.getJSONObject ( "node" );
				final StorageInode inode = StorageInode.fromName ( node.getString("svc"), node.getString("id") );
				store ( p, inode );
			}
			fDirty = false;
		}
		finally
		{
			br.close ();
		}
	}

	private synchronized void storeFile () throws IOException
	{
		if ( fDirty )
		{
			final PrintWriter fw = new PrintWriter ( new FileWriter ( fFile ) );
			try
			{
				for ( Entry<Path, StorageInode> entry : fMap.entrySet () )
				{
					final StorageInode node = entry.getValue ();

					final JSONObject o = new JSONObject ();
					o.put ( "path", entry.getKey ().toString () );
					o.put ( "node",
						new JSONObject()
							.put ( "svc", node.getServiceName () )
							.put ( "id", node.getId () )
					);
					fw.println ( o.toString () );
				}
				
				fDirty = false;
			}
			catch ( JSONException x )
			{
				throw new IOException ( x );
			}
			finally
			{
				fw.close ();
			}
		}
	}
}
