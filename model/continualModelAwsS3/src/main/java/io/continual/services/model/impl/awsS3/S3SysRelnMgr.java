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
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.util.data.TypeConvertor;
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
		final LinkedList<ModelRelationInstance> relns = new LinkedList<> ();
		relns.addAll ( getInboundRelations ( forObject ) );
		relns.addAll ( getOutboundRelations ( forObject ) );
		for ( ModelRelation mr : relns )
		{
			unrelate ( mr );
		}
	}

	public List<ModelRelationInstance> getInboundRelations ( Path forObject ) throws ModelServiceException, ModelRequestException
	{
		return getInboundRelationsNamed ( forObject, null );
	}

	public List<ModelRelationInstance> getOutboundRelations ( Path forObject ) throws ModelServiceException, ModelRequestException
	{
		return getOutboundRelationsNamed ( forObject, null );
	}

	public List<ModelRelationInstance> getInboundRelationsNamed ( Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

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

	public List<ModelRelationInstance> getOutboundRelationsNamed ( Path forObject, String named ) throws ModelServiceException, ModelRequestException
	{
		final LinkedList<ModelRelationInstance> result = new LinkedList<> ();

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

	private String encodePathComponent ( String s )
	{
		return TypeConvertor.urlEncode ( s );
	}

	private String decodePathComponent ( String s )
	{
		return TypeConvertor.urlDecode ( s );
	}

	private String getOutboundPrefixFor ( Path from, String reln )
	{
		final StringBuilder sb = new StringBuilder ()
			.append ( encodePathComponent ( from.toString ().substring ( 1 ) ) )
			.append ( kFwdSegmentSeparator )
		;
		if ( reln != null && reln.length () > 0 )
		{
			sb
				.append ( reln )
				.append ( kFwdSegmentSeparator )
			;
		}
		final Name name = Name.fromString ( sb.toString () );
		final Path p = fRelnsRoot.makeChildItem ( name );
		return p.toString ().substring ( 1 );
	}

	private String getInboundPrefixFor ( Path to, String reln )
	{
		final StringBuilder sb = new StringBuilder ()
			.append ( encodePathComponent ( to.toString ().substring ( 1 ) ) )
			.append ( kRevSegmentSeparator )
		;
		if ( reln != null && reln.length () > 0 )
		{
			sb
				.append ( reln )
				.append ( kRevSegmentSeparator )
			;
		}
		final Name name = Name.fromString ( sb.toString () );
		final Path p = fRelnsRoot.makeChildItem ( name );
		return p.toString ().substring ( 1 );
	}

	private String forwardDirToS3Path ( ModelRelation mr )
	{
		final StringBuilder sb = new StringBuilder ()
			.append ( encodePathComponent ( mr.getFrom ().toString ().substring ( 1 ) ) )
			.append ( kFwdSegmentSeparator )
			.append ( encodePathComponent ( mr.getName () ) )
			.append ( kFwdSegmentSeparator )
			.append ( encodePathComponent ( mr.getTo ().toString ().substring ( 1 ) ) )
		;
		final Name name = Name.fromString ( sb.toString () );
		final Path p = fRelnsRoot.makeChildItem ( name );
		return p.toString ().substring ( 1 );
	}

	private String reverseDirToS3Path ( ModelRelation mr )
	{
		final StringBuilder sb = new StringBuilder ()
			.append ( encodePathComponent ( mr.getTo ().toString ().substring ( 1 ) ) )
			.append ( kRevSegmentSeparator )
			.append ( encodePathComponent ( mr.getName () ) )
			.append ( kRevSegmentSeparator )
			.append ( encodePathComponent ( mr.getFrom ().toString ().substring ( 1 ) ) )
		;
		final Name name = Name.fromString ( sb.toString () );
		final Path p = fRelnsRoot.makeChildItem ( name );
		return p.toString ().substring ( 1 );
	}

	private ModelRelationInstance s3EntryToReln ( S3ObjectSummary objectSummary )
	{
		final String key = objectSummary.getKey ();
		final Path keyPath = Path.fromString ( "/" + key );
		final Path localPart = keyPath.makePathWithinParent ( fRelnsRoot );
		final String localPartStr = localPart.toString ().substring ( 1 );

		if ( localPartStr.contains ( kFwdSegmentSeparator ) )
		{
			final String[] parts = localPartStr.split ( kFwdSegmentSeparator );
			return ModelRelationInstance.from ( Path.fromString ( "/" + decodePathComponent(parts[0]) ), parts[1], Path.fromString ( "/" + decodePathComponent ( parts[2] ) ) );
		}
		else
		{
			final String[] parts = localPartStr.split ( kRevSegmentSeparator );
			return ModelRelationInstance.from ( Path.fromString ( "/" + decodePathComponent(parts[2]) ), parts[1], Path.fromString ( "/" + decodePathComponent ( parts[0] ) ) );
		}
	}

	private final String kFwdSegmentSeparator = "-->";
	private final String kRevSegmentSeparator = "<--";
	private final byte[] kBytesForObject = new JSONObject ().toString ().getBytes ( S3Model.kUtf8 );
}
