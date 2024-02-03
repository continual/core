package io.continual.templating.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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
	public static ContinualTemplateSource fromResource ( final String resName )
	{
		return new ContinualTemplateSource ()
		{
			InputStream is = null;

			@Override
			public String getName ()
			{
				return resName;
			}

			@Override
			public InputStream getTemplate () throws TemplateNotFoundException
			{
				try
				{
					is = ResourceLoader.load ( resName );
					if ( is == null ) throw new TemplateNotFoundException ( "Couldn't load resource " + resName );
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
		return new ContinualTemplateSource ()
		{
			@Override
			public String getName ()
			{
				return "input stream";
			}

			@Override
			public InputStream getTemplate () throws TemplateNotFoundException
			{
				return is;
			}

			@Override
			public void close () throws IOException
			{
				is.close ();
			}
		};
	}
}
