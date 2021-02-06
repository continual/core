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

package io.continual.services.model.service.impl.s3;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;

import io.continual.http.util.http.standards.MimeTypes;
import io.continual.services.model.core.ModelObjectPath;
import io.continual.util.naming.Path;

/**
 * An interface to the S3 system
 */
public class S3Interface
{
	public S3Interface ( AmazonS3Client s3, String bucket, String prefix )
	{
		fS3 = s3;
		fBucketId = bucket;
		fPrefix = prefix;
	}

	public static String removeTrailingSlash ( String in )
	{
		if ( in.endsWith ( "/" ) ) return in.substring ( 0, in.length () - 1 );
		return in;
	}

	public boolean doesObjectExist ( ModelObjectPath path )
	{
		return fS3.doesObjectExist ( fBucketId, makeS3DataNodePath ( path ) );
	}

	public Set<String> getTopLevelItems ()
	{
		final ListObjectsV2Result listing = fS3.listObjectsV2 (
			new ListObjectsV2Request()
				.withBucketName ( fBucketId )
				.withDelimiter ( "/" )
		);

		final TreeSet<String> result = new TreeSet<> ();
		for ( String child : listing.getCommonPrefixes () )	// FIXME: truncation
		{
			result.add ( child );
		}
		return result;
	}

	public Set<String> getChildrenOf ( String container )
	{
		final ListObjectsV2Result listing = fS3.listObjectsV2 (
			new ListObjectsV2Request()
				.withBucketName ( fBucketId )
				.withPrefix ( makeS3Path ( container, true ) )
				.withDelimiter ( "/" )
		);

		final TreeSet<String> result = new TreeSet<> ();
		for ( String child : listing.getCommonPrefixes () )	// FIXME: truncation
		{
			result.add ( child );
		}
		return result;
	}

	public Set<Path> getChildrenOf ( ModelObjectPath path )
	{
		final ListObjectsV2Result listing = fS3.listObjectsV2 (
			new ListObjectsV2Request()
				.withBucketName ( fBucketId )
				.withPrefix ( makeS3Path ( path, true ) )
				.withDelimiter ( "/" )
		);

		final TreeSet<Path> result = new TreeSet<> ();
		for ( String child : listing.getCommonPrefixes () )	// FIXME: truncation
		{
			result.add ( makeModelPath ( child ) );
		}
		return result;
	}

	public boolean exists ( String path )
	{
		final String s3Path = makeS3DataNodePath ( path );
		final boolean result = fS3.doesObjectExist ( fBucketId, s3Path );
		logTrx ( "XST", s3Path, "" + result );
		return result;
	}

	public InputStream getObject ( String path )
	{
		final String s3Path = makeS3DataNodePath ( path );
		final InputStream is = fS3.getObject ( fBucketId, s3Path ).getObjectContent ();
		logTrx ( "GET", s3Path, "" );
		return is;
	}

	public InputStream getObject ( ModelObjectPath path )
	{
		final String s3Path = makeS3DataNodePath ( path );
		final InputStream is = fS3.getObject ( fBucketId, s3Path ).getObjectContent ();
		logTrx ( "GET", s3Path, "" );
		return is;
	}

	public void putObject ( String path, String data )
	{
		final byte[] bytes = data.getBytes ( Charset.forName ( "UTF-8" ) );
		final long len = bytes.length;

		final ObjectMetadata md = new ObjectMetadata ();
		md.setContentType ( MimeTypes.kAppJson );
		md.setContentLength ( len );

		final String s3Path = makeS3DataNodePath ( path );
		fS3.putObject ( fBucketId, s3Path, new ByteArrayInputStream ( bytes ), md );
		logTrx ( "PUT", s3Path, "" );
	}

	public void putObject ( ModelObjectPath path, String data )
	{
		final byte[] bytes = data.getBytes ( Charset.forName ( "UTF-8" ) );
		final long len = bytes.length;

		final ObjectMetadata md = new ObjectMetadata ();
		md.setContentType ( MimeTypes.kAppJson );
		md.setContentLength ( len );

		final String s3Path = makeS3DataNodePath ( path );
		fS3.putObject ( fBucketId, s3Path, new ByteArrayInputStream ( bytes ), md );
		logTrx ( "PUT", s3Path, "" );
	}

