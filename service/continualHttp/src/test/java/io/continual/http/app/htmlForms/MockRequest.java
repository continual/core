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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import io.continual.http.service.framework.context.CHttpRequest;

public class MockRequest implements CHttpRequest
{
	public MockRequest ( Map<String,String[]> values )
	{
		fValues = values;
	}

	@Override
	public String getMethod ()
	{
		return "GET";
	}

	@Override
	public String getPathInContext ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFirstHeader ( String string )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getHeader ( String string )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<String>> getAllHeaders ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String[]> getParameterMap ()
	{
		return fValues;
	}

	@Override
	public String getParameter ( String key )
	{
		final String[] vals = fValues.get ( key );
		return ( vals != null && vals.length > 0 ) ? vals[0] : null;
	}

	@Override
	public void changeParameter ( String fieldName, String defVal )
	{
		fValues.put ( fieldName, new String[] { defVal } );
	}

	@Override
	public String getContentType ()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getBodyStream ()
		throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BufferedReader getBodyStreamAsText ()
		throws IOException
	{
		// TODO Auto-generated method stub
		return null;
	}

	private final Map<String,String[]> fValues;

	@Override
	public String getActualRemoteAddress ()
	{
		return "192.168.1.1";
	}

	@Override
	public String getBestRemoteAddress ()
	{
		return getActualRemoteAddress();
	}

	@Override
	public int getActualRemotePort ()
	{
		return 4321;
	}

	@Override
	public int getBestRemotePort ()
	{
		return getActualRemotePort ();
	}

	@Override
	public int getContentLength ()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getParameter ( String key, String defVal )
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getIntParameter ( String key, int defVal )
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLongParameter ( String key, long defVal )
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDoubleParameter ( String key, double defVal )
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getBooleanParameter ( String key, boolean defVal )
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public char getCharParameter ( String key, char defVal )
	{
		// TODO Auto-generated method stub
		return ' ';
	}

	@Override
	public String getUrl ()
	{
		return "http://foo.bar";
	}

	@Override
	public String getQueryString ()
	{
		return null;
	}

	@Override
	public boolean isSecure ()
	{
		return false;
	}
}
