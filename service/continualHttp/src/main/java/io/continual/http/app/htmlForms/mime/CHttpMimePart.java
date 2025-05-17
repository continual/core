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

package io.continual.http.app.htmlForms.mime;

import java.io.IOException;
import java.io.InputStream;

/**
 * A MIME part. These are created by the multipart MIME reader via the supplied
 * part factory. 
 */
public interface CHttpMimePart extends Comparable<CHttpMimePart>
{
	/**
	 * Get the content type for this part.
	 * @return a type string
	 */
	String getContentType ();

	/**
	 * Get the content disposition value for this part.
	 * @return a content disposition string
	 */
	String getContentDisposition ();

	/**
	 * Get the name for this part.
	 * @return a name string
	 */
	String getName ();

	/**
	 * Get the value associated with a given content disposition key. If the value doesn't
	 * exist, null is returned. If the value is not provided, an empty string is returned.
	 * @param key
	 * @return a string or null if undefined
	 */
	String getContentDispositionValue ( String key );

	/**
	 * Return true if this is a stream part.
	 * @return true if this is a stream part.
	 */
	boolean isStream ();

	/**
	 * open a stream to read this part's data.
	 * @return an input stream
	 * @throws IOException
	 */
	InputStream openStream () throws IOException;

	/**
	 * Get this part's data as a string.
	 * @return the part data as a string
	 */
	String getAsString ();

	/**
	 * Discard this part.
	 */
	void discard ();

	/**
	 * Used by the MIME reader to write bytes to the part.
	 * @param line
	 * @param offset
	 * @param length
	 * @throws IOException
	 */
	void write ( byte[] line, int offset, int length ) throws IOException;

	/**
	 * Used by the MIME reader to close the part during its creation.
	 * @throws IOException
	 */
	void close () throws IOException;

	/**
	 * Used in comparison to compare across implementation types.
	 * @return a comparison string
	 */
	String getCompareString ();

	/**
	 * implement comparison
	 */
	default int compareTo ( CHttpMimePart that )
	{
		return getCompareString ().compareTo ( that.getCompareString () );
	}
}
