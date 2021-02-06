package io.continual.basesvcs.services.id;

import io.continual.services.Service;

/**
 * Identify the node (i.e. host/vm/container) for this process.
 */
public interface NodeIdService extends Service
{
	String getNodeId ();
}
