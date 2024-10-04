package io.continual.services.messaging.impl.kafka.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.MemberAssignment;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.json.JSONObject;

import io.continual.util.data.json.JsonSerialized;
import io.continual.util.data.json.JsonVisitor;
import io.continual.util.data.json.JsonVisitor.ItemRenderer;

public class KafkaClusterStatus implements AutoCloseable
{
	public class KafkaClusterStatusException extends Exception
	{
		public KafkaClusterStatusException ( Throwable t ) { super(t); }
		private static final long serialVersionUID = 1L;
	}
	
	public KafkaClusterStatus ( AdminClient adminClient )
	{
		fKafka = adminClient;
	}

	public KafkaClusterStatus ( JSONObject adminClientConfig )
	{
		this ( KafkaAdminClient.create ( JsonVisitor.objectToMap ( adminClientConfig, val -> val ) ) );
	}

	@Override
	public void close ()
	{
		if ( fKafka != null ) fKafka.close ();
	}

	public interface Partition extends JsonSerialized
	{
		int getIndex ();
		long getEndOffsetPosition ();
		long getEndOffsetTimestamp ();

		String getLeader ();
		Set<String> getIsrs ();
		Set<String> getReplicas ();

		Partition setOffset ( long pos, long time );

		@Override
		default JSONObject toJson ()
		{
			return new JSONObject ()
				.put ( "index", getIndex () )
				.put ( "endOffsetPosition", getEndOffsetPosition () )
				.put ( "endOffsetTime", getEndOffsetTimestamp () )
				.put ( "leader", getLeader () )
				.put ( "isrs", JsonVisitor.listToArray ( getIsrs() ) )
				.put ( "replicas", JsonVisitor.listToArray ( getReplicas() ) )
			;
		}
	}

	public interface Topic extends JsonSerialized
	{
		String getId ();
		String getName ();
		Partition getPartition ( int index );
		List<? extends Partition> getPartitions ();
		Topic addPartition ( Partition p );

		@Override
		default JSONObject toJson ()
		{
			return new JSONObject ()
				.put ( "id", getId () )
				.put ( "name", getName () )
				.put ( "partitions", JsonVisitor.listToArray ( getPartitions() ) )
			;
		}
	}

	public interface Member extends JsonSerialized
	{
		String getClientId ();
		String getConsumerId ();

		@Override
		default JSONObject toJson ()
		{
			return new JSONObject ()
				.put ( "clientId", getClientId () )
				.put ( "consumerId", getConsumerId () )
			;
		}
	}

	public interface OffsetInfo extends JsonSerialized
	{
		long getOffsetPosition ();
		long getEndOfLog ();
		OffsetInfo setEndOfLog ( long endOffsetPosition );

		@Override
		default JSONObject toJson ()
		{
			return new JSONObject ()
				.put ( "offset", getOffsetPosition () )
				.put ( "endOfLog", getEndOfLog () )
				.put ( "lag", getEndOfLog()>-1 ? getEndOfLog () - getOffsetPosition () : null )
			;
		}
	}
	
	public interface Group extends JsonSerialized
	{
		String getId ();
		String getState ();
		boolean isSimple ();
		List<? extends Member> getMembers ();
		Map<String,Map<Integer, OffsetInfo>> getOffsets ();

		@Override
		default JSONObject toJson ()
		{
			return new JSONObject ()
				.put ( "id", getId () )
				.put ( "state", getState () )
				.put ( "isSimple", isSimple() )
				.put ( "members", JsonVisitor.listToArray ( getMembers() ) )
				.put ( "offsets", JsonVisitor.mapToObject ( getOffsets(), new ItemRenderer<Map<Integer,OffsetInfo>,JSONObject> ()
				{
					@Override
					public JSONObject render ( Map<Integer, OffsetInfo> offsetMap ) throws IllegalArgumentException
					{
						return JsonVisitor.mapToObject ( offsetMap, i->""+i, oi -> oi.toJson () );
					}
				} ) )
			;
		}
	}

