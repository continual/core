package io.continual.builder.sources;

import java.util.prefs.Preferences;

import org.junit.Test;

import junit.framework.TestCase;

public class BuilderPrefsDataSourceTest extends TestCase
{
	@Test
	public void testConstructor ()
	{
		assertNotNull ( new BuilderPrefsDataSource ( null ) );
	}

	@Test
	public void testGetClassNameFromData ()
	{
		final Preferences pref = Preferences.userRoot ().node ( this.getClass ().getName () );
		pref.put ( "class" , "BuilderReadableDataSource" );
		assertEquals ( "BuilderReadableDataSource" , 
				new BuilderPrefsDataSource ( pref ).getClassNameFromData () );
	}
}
