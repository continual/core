package io.continual.flowcontrol.model;

import java.security.GeneralSecurityException;

import io.continual.services.Service;

/**
 * An encryption service for string values.
 */
public interface Encryptor extends Service
{
	/**
	 * Encrypt the given value
	 * @param clearText
	 * @return the encrypted value as a string
	 * @throws GeneralSecurityException
	 */
	String encrypt ( String clearText ) throws GeneralSecurityException;

	/**
	 * Decrypt an encrypted value from this service
	 * @param encryptedText
	 * @return the clear text
	 * @throws GeneralSecurityException
	 */
	String decrypt ( String encryptedText ) throws GeneralSecurityException;
}
