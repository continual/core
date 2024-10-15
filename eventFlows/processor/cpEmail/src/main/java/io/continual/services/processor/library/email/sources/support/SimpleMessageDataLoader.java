package io.continual.services.processor.library.email.sources.support;

import java.util.LinkedList;
import java.util.List;

import javax.mail.internet.MimeMessage;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.model.Message;
import io.continual.services.processor.library.email.sources.MimeMessageWrapper;

public class SimpleMessageDataLoader implements DataLoader
{
	@Override
	public List<Message> getMessages ( long uid, MimeMessage msg ) throws BuildFailure
	{
		final LinkedList<Message> result = new LinkedList<>();

		result.add ( new MimeMessageWrapper ( uid, msg ) );
		
		return result;
	}
}