	public void putObject ( ModelObjectPath path, InputStream objectData, long length )
	{
		final ObjectMetadata md = new ObjectMetadata ();
		md.setContentType ( MimeTypes.kAppJson );
		if ( length > -1 )
		{
			md.setContentLength ( length );
		}

		final String s3Path = makeS3DataNodePath ( path );
		fS3.putObject ( fBucketId, s3Path, objectData, md );
		logTrx ( "PUT", s3Path, "" );
	}

	public void deleteObject ( ModelObjectPath id )
	{
		final String s3Path = makeS3DataNodePath ( id );
		fS3.deleteObject ( fBucketId, s3Path );
		logTrx ( "DEL", s3Path, "" );
	}

	private final AmazonS3Client fS3;
	private final String fBucketId;
	private final String fPrefix;

	public static String makeS3AcctPath ( String prefix, String acctId, boolean asFolder )
	{
		final StringBuilder sb = new StringBuilder ();
		if ( prefix != null && prefix.length () > 0 )
		{
			sb
				.append ( prefix )
				.append ( prefix.endsWith ( "/" ) ? "" : "/" )
			;
		}
		sb.append ( acctId );

		if ( asFolder )
		{
			sb.append ( "/" );
		}

		return sb.toString ();
	}

	private Path makeModelPath ( String key )
	{
		if ( !key.startsWith ( fPrefix ) ) throw new IllegalArgumentException ( key + " does not start with " + fPrefix );

		key = key.substring ( fPrefix.length () );
		if ( !key.startsWith ( "/" ) )
		{
			key = "/" + key;
		}
		return Path.fromString ( key );
	}

	private String makeS3Path ( ModelObjectPath nodeId, boolean isFolder )
	{
		final StringBuilder sb = new StringBuilder ();
		if ( fPrefix != null && fPrefix.length () > 0 )
		{
			sb
				.append ( fPrefix )
				.append ( fPrefix.endsWith ( "/" ) ? "" : "/" )
			;
		}

		final String nodeStr = nodeId.getId ();
		sb.append ( nodeStr );
		if ( isFolder && !nodeStr.endsWith ( "/" ) )
		{
			sb.append ( "/" );
		}

		return sb.toString ();
	}

	private String makeS3Path ( String nodeStr, boolean isFolder )
	{
		final StringBuilder sb = new StringBuilder ();
		if ( fPrefix != null && fPrefix.length () > 0 )
		{
			sb
				.append ( fPrefix )
				.append ( fPrefix.endsWith ( "/" ) ? "" : "/" )
			;
		}

		sb.append ( nodeStr );
		if ( isFolder && !nodeStr.endsWith ( "/" ) )
		{
			sb.append ( "/" );
		}

		return sb.toString ();
	}

//	private String makeS3Path ( String path )
//	{
//		final StringBuilder sb = new StringBuilder ();
//		if ( fPrefix != null && fPrefix.length () > 0 )
//		{
//			sb
//				.append ( fPrefix )
//				.append ( fPrefix.endsWith ( "/" ) ? "" : "/" )
//			;
//		}
//		sb.append ( path );
//		return sb.toString ();
//	}

	private String makeS3DataNodePath ( ModelObjectPath nodeId )
	{
		final String nodeStr = makeS3Path ( nodeId, false );

		final StringBuilder sb = new StringBuilder ();
		sb.append ( nodeStr );

		if ( !nodeStr.endsWith ( "/" ) )
		{
			sb.append ( "/" );
		}
		sb.append ( kDataObject );

		return sb.toString ();
	}

	private String makeS3DataNodePath ( String path )
	{
		final StringBuilder sb = new StringBuilder ();
		sb.append ( makeS3Path ( path, true ) );
		sb.append ( kDataObject );
		return sb.toString ();
	}

	private static final String kDataObject = "data.json";

	private void logTrx ( String op, String path, String msg )
	{
		log.info ( "S3 " + op + " @ " + fBucketId + " : " + path + ( msg != null && msg.length () > 0 ? ( " ... " + msg ) : "" ) );
	}
	private static final Logger log = LoggerFactory.getLogger ( S3Interface.class );
}
