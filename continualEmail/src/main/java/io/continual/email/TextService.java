package io.continual.email;

import java.io.IOException;

public interface TextService
{
	interface SmsBuilder
	{
		SmsBuilder to ( String phoneNumber );
		SmsBuilder withBody ( String body );
	}

	SmsBuilder createMessage ();
	
	void sendMessage ( SmsBuilder msg ) throws IOException;
}
