package io.continual.services.processor.library.email.sources.support;

public interface SeenTracker extends AutoCloseable
{
	void addUid ( long uid );

	boolean isUidSeen ( long uid );
}
