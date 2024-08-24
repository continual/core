package io.continual.flowcontrol.impl.enc;

import java.security.GeneralSecurityException;

public interface Encryptor
{
	String encrypt ( String val ) throws GeneralSecurityException;
	String decrypt ( String val ) throws GeneralSecurityException;
}
