/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */

package io.continual.http.app.htmlForms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.LoggerFactory;

import io.continual.http.app.htmlForms.mime.CHttpMimePart;
import io.continual.http.app.htmlForms.mime.CHttpMimePartFactory;
import io.continual.http.app.htmlForms.mime.CHttpMimePartsReader;
import io.continual.http.service.framework.context.CHttpRequest;
import io.continual.http.util.http.standards.HttpMethods;
import io.continual.util.collections.MultiMap;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.TypeConvertor.conversionError;

/**
 * A form post wrapper provides form related methods over a CHttpRequest. 
 */
public class CHttpFormPostWrapper
{
	public static class ParseException extends Exception 
	{
		public ParseException ( Throwable t ) { super ( t ); }
		private static final long serialVersionUID = 1L;
	}

	/**
	 * Construct a form post wrapper from a request.
	 * @param req
	 */
	public CHttpFormPostWrapper ( CHttpRequest req )
	{
		this ( req, null );
	}

	/**
	 * Construct a form post wrapper from a request, and use the given MIME reader
	 * part factory. The MIME reader is only invoked if the request's content type
	 * is multipart/form-data.
	 * 
	 * @param req
	 * @param mimePartFactory If null, use the built-in part factory.
	 */
	public CHttpFormPostWrapper ( CHttpRequest req, CHttpMimePartFactory mimePartFactory )
	{
		fRequest = req;
		final String ct = req.getContentType ();

		fIsMultipartFormData = ct != null && ct.startsWith ( "multipart/form-data" );
		fPartFactory = mimePartFactory == null ? new simpleStorage () : mimePartFactory;
		fParsedValues = new HashMap<>();
		fParseComplete = false;
	}

	/**
	 * this must be called to cleanup mime part resources (e.g. tmp files)
	 */
	public void close ()
	{
		for ( CHttpMimePart vi : fParsedValues.values () )
		{
			vi.discard ();
		}
	}
	
	@Override
	public String toString ()
	{
		final StringBuilder sb = new StringBuilder ();

		sb.append ( fRequest.getMethod ().toUpperCase () ).append ( " {" );
		if ( fIsMultipartFormData )
		{
			if ( fParseComplete )
			{
				for ( Entry<String, CHttpMimePart> e : fParsedValues.entrySet () )
				{
					sb.append ( e.getKey () ).append ( ":" );
					final CHttpMimePart mp = e.getValue ();
					if ( mp.getAsString () != null )
					{
						 sb.append ( "'" ).append(mp.getAsString()).append ( "' " );
					}
					else
					{
						sb.append ( "(data) " );
					}
				}
			}
			else
			{
				sb.append ( "not parsed yet" );
			}
		}
		else
		{
			for ( Entry<String, String[]> e : fRequest.getParameterMap().entrySet () )
			{
				final StringBuilder sb2 = new StringBuilder ();
				for ( String val : e.getValue () )
				{
					if ( sb2.length () > 0 ) sb2.append ( "," );
					sb2.append ( val );
				}
				sb.append ( e.getKey() ).append ( ": [" ).append ( sb2.toString () ).append ( "], " );
			}
		}
		sb.append ( " }" );

		return sb.toString ();
	}

	/**
	 * Is the underlying request a POST? (Not a PUT, not anything else. Just POST.)
	 * @return true if the underlying request is a POST.
	 */
	public boolean isPost ()
	{
		return fRequest.getMethod ().toLowerCase ().equals ( HttpMethods.POST );
	}

	/**
	 * Does the form have a given parameter (aka field)
	 * @param name
	 * @return true if the named parameter/field exists in the form post
	 * @throws ParseException 
	 */
	public boolean hasParameter ( String name ) throws ParseException
	{
		parseIfNeeded ();

		return fIsMultipartFormData ?
			fParsedValues.containsKey ( name ) :
			fRequest.getParameterMap ().containsKey ( name );
	}

