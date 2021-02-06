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

import io.continual.util.collections.MultiMap;

/**
 * A MIME part factory. The factory is provided to the multipart MIME reader to
 * allow an application to create parts. For example, a web app receiving a file
 * input may want to store that file on AWS S3 rather than in a local tmp file.
 */
public interface CHttpMimePartFactory
{
	/**
	 * Create a MIME part given header values for the part section.
	 * @param partHeaders
	 * @return a new MIME part
	 * @throws IOException
	 */
	CHttpMimePart createPart ( MultiMap<String,String> partHeaders ) throws IOException;
}
