package io.continual.flowcontrol.impl.jobdb.model;

import java.security.GeneralSecurityException;

import org.junit.Test;

import io.continual.flowcontrol.impl.enc.Enc;
import junit.framework.TestCase;

public class ModelJobDbTest extends TestCase
{
	@Test
	public void testEnc () throws GeneralSecurityException
	{
		final Enc enc = new Enc ( "password" );

		{
			final String encrypted = enc.encrypt ( "clear" );
			final String decrypted = enc.decrypt ( encrypted );
			assertEquals ( "clear", decrypted );
		}

		{
			final String encrypted = enc.encrypt ( "dear" );
			final String decrypted = enc.decrypt ( encrypted );
			assertEquals ( "dear", decrypted );
		}
	}
}