	/**
	 * Get the form post parameters in a map from name to string value.
	 * @return a map of post parameters
	 * @throws ParseException 
	 */
	public Map<String,String> getValues () throws ParseException 
	{
		final HashMap<String,String> map = new HashMap<>();

		parseIfNeeded ();

		if ( fIsMultipartFormData )
		{
			for ( Map.Entry<String,CHttpMimePart> e : fParsedValues.entrySet () )
			{
				final String val = e.getValue ().getAsString ();
				if ( val != null )
				{
					map.put ( e.getKey(), val );
				}
			}
		}
		else
		{
			for ( Map.Entry<?,?> e : fRequest.getParameterMap ().entrySet() )
			{
				final String key = e.getKey ().toString ();
				final String[] vals = (String[]) e.getValue ();
				String valToUse = "";
				if ( vals.length > 0 )
				{
					valToUse = vals[0];
				}
				map.put ( key, valToUse );
			}
		}
		return map;
	}

	/**
	 * Does the form contain the given field? This goes beyond hasParameter() to check
	 * on a multipart MIME post whether the value provided is a string.
	 * 
	 * @param name
	 * @return true if the named field exists
	 * @throws ParseException 
	 */
	public boolean isFormField ( String name ) throws ParseException
	{
		boolean result = false;
		if ( hasParameter ( name ) )
		{
			if ( fIsMultipartFormData )
			{
				final CHttpMimePart val = fParsedValues.get ( name );
				result = ( val != null && val.getAsString () != null );
			}
			else
			{
				result = true;
			}
		}
		return result;
	}

	/**
	 * Get the value of a field as a string. This returns null for MIME parts like
	 * file uploads -- the value has to be available as a string rather than a stream.
	 * 
	 * @param name
	 * @return a string for the named field, or null if it doesn't exist (or is a MIME part)
	 * @throws ParseException 
	 */
	public String getValue ( String name ) throws ParseException
	{
		parseIfNeeded ();

		String result;
		if ( fIsMultipartFormData )
		{
			final CHttpMimePart val = fParsedValues.get ( name );
			
			result = null;
			if ( val != null && val.getAsString () != null )
			{
				result = val.getAsString ().trim ();
			}
		}
		else
		{
			result = fRequest.getParameter ( name );
			if ( result != null )
			{
				result = result.trim ();
			}
		}
		return result;
	}

	/**
	 * A convenience version of getValue(String). Useful for passing enums. The argument
	 * is converted to a string.
	 * @param o
	 * @return the string value for the given field
	 * @throws ParseException 
	 */
	public String getValue ( Object o ) throws ParseException
	{
		return getValue ( o.toString () );
	}

	/**
	 * Get the named value, or return defVal if it does not exist on the form.
	 * @param key
	 * @param defVal
	 * @return the value from the form, or the default value
	 * @throws ParseException 
	 */
	public String getValue ( String key, String defVal ) throws ParseException
	{
		String result = getValue ( key );
		if ( result == null )
		{
			result = defVal;
		}
		return result;
	}

	/**
	 * A convenience version for use with Enums. The field name argument is converted to a string.
	 * @param fieldName
	 * @param defVal
	 * @return
	 * @throws ParseException 
	 */
	public String getValue ( Object fieldName, String defVal ) throws ParseException
	{
		return getValue ( fieldName.toString (), defVal );
	}

	/**
	 * Get the named value as a boolean, or return valIfMissing if no such field exists.
	 * @param name
	 * @param valIfMissing
	 * @return true/false
	 * @throws ParseException 
	 */
	public boolean getValueBoolean ( String name, boolean valIfMissing ) throws ParseException
	{
		boolean result = valIfMissing;
		final String val = getValue ( name );
		if ( val != null )
		{
			result = TypeConvertor.convertToBooleanBroad ( val );
		}
		return result;
	}

	/**
	 * A convenience version for use with Enums. The field name argument is converted to a string.
	 * @param fieldName
	 * @param valIfMissing
	 * @return true/false
	 * @throws ParseException 
	 */
	public boolean getValueBoolean ( Object fieldName, boolean valIfMissing ) throws ParseException
	{
		return getValueBoolean ( fieldName.toString() , valIfMissing );
	}
	
	/**
	 * Get the named value as an integer, or null if no such value exists
	 * @param name
	 * @return the integer value or null
	 * @throws ParseException 
	 */
	public Integer getValueInt ( String name ) throws ParseException
	{
		return getValueInt ( name, null );
	}

	/**
	 * Get the named value as an integer, or return valIfMissing if no such field exists.
	 * @param name
	 * @param valIfMissing
	 * @return the integer value
	 * @throws ParseException 
	 */
	public Integer getValueInt ( String name, Integer valIfMissing ) throws ParseException
	{
		Integer result = valIfMissing;
		final String val = getValue ( name );
		if ( val != null )
		{
			try
			{
				result = TypeConvertor.convertToInt ( val );
			}
			catch ( conversionError e )
			{
				result = valIfMissing;
			}
		}
		return result;
	}

