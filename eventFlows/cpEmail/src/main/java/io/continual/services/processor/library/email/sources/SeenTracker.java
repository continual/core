package io.continual.services.processor.library.email.sources;

interface SeenTracker extends AutoCloseable
{
	void addUid ( long uid );

	boolean isUidSeen ( long uid );
}