	public Map<String,Topic> getTopicsReport () throws KafkaClusterStatusException
	{
		try
		{
			final HashMap<String,Topic> topicsTop = new HashMap<> ();

			for ( TopicListing tl : fKafka.listTopics ().listings().get () )
			{
				// we don't report on internal topics
				if ( tl.isInternal () ) continue;
				topicsTop.put ( tl.name (), new TopicImpl ( tl.name (), tl.topicId ().toString () ) );
			}

			final HashMap<TopicPartition, OffsetSpec> partitionMap = new HashMap<> ();

			for ( Map.Entry<String,TopicDescription> tdEntry : fKafka.describeTopics ( topicsTop.keySet () ).allTopicNames ().get ().entrySet () )
			{
				final Topic topicEntry = topicsTop.get ( tdEntry.getKey () );
				for ( TopicPartitionInfo pi : tdEntry.getValue ().partitions () )
				{
					partitionMap.put ( new TopicPartition ( tdEntry.getKey (), pi.partition () ), OffsetSpec.latest () );

					final PartitionImpl p = new PartitionImpl ( pi.partition (), pi.leader ().idString (), pi.isr (), pi.replicas() );
					topicEntry.addPartition ( p );
				}
			}

			// get offsets
			for ( Map.Entry<TopicPartition,ListOffsetsResultInfo> entry : fKafka.listOffsets ( partitionMap ).all ().get ( kKafkaCallTimeoutSeconds, TimeUnit.SECONDS ).entrySet () )
			{
				final TopicPartition tp = entry.getKey ();
				final ListOffsetsResultInfo offsets = entry.getValue ();

				final Topic topicInfo = topicsTop.get ( tp.topic () );
				topicInfo.getPartition ( tp.partition () ).setOffset ( offsets.offset (), offsets.timestamp () );
			}

			return topicsTop;
		}
		catch ( InterruptedException | ExecutionException | TimeoutException e )
		{
			throw new KafkaClusterStatusException ( e );
		}
	}
	
	public Map<String,Group> getConsumersReport () throws KafkaClusterStatusException
	{
		try
		{
			final Map<String,Group> groupsTop = new HashMap<> ();

			for ( ConsumerGroupListing cgl : fKafka.listConsumerGroups ().all ().get ( kKafkaCallTimeoutSeconds, TimeUnit.SECONDS ) )
			{
				final GroupImpl group = new GroupImpl ( cgl.groupId (), cgl.isSimpleConsumerGroup (), cgl.state ().orElse ( ConsumerGroupState.UNKNOWN ).toString () );
				groupsTop.put ( cgl.groupId (), group );

				// get member details
				for ( Map.Entry<String,ConsumerGroupDescription> dcgrMapEntry : fKafka.describeConsumerGroups ( Collections.singletonList ( group.getId () ) ).all ().get ( kKafkaCallTimeoutSeconds, TimeUnit.SECONDS ).entrySet () )
				{
					final ConsumerGroupDescription desc = dcgrMapEntry.getValue ();
					for ( MemberDescription member : desc.members () )
					{
						final String consumerId = member.consumerId ();
						final String clientId = member.clientId ();

						final MemberImpl mi = new MemberImpl ( consumerId, clientId );
						group.addMember ( mi );

						final MemberAssignment ma = member.assignment ();
						for ( TopicPartition tp : ma.topicPartitions () )
						{
							final String topic = tp.topic ();
							final int partition = tp.partition ();

							mi.addAssignment ( topic, partition );
						}
					}
				}
	
				// get offset details
				for ( Map.Entry<String, Map<TopicPartition, OffsetAndMetadata>> entry : fKafka.listConsumerGroupOffsets ( group.getId () ).all ().get ( kKafkaCallTimeoutSeconds, TimeUnit.SECONDS ).entrySet () )
				{
					final Map<TopicPartition,OffsetAndMetadata> map = entry.getValue ();
					for ( Map.Entry<TopicPartition,OffsetAndMetadata> ee : map.entrySet () )
					{
						final String topic = ee.getKey ().topic ();
						final int partition = ee.getKey ().partition ();
						final OffsetAndMetadata offsetInfo = ee.getValue ();

						group.addOffset ( topic, partition, offsetInfo.offset () );
					}
				}
			}
			return groupsTop;
		}
		catch ( InterruptedException | ExecutionException | TimeoutException e )
		{
			throw new KafkaClusterStatusException ( e );
		}
	}

	public void calcLags ( Map<String,Topic> topicMap, Map<String,Group> groups ) throws KafkaClusterStatusException
	{
		for ( Group g : groups.values () )
		{
			for ( Map.Entry<String,Map<Integer, OffsetInfo>> offsetEntry : g.getOffsets ().entrySet () )
			{
				final String topicName = offsetEntry.getKey ();

				final Topic topic = topicMap.get ( topicName );
				if ( topic == null ) continue;

				for ( Map.Entry<Integer, OffsetInfo> partitionToOffsets : offsetEntry.getValue ().entrySet () )
				{
					final Partition p = topic.getPartition ( partitionToOffsets.getKey () );
					if ( p != null )
					{
						partitionToOffsets.getValue ().setEndOfLog ( p.getEndOffsetPosition () );
					}
				}
			}
		}
	}
	
	private final AdminClient fKafka;
	private static final int kKafkaCallTimeoutSeconds = 120;


