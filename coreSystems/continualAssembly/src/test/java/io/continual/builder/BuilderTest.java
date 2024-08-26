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
		  ClassLoader classLoader = new ClassLoader() {};

		try {
			Assert.assertNotNull ( Builder.withBaseClass ( CommonDataSource.class )
					.usingClassName ( "BuilderPrefsDataSource" )
					.searchingPath ( "io.continual.builder.sources" )
					.usingData ( (Preferences) null )
					.usingClassLoader(classLoader)
					.restrictFullClassnames ()
					.build () );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testBuildReadable ()
	{
		ClassLoader classLoader = new ClassLoader() {};
		final List<String> lstPaths = new ArrayList<>();
		lstPaths.add ( "io.continual.builder.sources" );
		try {
			Assert.assertNotNull ( Builder.withBaseClass ( CommonDataSource.class )
					.usingClassName ( "BuilderReadableDataSource" )
					.searchingPaths ( lstPaths )
					.usingData ( (NvReadable) null )
					.usingClassLoader(classLoader)
					.build () );
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@Test
	public void testBuildJsonObject ()
	{
		ClassLoader classLoader = new ClassLoader() {};
		try {
			Assert.assertNotNull ( Builder.withBaseClass ( CommonDataSource.class )
					.usingClassName ( "io.continual.builder.sources.BuilderJsonDataSource" )
					.usingData ( (JSONObject) null )
					.usingClassLoader(classLoader)
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

	@Test
	public void testBuildMethod ()
	{
		ClassLoader classLoader = new ClassLoader() {};
		try {
			// Single Param
			Assert.assertNotNull ( Builder.withBaseClass ( BuilderJsonDataSource.class )
					.usingClassName ( "io.continual.builder.BuilderTest$TestBuilderJsonDataSource1" )
					.usingData ( new JSONObject () )
					.usingClassLoader(classLoader)
					.providingContext ( new JSONObject () )
					.build () );
			// Double Param
			Assert.assertNotNull ( Builder.withBaseClass ( BuilderJsonDataSource.class )
					.usingClassName ( "io.continual.builder.BuilderTest$TestBuilderJsonDataSource2" )
					.usingData ( new JSONObject () )
					.usingClassLoader(classLoader)
					.providingContext ( new JSONObject () )
					.build () );
			// Static Single Param
			Builder.withBaseClass ( BuilderJsonDataSource.class )
					.usingClassName ( "io.continual.builder.BuilderTest$TestBuilderJsonDataSource3" )
					.usingData ( new JSONObject () )
					.usingClassLoader(classLoader)
					.providingContext ( new JSONObject () )
					.build ();
			// Static Double Param
			Builder.withBaseClass ( BuilderJsonDataSource.class )
					.usingClassName ( "io.continual.builder.BuilderTest$TestBuilderJsonDataSource4" )
					.usingData ( new JSONObject () )
					.usingClassLoader(classLoader)
					.providingContext ( new JSONObject () )
					.build ();
		} catch (BuildFailure e) {
			Assert.fail ( "Expected to execute but failed with exception " + e.getMessage () );
		}
	}

	@SuppressWarnings("unused")
	private static class TestBuilderJsonDataSource1 extends BuilderJsonDataSource {
		public TestBuilderJsonDataSource1 () {
			super ( new JSONObject () );
		}
		public TestBuilderJsonDataSource2 fromJson ( JSONObject data ) {
			return null;
		}
	}
	@SuppressWarnings("unused")
	private static class TestBuilderJsonDataSource2 extends BuilderJsonDataSource {
		public TestBuilderJsonDataSource2 () {
			super ( new JSONObject () );
		}
		public TestBuilderJsonDataSource1 fromJson ( JSONObject data , JSONObject context ) {
			return null;
		}
	}
	@SuppressWarnings("unused")
	private static class TestBuilderJsonDataSource3 extends BuilderJsonDataSource {
		public TestBuilderJsonDataSource3 () {
			super ( new JSONObject () );
		}
		public static TestBuilderJsonDataSource3 fromJson ( JSONObject data ) {
			return null;
		}
	}
	@SuppressWarnings("unused")
	private static class TestBuilderJsonDataSource4 extends BuilderJsonDataSource {
		public TestBuilderJsonDataSource4 () {
			super ( new JSONObject () );
		}
		public static TestBuilderJsonDataSource4 fromJson ( JSONObject data , JSONObject context ) {
			return null;
		}
	}
	

}
