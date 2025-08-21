package io.continual.jsonHttpClient.impl.ok;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.jsonHttpClient.HttpUsernamePasswordCredentials;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpRequest;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpResponse;
import io.continual.jsonHttpClient.JsonOverHttpClient.HttpServiceException;
import io.continual.util.data.TypeConvertor;
import io.continual.util.standards.MimeTypes;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class OkRequest implements HttpRequest
{
	public OkRequest ( OkHttpClient httpClient )
	{
		fHttpClient = httpClient;
		fHeaders = new HashMap<>();
	}

	@Override
	public HttpRequest onPath ( String url )
	{
		try
		{
			final URI uri = new URI ( url );
			final Map<String,String> params = parseQuery ( uri.getQuery () );
			if ( !params.isEmpty () )
			{
				final String scheme = uri.getScheme ();
				final String userInfo = uri.getUserInfo ();
				final String host = uri.getHost ();
				final int port = uri.getPort ();
				final String rawPath = uri.getRawPath ();

				// Build URL manually to avoid double-encoding of rawPath
				final StringBuilder urlBuilder = new StringBuilder ();
				urlBuilder.append ( scheme ).append ( "://" );
				if ( userInfo != null )
				{
					urlBuilder.append ( userInfo ).append ( "@" );
				}
				urlBuilder.append ( host );
				if ( port != -1 && port != getDefaultPort ( scheme ) )
				{
					urlBuilder.append ( ":" ).append ( port );
				}
				if ( rawPath != null )
				{
					urlBuilder.append ( rawPath );
				}
				fPath = urlBuilder.toString ();
				for ( Map.Entry<String,String> e : params.entrySet () )
				{
					addQueryParam ( e.getKey (), e.getValue () );
				}
				return this;
			}
		}
		catch ( URISyntaxException x )
		{
			// ignore
		}

		fPath = url;
		return this;
	}
	
	@Override
	public HttpRequest asUser ( HttpUsernamePasswordCredentials creds )
	{
		fCreds = creds;
		return this;
	}

	@Override
	public HttpRequest withHeader ( String name, String value )
	{
		List<String> set = fHeaders.get ( name );
		if ( set == null )
		{
			set = new LinkedList<String>();
			fHeaders.put ( name, set );
		}
		set.add ( value );
		
		return this;
	}

	@Override
	public HttpRequest withHeaders ( Map<String,String> headers )
	{
		for ( Map.Entry<String, String> e : headers.entrySet () )
		{
			withHeader ( e.getKey (), e.getValue () );
		}
		return this;
	}

	@Override
	public HttpRequest withExplicitQueryString ( String qs )
	{
		fQueryString = qs;
		fQueryParams = null;
		return this;
	}

	@Override
	public HttpRequest addQueryParam ( String key, String val )
	{
		fQueryString = null;
		if ( fQueryParams == null )
		{
			fQueryParams = new HashMap<> ();
		}
		fQueryParams.put ( key, val );
		return this;
	}

	@Override
	public HttpRequest withQueryString ( Map<String, String> qsMap )
	{
		for ( Map.Entry<String,String> e : qsMap.entrySet () )
		{
			addQueryParam ( e.getKey (), e.getValue () );
		}
		return this;
	}

	private Request.Builder basicReq ()
	{
		String path = fPath;

		if ( fQueryString != null && fQueryString.length () > 0 )
		{
			path = path + "?" + fQueryString;
		}
		else if ( fQueryParams != null && fQueryParams.size () > 0 )
		{
			final StringBuilder sb = new StringBuilder ();
			for ( Map.Entry<String,String> e : fQueryParams.entrySet () )
			{
				if ( sb.length () > 0 )
				{
					sb.append ( "&" );
				}
				sb
					.append ( e.getKey () )
					.append ( "=" )
					.append ( TypeConvertor.urlEncode ( e.getValue () )	)
				;
			}
			path = path + "?" + sb.toString ();
		}

		Request.Builder rb = new Request.Builder ()
			.url ( path )
		;

		if ( fCreds != null )
		{
			rb = rb.header ( "Authorization", Credentials.basic ( fCreds.getUser (), fCreds.getPassword () ) );
		}

		for ( Map.Entry<String, List<String>> e : fHeaders.entrySet () )
		{
			for ( String val : e.getValue () )
			{
				rb.addHeader ( e.getKey (), val );
			}
		}

		return rb;
	}

	@Override
	public HttpResponse get () throws HttpServiceException
	{
		return run ( "GET", basicReq ().build () );
	}

	@Override
	public HttpResponse delete () throws HttpServiceException
	{
		return run ( "DEL", basicReq ().delete().build () );
	}

	@Override
	public HttpResponse put ( JSONObject body ) throws HttpServiceException
	{
		final RequestBody rb = RequestBody.create (
			body.toString (),
			MediaType.parse ( MimeTypes.kAppJson )
		);
		return run ( "PUT", basicReq ().put(rb).build () );
	}

	@Override
	public HttpResponse patch ( JSONObject body ) throws HttpServiceException
	{
		final RequestBody rb = RequestBody.create (
			body.toString (),
			MediaType.parse ( MimeTypes.kAppJson )
		);
		return run ( "PAT", basicReq ().patch( rb ).build () );
	}

	@Override
	public HttpResponse post ( JSONObject body ) throws HttpServiceException
	{
		final RequestBody rb = RequestBody.create (
			body.toString (),
			MediaType.parse ( MimeTypes.kAppJson )
		);
		return run ( "POS", basicReq ().post(rb).build () );
	}

	@Override
	public HttpResponse post ( JSONArray body ) throws HttpServiceException
	{
		final RequestBody rb = RequestBody.create (
			body.toString (),
			MediaType.parse ( MimeTypes.kAppJson )
		);
		return run ( "POS", basicReq ().post(rb).build () );
	}

	private final OkHttpClient fHttpClient;

	private String fPath;
	private HttpUsernamePasswordCredentials fCreds;
	private HashMap<String,List<String>> fHeaders;
	private String fQueryString = null;
	private HashMap<String,String> fQueryParams = null;

	private HttpResponse run ( String verbForLog, Request request ) throws HttpServiceException
	{
		try
		{
			final String user = fCreds == null ? " (anon)" : " (as " + fCreds.getUser () + ")";
			log.info ( "HTTP {} " + fPath + user, verbForLog );

			final long startMs = System.nanoTime ();
			final Response response = fHttpClient.newCall ( request ).execute ();
			final long endMs = System.nanoTime ();

			final int code = response.code ();
			log.info ( "    -> {}; {} ms", code, ( endMs - startMs ) / 1000L / 1000L );
			
			return new OkResponse ( code, response );
		}
		catch ( IOException | JSONException e )
		{
			throw new HttpServiceException ( e );
		}
	}

	private static Map<String, String> parseQuery ( String query )
	{
		Map<String, String> queryPairs = new HashMap<> ();
		if ( query != null && !query.isEmpty () )
		{
			String[] pairs = query.split ( "&" );
			for ( String pair : pairs )
			{
				int idx = pair.indexOf ( "=" );
				queryPairs.put ( pair.substring ( 0, idx ),
					pair.substring ( idx + 1 ) );
			}
		}
		return queryPairs;
	}

	private static int getDefaultPort ( String scheme )
	{
		if ( "http".equalsIgnoreCase ( scheme ) )
		{
			return 80;
		}
		else if ( "https".equalsIgnoreCase ( scheme ) )
		{
			return 443;
		}
		return -1;
	}

	private static final Logger log = LoggerFactory.getLogger ( OkRequest.class );
}
