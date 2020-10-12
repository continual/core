/*
 *	Copyright 2019, Continual.io
 *
 *	Licensed under the Apache License, Version 2.0 (the "License");
 *	you may not use this file except in compliance with the License.
 *	You may obtain a copy of the License at
 *	
 *	http://www.apache.org/licenses/LICENSE-2.0
 *	
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS,
 *	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *	See the License for the specific language governing permissions and
 *	limitations under the License.
 */
package io.continual.iam.credentials;

/**
 * A username and password credential.  Note that username and userId
 * (used as the key to the user database) are not necessarily the same
 * value.
 */
public class UsernamePasswordCredential
{
	public UsernamePasswordCredential ( String un, String pw )
	{
		fUsername = un;
		fPassword = pw;
	}

	@Override
	public String toString () { return "User/Pwd for " + fUsername; }
	
	public String getUsername () { return fUsername; }
	public String getPassword () { return fPassword; }

	private final String fUsername;
	private final String fPassword;
}
