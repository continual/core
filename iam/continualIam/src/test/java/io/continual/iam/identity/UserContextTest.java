package io.continual.iam.identity;

import org.json.JSONObject;
import org.junit.Test;

import junit.framework.TestCase;

import io.continual.iam.impl.common.CommonJsonIdentity;

public class UserContextTest extends TestCase
{
	@Test
	public void testBuilderSameUserSponsor ()
	{
		final Identity id = new CommonJsonIdentity ( "user" , null , null );
		final UserContext<Identity> uc = UserContext.builder ()
				.forUser ( id )
				.sponsoredByUser ( id )
				.build();
		assertNotNull ( uc );
		assertEquals ( id.getId() , uc.getUser().getId() );
		assertEquals ( id.getId() , uc.getSponsor().getId() );
	}

	@Test
	public void testBuilderDiffUserSponsor ()
	{
		final Identity userId = new CommonJsonIdentity ( "user1" , null , null );
		final Identity sponsorId = new CommonJsonIdentity ( "user2" , null , null );
		final UserContext<Identity> uc = UserContext.builder ()
				.forUser ( userId )
				.sponsoredByUser ( sponsorId )
				.build();
		assertNotNull ( uc );
		assertEquals ( userId.getId() , uc.getEffectiveUserId() );
		assertEquals ( sponsorId.getId() , uc.getActualUserId() );
	}
	
	@Test
	public void testBuilderNoSponsor ()
	{
		final Identity id = new CommonJsonIdentity ( "user" , null , null );
		final UserContext<Identity> uc = UserContext.builder ()
				.forUser ( id )
				.sponsoredByUser ( null )
				.build();
		assertNotNull ( uc );
		assertEquals ( id.getId() , uc.getEffectiveUserId() );
		assertEquals ( id.getId() , uc.getActualUserId() );
	}

	@Test
	public void testToStringWithSponsor ()
	{
		final Identity userId = new CommonJsonIdentity ( "user1" , null , null );
		final Identity sponsorId = new CommonJsonIdentity ( "user2" , null , null );
		final String expect = userId.getId () + " (" + sponsorId.getId () + ")";
		final UserContext<Identity> uc = UserContext.builder ()
				.forUser ( userId )
				.sponsoredByUser ( sponsorId )
				.build();
		assertNotNull ( uc );
		final String result = uc.toString();
		assertEquals ( expect , result );
	}

	@Test
	public void testToStringNoSponsor ()
	{
		final Identity userId = new CommonJsonIdentity ( "user" , null , null );
		final String expect = userId.getId ();
		final UserContext<Identity> uc = UserContext.builder ()
				.forUser ( userId )
				.sponsoredByUser ( null )
				.build();
		assertNotNull ( uc );
		final String result = uc.toString();
		assertEquals ( expect , result );
	}

	@Test
	public void testToJsonWithSponsor ()
	{
		final Identity userId = new CommonJsonIdentity ( "user1" , null , null );
		final Identity sponsorId = new CommonJsonIdentity ( "user2" , null , null );
		final UserContext<Identity> uc = UserContext.builder ()
				.forUser ( userId )
				.sponsoredByUser ( sponsorId )
				.build();
		final JSONObject result = uc.toJson();
		assertNotNull ( result );
		assertEquals ( 2 , result.length() );
		assertEquals ( userId.getId() , result.get( "identity" ) );
		assertEquals ( sponsorId.getId() , result.get( "sponsor" ) );
	}

	@Test
	public void testToJsonNoSponsor ()
	{
		final Identity userId = new CommonJsonIdentity ( "user" , null , null );
		final UserContext<Identity> uc = UserContext.builder ()
				.forUser ( userId )
				.sponsoredByUser ( null )
				.build();
		final JSONObject result = uc.toJson();
		assertNotNull ( result );
		assertEquals ( 1 , result.length() );
		assertEquals ( userId.getId() , result.get( "identity" ) );
		assertFalse ( result.has( "sponsor" ) );
	}
}
