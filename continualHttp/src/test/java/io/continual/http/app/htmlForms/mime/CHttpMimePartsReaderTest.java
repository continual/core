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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import io.continual.http.app.htmlForms.CHttpFormPostWrapper.inMemoryFormDataPart;
import io.continual.util.collections.MultiMap;
import junit.framework.TestCase;

public class CHttpMimePartsReaderTest extends TestCase
{
	@Test
	public void testSimpleMultipartStream () throws IOException
	{
		final CHttpMimePartsReader reader = new CHttpMimePartsReader ( "---foobar", new testStorage () );
		final String input = "\n" +
				"-----foobar\r\n" +
				"Content-Disposition: form-data; name=\"caption\"\r\n" +
				"\r\n" + 
				"test\r\n" + 
				"-----foobar\r\n" +
				"Content-Disposition: form-data; name=\"image\"; filename=\"myphoto.jpg\"\r\n" +
				"Content-Type: image/jpeg\r\n" + 
				"\r\n" + 
				"binary-data\r\n" + 
				"-----foobar--\r\n";

		final InputStream is = new ByteArrayInputStream ( input.getBytes () ); 
		reader.read ( is );

		final List<CHttpMimePart> parts = reader.getParts ();
		assertEquals ( 2, parts.size() );

		final CHttpMimePart p1 = parts.remove ( 0 );
		assertNotNull ( p1 );

		final CHttpMimePart p2 = parts.remove ( 0 );
		assertNotNull ( p2 );

		assertEquals ( "test", p1.getAsString () );
	}

	static class testStorage implements CHttpMimePartFactory
	{
		@Override
		public CHttpMimePart createPart ( MultiMap<String, String> partHeaders ) throws IOException
		{
			final String contentDisp = partHeaders.getFirst ( "content-disposition" );
			if ( contentDisp != null && contentDisp.contains ( "filename=\"" ) )
			{
				// would be a file
				return new inMemoryFormDataPart ( partHeaders.getFirst ( "content-type" ), contentDisp );
			}
			else
			{
				return new inMemoryFormDataPart ( partHeaders.getFirst ( "content-type" ), contentDisp );
			}
		}
	}
}