	/**
	 * A convenience version for use with Enums. The field name argument is converted to a string.
	 * @param fieldName
	 * @param valIfMissing
	 * @throws ParseException 
	 */
	public Integer getValueInt ( Object fieldName, Integer valIfMissing ) throws ParseException
	{
		return getValueInt ( fieldName.toString() , valIfMissing );
	}

	/**
	 * Get the named value as an double.
	 * @param name
	 * @return the value, or null
	 * @throws ParseException 
	 */
	public Double getValueDouble ( String name ) throws ParseException
	{
		return getValueDouble ( name, null );
	}
	
	/**
	 * Get the named value as an double, or return valIfMissing if no such field exists.
	 * @param name
	 * @param valIfMissing
	 * @return the value
	 * @throws ParseException 
	 */
	public Double getValueDouble ( String name, Double valIfMissing ) throws ParseException
	{
		Double result = valIfMissing;
		final String val = getValue ( name );
		if ( val != null )
		{
			try
			{
				result = TypeConvertor.convertToDouble ( val );
			}
			catch ( conversionError e )
			{
				result = valIfMissing;
			}
		}
		return result;
	}

	/**
	 * A convenience version for use with Enums. The field name argument is converted to a string.
	 * @param fieldName
	 * @param valIfMissing
	 * @throws ParseException 
	 */
	public Double getValueDouble ( Object fieldName, Double valIfMissing ) throws ParseException
	{
		return getValueDouble ( fieldName.toString() , valIfMissing );
	}

	/**
	 * Change the value for a given field.
	 * @param fieldName
	 * @param newVal
	 * @throws ParseException 
	 */
	public void changeValue ( String fieldName, String newVal ) throws ParseException
	{
		parseIfNeeded ();

		if ( fIsMultipartFormData )
		{
			if ( fParsedValues.containsKey ( fieldName ) )
			{
				fParsedValues.get ( fieldName ).discard ();
			}
			
			final inMemoryFormDataPart part = new inMemoryFormDataPart ( "", "form-data; name=\"" + fieldName + "\"" );
			final byte[] array = newVal.getBytes ();
			part.write ( array, 0, array.length );
			part.close ();
			fParsedValues.put ( fieldName, part );
		}
		else
		{
			fRequest.changeParameter ( fieldName, newVal );
		}
	}

	/**
	 * Get the MIME part for a given field name.
	 * @param name
	 * @return a MIME part
	 * @throws ParseException 
	 */
	public CHttpMimePart getStream ( String name ) throws ParseException
	{
		parseIfNeeded ();

		if ( fIsMultipartFormData )
		{
			final CHttpMimePart val = fParsedValues.get ( name );
			if ( val != null && val.getAsString () == null )
			{
				return val;
			}
		}
		return null;
	}
	
	private final CHttpRequest fRequest;
	private final boolean fIsMultipartFormData;
	private boolean fParseComplete;
	private final HashMap<String,CHttpMimePart> fParsedValues;
	private final CHttpMimePartFactory fPartFactory;

	private void parseIfNeeded () throws ParseException
	{
		if ( fIsMultipartFormData && !fParseComplete )
		{
			try
			{
				final String ct = fRequest.getContentType ();
				int boundaryStartIndex = ct.indexOf ( kBoundaryTag );
				if ( boundaryStartIndex != -1 )
				{
					boundaryStartIndex = boundaryStartIndex + kBoundaryTag.length ();
					final int semi = ct.indexOf ( ";", boundaryStartIndex );
					int boundaryEndIndex = semi == -1 ? ct.length () : semi;

					final String boundary = ct.substring ( boundaryStartIndex, boundaryEndIndex ).trim ();
					final CHttpMimePartsReader mmr = new CHttpMimePartsReader ( boundary, fPartFactory );
					final InputStream is = fRequest.getBodyStream ();
					mmr.read ( is );
					is.close ();

					for ( CHttpMimePart mp : mmr.getParts () )
					{
						fParsedValues.put ( mp.getName(), mp );
					}
				}
			}
			catch ( IOException e )
			{
				log.warn ( "There was a problem reading a multipart/form-data POST: " + e.getMessage () );
				throw new ParseException ( e );
			}
			fParseComplete = true;
		}
	}

