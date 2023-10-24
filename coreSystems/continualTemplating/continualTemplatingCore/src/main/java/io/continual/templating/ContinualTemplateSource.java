package io.continual.templating;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import org.slf4j.LoggerFactory;

import com.github.f4b6a3.ulid.Ulid;

import io.continual.resources.ResourceLoader;

public interface ContinualTemplateSource extends AutoCloseable
{
	public static class TemplateNotFoundException extends Exception
	{
		public TemplateNotFoundException ( String msg ) { super ( msg ); }
		public TemplateNotFoundException ( Throwable t ) { super ( t ); }
		private static final long serialVersionUID = 1L;
	};

	/**
	 * Get a name for this template (used mainly for logging)
	 * @return a name
	 */
	String getName ();

	/**
	 * Get the (default) template as an input stream
	 * @return an input stream
	 * @throws TemplateNotFoundException
	 */
	InputStream getTemplate () throws TemplateNotFoundException;

	/**
	 * Close this template source
	 */
	default void close () throws Exception {}

	/**
	 * Build a template source from a UTF-8 string
	 * @param stringVal
	 * @return a template source
	 */
	public static ContinualTemplateSource fromString ( final String stringVal )
	{
		return new ContinualTemplateSource ()
		{
			@Override
			public InputStream getTemplate () throws TemplateNotFoundException
			{
				return new ByteArrayInputStream ( stringVal.getBytes ( StandardCharsets.UTF_8 ) );
			}

			@Override
			public String getName ()
			{
				return stringVal;
			}
		};
	}

	/**
	 * Build a template source from a resource name
	 * @param resName
	 * @return a template source
	 */
	public static ContinualTemplateSource fromResource ( final String resName, final String relativeName )
	{
		return new ContinualTemplateSource ()
		{
			InputStream is = null;

			@Override
			public String getName ()
			{
				return relativeName;
			}

			@Override
			public InputStream getTemplate () throws TemplateNotFoundException
			{
				try
				{
					is = ResourceLoader.load ( resName );
					if ( is == null )
					{
						LoggerFactory.getLogger ( ContinualTemplateSource.class ).warn ( "Couldn't load resource {}" , resName );
						throw new TemplateNotFoundException ( "Couldn't load resource " + resName );
					}
					return is;
				}
				catch ( IOException e )
				{
					throw new TemplateNotFoundException ( e );
				}
			}

			@Override
			public void close () throws IOException
			{
				if ( is != null )
				{
					is.close ();
				}
			}
		};
	}

	/**
	 * Build a template source from a file
	 * @param file
	 * @return a template source
	 */
	public static ContinualTemplateSource fromFile ( final File file )
	{
		return new ContinualTemplateSource ()
		{
			FileInputStream fFile = null;

			@Override
			public String getName ()
			{
				return file.getName ();
			}

			@Override
			public InputStream getTemplate () throws TemplateNotFoundException
			{
				if ( fFile != null ) throw new IllegalStateException ( "Template was already retrieved." );
				try
				{
					fFile = new FileInputStream ( file );
					return fFile;
				}
				catch ( FileNotFoundException e )
				{
					throw new TemplateNotFoundException ( e );
				}
			}

			@Override
			public void close () throws IOException
			{
				if ( fFile != null )
				{
					fFile.close ();
				}
			}
		};
	}

	/**
	 * Build a template source from an input stream
	 * @param is
	 * @return a template source
	 */
	public static ContinualTemplateSource fromInputStream ( final InputStream is )
	{
		// we return a manufactured unique name so that the input stream isn't inadvertently cached by name 
		return fromInputStream ( is, "input stream " + Ulid.fast ().toLowerCase () );
	}

	/**
	 * Build a template source from an input stream
	 * @param is
	 * @return a template source
	 */
	public static ContinualTemplateSource fromInputStream ( final InputStream is, String uniqueName )
	{
		return new ContinualTemplateSource ()
		{
			@Override
			public String getName () { return uniqueName; }

			@Override
			public InputStream getTemplate () { return is; }

			@Override
			public void close () throws IOException { is.close (); }
		};
	}

	/**
	 * Build a template source from a sequences of sources
	 * @param streams
	 * @return a template source
	 */
	public static ContinualTemplateSource combinedStreams ( ContinualTemplateSource... streams )
	{
		return new ContinualTemplateSource ()
		{
			@Override
			public String getName ()
			{
				final StringBuilder sb = new StringBuilder ();
				for ( ContinualTemplateSource src : streams )
				{
					if ( sb.length () > 0 )
					{
						sb.append ( " + " );
					}
					sb.append ( src.getName () );
				}
				return sb.toString ();
			}

			@Override
			public InputStream getTemplate () throws TemplateNotFoundException
			{
				final LinkedList<InputStream> srcs = new LinkedList<> ();
				for ( ContinualTemplateSource src : streams )
				{
					srcs.add ( src.getTemplate () );
				}

				return new InputStream ()
				{
					@Override
					public int read () throws IOException
					{
						while ( !srcs.isEmpty () )
						{
							// get the lead stream
							final InputStream currentStream = srcs.peek ();

							final int data = currentStream.read ();
							if ( data == -1 )
							{
								// end of this stream. remove it.
								srcs.remove ();
							}
							else
							{
								return data;
							}
						}

						// eof
						return -1;
					}
				};
			}

			@Override
			public void close () throws Exception
			{
				for ( ContinualTemplateSource src : streams )
				{
					src.close ();
				}
			}
		};
	}
}
