package io.continual.services.processor.library.email.sources.support;

import java.io.IOException;
import java.util.List;

import javax.mail.internet.MimeMessage;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.processor.engine.model.Message;

public interface DataLoader
{
	List<Message> getMessages ( long uid, MimeMessage msg ) throws BuildFailure, IOException;
}
