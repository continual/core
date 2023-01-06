package io.continual.builder;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.builder.common.CommonDataSource;
import io.continual.builder.sources.BuilderJsonDataSource;
import io.continual.util.nv.NvReadable;

public class BuilderTest
{
	@Test
	public void testFromJsonNoContext ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "class" , "io.continual.builder.sources.BuilderJsonDataSource" );

		try {
			Assert.assertNotNull ( Builder.fromJson ( CommonDataSource.class , jsonObj ) );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testFromJsonWithContext ()
	{
		final JSONObject jsonObj = new JSONObject ();
		jsonObj.put ( "class" , "io.continual.builder.sources.BuilderJsonDataSource" );

		try {
			Assert.assertNotNull ( Builder.fromJson ( CommonDataSource.class , jsonObj , 
					new BuilderJsonDataSource ( jsonObj ) ) );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testBuildFromString ()
	{
		try {
			Assert.assertNotNull ( Builder.withBaseClass ( CommonDataSource.class )
					.usingClassName ( "io.continual.builder.sources.BuilderJsonDataSource" )
					.fromString ( "{}" )
					.allowFullClassnames ()
					.build () );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testBuildPreferences ()
	{
		try {
			Assert.assertNotNull ( Builder.withBaseClass ( CommonDataSource.class )
					.usingClassName ( "BuilderPrefsDataSource" )
					.searchingPath ( "io.continual.builder.sources" )
					.usingData ( (Preferences) null )
					.restrictFullClassnames ()
					.build () );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testBuildReadable ()
	{
		final List<String> lstPaths = new ArrayList<>();
		lstPaths.add ( "io.continual.builder.sources" );
		try {
			Assert.assertNotNull ( Builder.withBaseClass ( CommonDataSource.class )
					.usingClassName ( "BuilderReadableDataSource" )
					.searchingPaths ( lstPaths )
					.usingData ( (NvReadable) null )
					.build () );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testBuildJsonObject ()
	{
		try {
			Assert.assertNotNull ( Builder.withBaseClass ( CommonDataSource.class )
					.usingClassName ( "io.continual.builder.sources.BuilderJsonDataSource" )
					.usingData ( (JSONObject) null )
					.build () );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testBuildStream ()
	{
		try {
			Assert.assertNotNull ( Builder.withBaseClass ( BuilderJsonDataSource.class )
					.usingClassName ( "io.continual.builder.sources.BuilderJsonDataSource" )
					.readingJsonData ( new ByteArrayInputStream ( "{}".getBytes() ) )
					.build () );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test ( expected = BuildFailure.class )
	public void testBuildNoClassNameException () throws BuildFailure
	{
		Builder.withBaseClass ( CommonDataSource.class )
				.build();
	}

	@Test ( expected = BuildFailure.class )
	public void testBuildInstantiateException () throws BuildFailure
	{
		Builder.withBaseClass ( CommonDataSource.class )
				.usingClassName ( "io.continual.builder.common.CommonDataSource" )
				.build();
	}

	@Test ( expected = BuildFailure.class )
	public void testBuildClassNotFoundException () throws BuildFailure
	{
		Builder.withBaseClass ( CommonDataSource.class )
				.usingClassName ( "io.continual.builder.sources.DummyDataSource" )
				.build();
	}

	@Test ( expected = ClassCastException.class )
	public void testBuildClassCastException () throws BuildFailure
	{
		Builder.withBaseClass ( CommonDataSource.class )
				.usingClassName ( "io.continual.services.ConfigObject" )
				.build();
	}

	@Test ( expected = BuildFailure.class )
	public void testBuildFailureException () throws BuildFailure
	{
		throw new BuildFailure ( "Coverage" , new Throwable ( "Coverage" ) );
	}
}
