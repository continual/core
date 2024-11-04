package io.continual.flowcontrol.impl.enc;

import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONException;
import org.json.JSONObject;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.services.encryption.Encryptor;
import io.continual.services.ServiceContainer;
import io.continual.services.SimpleService;
import io.continual.util.data.TypeConvertor;
import io.continual.util.data.UniqueStringGenerator;
import io.continual.util.data.exprEval.ExpressionEvaluator;

public class Enc extends SimpleService implements Encryptor
{
	public Enc ( ServiceContainer sc, JSONObject config ) throws BuildFailure
	{
		final ExpressionEvaluator ee = sc.getExprEval ();
		try
		{
			fEncKey = ee.evaluateText ( ee.evaluateText ( config.getString ( "key" ) ) );
			fCipher = Cipher.getInstance ( ee.evaluateText ( config.optString ( "cipher", "AES/CBC/PKCS5Padding" ) ) );
		}
		catch ( JSONException | NoSuchAlgorithmException | NoSuchPaddingException x )
		{
			throw new BuildFailure ( x );
		}
	}

	@Override
	public String encrypt ( String val ) throws GeneralSecurityException
	{
		final byte[] salt = generateSalt().getBytes ( StandardCharsets.UTF_8 );
		final SecretKeySpec secretKeySpec = getSecretKeySpec ( salt );

		fCipher.init ( Cipher.ENCRYPT_MODE, secretKeySpec );
		final AlgorithmParameters params = fCipher.getParameters ();
		final byte[] iv = params.getParameterSpec ( IvParameterSpec.class ).getIV ();
		final byte[] ciphertext = fCipher.doFinal ( val.getBytes ( StandardCharsets.UTF_8 ) );

		return TypeConvertor.base64Encode ( ciphertext ) + ":" + TypeConvertor.base64Encode ( iv ) + ":" + TypeConvertor.base64Encode ( salt );
	}

	@Override
	public String decrypt ( String val ) throws GeneralSecurityException
	{
		final String[] parts = val.split ( ":" );
		if ( parts.length < 2 || parts.length > 3 ) throw new GeneralSecurityException ( "Unexpected encrypted text format." );

		// older data uses a temporary salt that isn't stored in the value
		byte[] salt = kFixmeSalt; 
		if ( parts.length == 3 )
		{
			salt = TypeConvertor.base64Decode ( parts[2] );
		}

		final SecretKeySpec secretKeySpec = getSecretKeySpec ( salt );

		final byte[] iv = TypeConvertor.base64Decode ( parts[1] );
		final byte[] ciphertext = TypeConvertor.base64Decode ( parts[0] );

		fCipher.init ( Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec ( iv ) );
		return new String ( fCipher.doFinal ( ciphertext ), StandardCharsets.UTF_8 );
	}

	private final String fEncKey;
	private final Cipher fCipher; 

	private static final String kKeyChars = "ABCDEFGHJIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private static final int kSaltLength = 32;

	private static String generateSalt ()
	{
		return UniqueStringGenerator.createKeyUsingAlphabet ( null, kKeyChars, kSaltLength );
	}

	private SecretKeySpec getSecretKeySpec ( byte[] salt ) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		final SecretKeyFactory factory = SecretKeyFactory.getInstance ( "PBKDF2WithHmacSHA256" );
		final KeySpec spec = new PBEKeySpec ( fEncKey.toCharArray (), salt, 65536, 256 );
		final SecretKey tmp = factory.generateSecret ( spec );
		return new SecretKeySpec ( tmp.getEncoded (), "AES" );
	}

	// older data used this static salt
	private static final byte[] kFixmeSalt = "salty".getBytes ( StandardCharsets.UTF_8 );

	/**
	 * Provided for unit test of backward compatible decrypt. Don't use this for new code.
	 * @param val
	 * @Deprecated
	 * @return an encrypted string using the older format
	 * @throws GeneralSecurityException
	 */
	@Deprecated
	String _encryptOld ( String val ) throws GeneralSecurityException
	{
		final SecretKeySpec secretKeySpec = getSecretKeySpec ( kFixmeSalt );

		fCipher.init ( Cipher.ENCRYPT_MODE, secretKeySpec );
		final AlgorithmParameters params = fCipher.getParameters ();
		final byte[] iv = params.getParameterSpec ( IvParameterSpec.class ).getIV ();
		final byte[] ciphertext = fCipher.doFinal ( val.getBytes ( StandardCharsets.UTF_8 ) );

		return TypeConvertor.base64Encode ( ciphertext ) + ":" + TypeConvertor.base64Encode ( iv );
	}
}
