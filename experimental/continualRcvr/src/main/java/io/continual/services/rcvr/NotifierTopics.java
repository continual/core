package io.continual.services.rcvr;

/**
 * Specific topics used throughout this infrastructure.
 */
public enum NotifierTopics
{
	/**
	 * Inbound events from users (the outside world).
	 */
	USER_EVENTS,

	/**
	 * Internally generated model update requests.
	 */
	UPDATE_REQUESTS,
	
	/**
	 * Notifications that an object in the data model has changed.
	 */
	DB_UPDATES,


	CONTEXT_UPDATES,

	ACCOUNT_UPDATES,

	MANAGEMENT_DATA
}
