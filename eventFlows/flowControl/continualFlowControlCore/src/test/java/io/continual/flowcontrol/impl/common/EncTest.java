package io.continual.flowcontrol.impl.common;

import java.security.GeneralSecurityException;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.services.ServiceContainer;
import junit.framework.TestCase;

public class EncTest extends TestCase
{
	@Test
	public void testEnc () throws GeneralSecurityException, BuildFailure
	{
		final Enc enc = new Enc ( new ServiceContainer (), new JSONObject ().put ( "key", "password" ) );

		{
			final String encrypted = enc.encrypt ( "clear" );
			assertEquals ( 3, encrypted.split ( ":" ).length );
			final String decrypted = enc.decrypt ( encrypted );
			assertEquals ( "clear", decrypted );
		}

		{
			final String encrypted = enc.encrypt ( "dear" );
			assertEquals ( 3, encrypted.split ( ":" ).length );
			final String decrypted = enc.decrypt ( encrypted );
			assertEquals ( "dear", decrypted );
		}
	}

	@Test
	public void testOldDecrypt () throws GeneralSecurityException, BuildFailure
	{
		final Enc enc = new Enc ( new ServiceContainer (), new JSONObject ().put ( "key", "password" ) );

		@SuppressWarnings("deprecation")
		final String encrypted = enc._encryptOld ( "clear" );
		assertEquals ( 2, encrypted.split ( ":" ).length );
		final String decrypted = enc.decrypt ( encrypted );
		assertEquals ( "clear", decrypted );
	}
}