	private static final String kBoundaryTag = "boundary=";

	static final org.slf4j.Logger log = LoggerFactory.getLogger ( CHttpFormPostWrapper.class );

	public static abstract class basePart implements CHttpMimePart
	{
		public basePart ( String contentType, String contentDisp )
		{
			fType = contentType;
			fDisp = contentDisp;

			fDispMap = new HashMap<>();
			parseDisposition ( contentDisp );

			final int nameSpot = fDisp.indexOf ( "name=\"" );
			String namePart = fDisp.substring ( nameSpot + "name=\"".length () );
			final int closeQuote = namePart.indexOf ( "\"" );
			namePart = namePart.substring ( 0, closeQuote );
			fName = namePart;
		}

		@Override
		public String getContentType ()
		{
			return fType;
		}

		@Override
		public String getContentDisposition ()
		{
			return fDisp;
		}

		@Override
		public String getContentDispositionValue ( String key )
		{
			return fDispMap.get ( key );
		}

		@Override
		public String getName ()
		{
			return fName;
		}

		@Override
		public void discard ()
		{
		}

		private final String fType;
		private final String fDisp;
		private final String fName;
		private final HashMap<String,String> fDispMap;

		// form-data; name="file"; filename="IMG_21022013_122919.png"
		private void parseDisposition ( String contentDisp )
		{
			final String[] parts = contentDisp.split ( ";");
			for ( String part : parts )
			{
				String key = part.trim ();
				String val = "";
				final int eq = key.indexOf ( '=' );
				if ( eq > -1 )
				{
					val = key.substring ( eq+1 );
					key = key.substring ( 0, eq );

					// if val is in quotes, remove them
					if ( val.startsWith ( "\"" ) && val.endsWith ( "\"" ) )
					{
						val = val.substring ( 1, val.length () - 1 );
					}
				}
				fDispMap.put ( key, val );
			}
		}
	}
	
	public static class inMemoryFormDataPart extends basePart
	{
		public inMemoryFormDataPart ( String ct, String cd )
		{
			super ( ct, cd );
			fValue = "";
		}
		
		@Override
		public void write ( byte[] line, int offset, int length )
		{
			fValue = new String ( line, offset, length );
		}

		@Override
		public void close ()
		{
		}

		@Override
		public InputStream openStream () throws IOException
		{
			throw new IOException ( "Opening stream on in-memory form data." );
		}

		@Override
		public String getAsString ()
		{
			return fValue;
		}

		private String fValue;
	}

	private static class tmpFilePart extends basePart
	{
		public tmpFilePart ( String ct, String cd ) throws IOException
		{
			super ( ct, cd );

			fFile = File.createTempFile ( "chttp.", ".part" );
			fStream = new FileOutputStream ( fFile );
		}

		@Override
		public void write ( byte[] line, int offset, int length ) throws IOException
		{
			if ( fStream != null )
			{
				fStream.write ( line, offset, length );
			}
		}

		@Override
		public void close () throws IOException
		{
			if ( fStream != null )
			{
				fStream.close ();
				fStream = null;
			}
		}

		@Override
		public InputStream openStream () throws IOException
		{
			if ( fStream != null )
			{
				log.warn ( "Opening input stream on tmp file before it's fully written." );
			}
			return new FileInputStream ( fFile );
		}

		@Override
		public String getAsString ()
		{
			return null;
		}

		@Override
		public void discard ()
		{
            //noinspection ResultOfMethodCallIgnored
            fFile.delete ();
			fFile = null;
			fStream = null;
		}

		private File fFile;
		private FileOutputStream fStream;
	}

	static class simpleStorage implements CHttpMimePartFactory
	{
		@Override
		public CHttpMimePart createPart ( MultiMap<String, String> partHeaders ) throws IOException
		{
			final String contentDisp = partHeaders.getFirst ( "content-disposition" );
			if ( contentDisp != null && contentDisp.contains ( "filename=\"" ) )
			{
				return new tmpFilePart ( partHeaders.getFirst ( "content-type" ), contentDisp );
			}
			else
			{
				return new inMemoryFormDataPart ( partHeaders.getFirst ( "content-type" ), contentDisp );
			}
		}
	}
}
