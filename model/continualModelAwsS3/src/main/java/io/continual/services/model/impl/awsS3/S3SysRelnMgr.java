package io.continual.services.model.impl.awsS3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;

/**
 * A simple relation manager over S3. This implementation is not transaction-safe and it's possible that
 * a write failure could leave the relationship set in a half-written state.
 * 
 * Within a "relations" path parallel to "objects", we write relations as empty objects with formulated names
 * representing the relation in each direction.
 */
class S3SysRelnMgr
{
	public S3SysRelnMgr ( AmazonS3 s3, String bucketId, Path relnsRoot ) throws BuildFailure
	{
		fS3 = s3;
		fBucketId = bucketId;
		fRelnsRoot = relnsRoot;
	}

	public void relate ( ModelRelation mr ) throws ModelServiceException, ModelRequestException
	{
		final String fwdPath = forwardDirToS3Path ( mr );
		try (
			final ByteArrayInputStream bais = new ByteArrayInputStream ( kBytesForObject )
		)
		{
			fS3.putObject ( fBucketId, fwdPath, bais, new ObjectMetadata () );
		}
		catch ( IOException x )
		{
			throw new ModelServiceException ( x );
		}
		
		final String revPath = reverseDirToS3Path ( mr );
		try (
			final ByteArrayInputStream bais = new ByteArrayInputStream ( kBytesForObject )
		)
		{
			fS3.putObject ( fBucketId, revPath, bais, new ObjectMetadata () );
		}
		catch ( IOException x )
		{
			throw new ModelServiceException ( x );
		}
	}

	public void unrelate ( ModelRelation mr ) throws ModelServiceException, ModelRequestException
	{
		try
		{
			fS3.deleteObject ( fBucketId, forwardDirToS3Path ( mr ) );
		}
		catch ( SdkClientException x )
		{
			throw new ModelServiceException ( x );
		}
		try
		{
			fS3.deleteObject ( fBucketId, reverseDirToS3Path ( mr ) );
		}
		catch ( SdkClientException x )
		{
			throw new ModelServiceException ( x );
		}
	}

	public boolean doesRelationExist ( ModelRelation mr ) throws ModelServiceException
	{
		final String fwdPath = forwardDirToS3Path ( mr );
		try
		{
			return fS3.doesObjectExist ( fBucketId, fwdPath );
		}
		catch ( SdkClientException x )
		{
			throw new ModelServiceException ( x );
		}
	}
	
	public void removeAllRelations ( Path forObject ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelation> relns = new LinkedList<> ();
		relns.addAll ( getInboundRelations ( forObject ) );
		relns.addAll ( getOutboundRelations ( forObject ) );
		for ( ModelRelation mr : relns )
		{
			unrelate ( mr );
		}
	}

	public List<ModelRelation> getInboundRelations ( Path forObject ) throws ModelServiceException, ModelRequestException
	{
		return getInboundRelationsNamed ( forObject, null );
	}

	public List<ModelRelation> getOutboundRelations ( Path forObject ) throws ModelServiceException, ModelRequestException
	{
		return getOutboundRelationsNamed ( forObject, null );
	}

	public List<ModelRelation> getInboundRelationsNamed ( Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();

		final String inPrefix = getInboundPrefixFor ( forObject, named );
		final ListObjectsRequest listObjectsRequest = new ListObjectsRequest ()
			.withBucketName ( fBucketId )
			.withPrefix ( inPrefix )
		;
		ObjectListing ol;
		do
		{
			ol = fS3.listObjects ( listObjectsRequest );
			for ( S3ObjectSummary objectSummary : ol.getObjectSummaries () )
			{
				result.add ( s3EntryToReln ( objectSummary ) );
			}
			listObjectsRequest.setMarker ( ol.getNextMarker () );
		}
		while ( ol.isTruncated () );

		return result;
	}

	public List<ModelRelation> getOutboundRelationsNamed ( Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelation> result = new LinkedList<> ();

		final String inPrefix = getOutboundPrefixFor ( forObject, named );
		final ListObjectsRequest listObjectsRequest = new ListObjectsRequest ()
			.withBucketName ( fBucketId )
			.withPrefix ( inPrefix )
		;
		ObjectListing ol;
		do
		{
			ol = fS3.listObjects ( listObjectsRequest );
			for ( S3ObjectSummary objectSummary : ol.getObjectSummaries () )
			{
				result.add ( s3EntryToReln ( objectSummary ) );
			}
			listObjectsRequest.setMarker ( ol.getNextMarker () );
		}
		while ( ol.isTruncated () );

		return result;
	}

	private final Path fRelnsRoot;
	private final AmazonS3 fS3;
	private final String fBucketId;

	private String getOutboundPrefixFor ( Path from, String reln )
	{
		final StringBuilder sb = new StringBuilder ()
			.append ( from.toString ().substring ( 1 ) )
			.append ( kFwdSegmentSeparator )
		;
		if ( reln != null && reln.length () > 0 )
		{
			sb
				.append ( reln )
				.append ( kFwdSegmentSeparator )
			;
		}
		return sb.toString ();
	}

	private String getInboundPrefixFor ( Path to, String reln )
	{
		final StringBuilder sb = new StringBuilder ()
			.append ( to.toString ().substring ( 1 ) )
			.append ( kRevSegmentSeparator )
		;
		if ( reln != null && reln.length () > 0 )
		{
			sb
				.append ( reln )
				.append ( kRevSegmentSeparator )
			;
		}
		return sb.toString ();
	}

	private String forwardDirToS3Path ( ModelRelation mr )
	{
		final Path p = fRelnsRoot.makeChildItem ( Name.fromString (
			mr.getFrom ().toString ().substring ( 1 ) + kFwdSegmentSeparator + mr.getName () + kFwdSegmentSeparator + mr.getTo ().toString ().substring ( 1 ) )
		);
		return p.toString ().substring ( 1 );
	}

	private String reverseDirToS3Path ( ModelRelation mr )
	{
		final Path p = fRelnsRoot.makeChildItem ( Name.fromString (
			mr.getTo ().toString ().substring ( 1 ) + kRevSegmentSeparator + mr.getName () + kRevSegmentSeparator + mr.getFrom ().toString ().substring ( 1 ) )
		);
		return p.toString ().substring ( 1 );
	}

	private ModelRelation s3EntryToReln ( S3ObjectSummary objectSummary )
	{
		return null;
	}

	private final String kFwdSegmentSeparator = "|-->|";
	private final String kRevSegmentSeparator = "|<--|";
	private final byte[] kBytesForObject = new JSONObject ().toString ().getBytes ( S3Model.kUtf8 );
}