	private class PartitionImpl implements Partition
	{
		public PartitionImpl ( int index, String leader, List<Node> isrs, List<Node> repls )
		{
			fIndex = index;

			fLeader = leader;
			fIsrs = new TreeSet<> ();
			for ( Node n : isrs ) { fIsrs.add ( n.idString () ); }
			fReplicas = new TreeSet<> ();
			for ( Node n : repls ) { fReplicas.add ( n.idString () ); }

			fEndOffsetPos = 0;
			fEndOffsetTime = -1;
		}

		public PartitionImpl setOffset ( long pos, long time )
		{
			fEndOffsetPos = pos;
			fEndOffsetTime = time;
			return this;
		}

		@Override
		public int getIndex () { return fIndex; }

		@Override
		public long getEndOffsetPosition () { return fEndOffsetPos; }

		@Override
		public long getEndOffsetTimestamp () { return fEndOffsetTime; }

		@Override
		public String getLeader () { return fLeader; }

		@Override
		public Set<String> getIsrs () { return fIsrs; }

		@Override
		public Set<String> getReplicas () { return fReplicas; }

		private final int fIndex;
		private final String fLeader;
		private final TreeSet<String> fIsrs;
		private final TreeSet<String> fReplicas;

		private long fEndOffsetPos;
		private long fEndOffsetTime;
	};

	private class TopicImpl implements Topic
	{
		public TopicImpl ( String name, String id ) { fName = name; fId = id; fPartitions = new ArrayList<> (); }

		@Override
		public String getId () { return fId; }

		@Override
		public String getName () { return fName; } 

		@Override
		public Partition getPartition ( int index ) { return fPartitions.get ( index ); }
		
		@Override
		public List<? extends Partition> getPartitions () { return fPartitions; }

		public TopicImpl addPartition ( Partition p )
		{
			final int index = p.getIndex ();
			fPartitions.ensureCapacity ( index + 1 );
			while ( fPartitions.size() < index+1 )
			{
				fPartitions.add ( null );
			}
			fPartitions.set ( index, p );
			return this;
		}

		private final String fName;
		private final String fId;
		private final ArrayList<Partition> fPartitions;
	}

	private class GroupImpl implements Group
	{
		public GroupImpl ( String id, boolean isSimple, String state )
		{
			fId = id;
			fIsSimple = isSimple;
			fState = state;
			fMembers = new LinkedList<> ();
			fOffsets = new HashMap<> ();
		}

		public GroupImpl addMember ( MemberImpl mi )
		{
			fMembers.add ( mi );
			return this;
		}

		public GroupImpl addOffset ( String topic, int partition, long offset )
		{
			if ( !fOffsets.containsKey ( topic ) )
			{
				fOffsets.put ( topic, new HashMap<> () );
			}
			fOffsets.get ( topic ).put ( partition, new OffsetInfoImpl ( offset ) );
			return this;
		}

		@Override
		public String getId () { return fId; }

		@Override
		public String getState () { return fState; }

		@Override
		public boolean isSimple () { return fIsSimple; }

		@Override
		public List<? extends Member> getMembers () { return fMembers; }

		@Override
		public Map<String,Map<Integer,OffsetInfo>> getOffsets () { return fOffsets; }

		private final String fId;
		private final String fState;
		private final boolean fIsSimple;
		private final LinkedList<MemberImpl> fMembers;
		private final HashMap<String,Map<Integer,OffsetInfo>> fOffsets;
	}

	private class MemberImpl implements Member
	{
		public MemberImpl ( String consumerId, String clientId ) { fClientId = clientId; fConsumerId = consumerId; fAssignments = new HashMap<>(); }

		@Override
		public String getClientId () { return fClientId; }

		@Override
		public String getConsumerId () { return fConsumerId; }

		public MemberImpl addAssignment ( String topic, int partition )
		{
			if ( !fAssignments.containsKey ( topic ) )
			{
				fAssignments.put ( topic, new TreeSet<Integer> () );
			}
			fAssignments.get ( topic ).add ( partition );
			return this;
		}

		private final String fClientId;
		private final String fConsumerId;
		private final HashMap<String,Set<Integer>> fAssignments;
	}

	private class OffsetInfoImpl implements OffsetInfo
	{
		public OffsetInfoImpl ( long offset )
		{
			fOffset = offset;
			fEndOfLog = -1;
		}

		@Override
		public long getOffsetPosition () { return fOffset; }

		@Override
		public long getEndOfLog () { return fEndOfLog; }

		@Override
		public OffsetInfo setEndOfLog ( long endOffsetPosition )
		{
			fEndOfLog = endOffsetPosition;
			return this;
		}

		private final long fOffset;
		private long fEndOfLog;
	}
}
