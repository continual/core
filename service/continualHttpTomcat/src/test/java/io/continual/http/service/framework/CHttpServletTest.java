package io.continual.http.service.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.http.service.framework.context.CHttpRequest;
import io.continual.util.naming.Path;
import junit.framework.TestCase;

public class CHttpServletTest extends TestCase
{
	@Test
	public void testMetricNamingForRootPathGet () throws BuildFailure
	{
		final CHttpServlet s = new CHttpServlet ();
		final CHttpMetricNamer mn = s.getMetricNamer ();

		assertEquals ( Path.fromString ( "/null" ), mn.getMetricNameFor ( null ) );
		
		assertEquals ( Path.fromString ( "/GET (root)" ), mn.getMetricNameFor ( new TestReq ( "GET", "/" ) ) );
	}

	private static class TestReq implements CHttpRequest 
	{
		private String fPath;
		private String fMethod;

		public TestReq ( String method, String path )
		{
			fMethod = method;
			fPath = path;
		}

		@Override
		public String getUrl ()
		{
			return "https://localhost:8080" + fPath;
		}

		@Override
		public String getQueryString ()
		{
			return null;
		}

		@Override
		public String getMethod ()
		{
			return fMethod;
		}

		@Override
		public String getPathInContext () { return fPath; }

		@Override
		public String getFirstHeader ( String header )
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public List<String> getHeader ( String header )
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
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getParameter ( String key )
		{
			// TODO Auto-generated method stub
			return null;
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
			return 0;
		}

		@Override
		public void changeParameter ( String fieldName, String defVal )
		{
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getContentType ()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getContentLength ()
		{
			// TODO Auto-generated method stub
			return 0;
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

		@Override
		public String getActualRemoteAddress ()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getBestRemoteAddress ()
		{
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getActualRemotePort ()
		{
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getBestRemotePort ()
		{
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public boolean isSecure ()
		{
			// TODO Auto-generated method stub
			return false;
		}
		
	}
}
