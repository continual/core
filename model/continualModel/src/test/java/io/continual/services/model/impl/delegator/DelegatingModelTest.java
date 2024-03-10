package io.continual.services.model.impl.delegator;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.junit.Test;

import io.continual.builder.Builder.BuildFailure;
import io.continual.iam.impl.common.CommonJsonIdentity;
import io.continual.services.model.core.Model;
import io.continual.services.model.core.ModelItemList;
import io.continual.services.model.core.ModelRelation;
import io.continual.services.model.core.ModelRelationInstance;
import io.continual.services.model.core.ModelRelationList;
import io.continual.services.model.core.ModelRequestContext;
import io.continual.services.model.core.data.JsonObjectAccess;
import io.continual.services.model.core.exceptions.ModelRequestException;
import io.continual.services.model.core.exceptions.ModelSchemaViolationException;
import io.continual.services.model.core.exceptions.ModelServiceException;
import io.continual.services.model.impl.mem.InMemoryModel;
import io.continual.services.model.impl.session.StdMountTableEntry;
import io.continual.util.naming.Name;
import io.continual.util.naming.Path;
import junit.framework.TestCase;

public class DelegatingModelTest extends TestCase
{
	private static final Path kMountPoint1 = Path.fromString ( "/foo/m1" );
	private static final Path kMountPoint2 = Path.fromString ( "/foo/m2" );

	@Test
	public void testBackingModelWork () throws IOException, BuildFailure, ModelSchemaViolationException, ModelRequestException, ModelServiceException
	{
		final TestIdentity user = new TestIdentity ();

		// the models
		final Model backingModel = new InMemoryModel ( "test", "backingmodel" );
		final Model model1 = new InMemoryModel ( "test", "mount1" );
		final Model model2 = new InMemoryModel ( "test", "mount2" );

		// place a few objects...
		final ModelRequestContext mrc1 = model1.getRequestContextBuilder ().forUser ( user ).build ();
		model1.createUpdate ( mrc1, Path.fromString ( "/foo" ) )
			.overwrite ( new JsonObjectAccess ( new JSONObject ().put ( "helloMyNameIs", "foo" ) ) )
			.execute ()
		;
		model1.createUpdate ( mrc1, Path.fromString ( "/bar" ) )
			.overwrite ( new JsonObjectAccess ( new JSONObject ().put ( "helloMyNameIs", "bar" ) ) )
			.execute ()
		;

		final ModelRequestContext mrc2 = model2.getRequestContextBuilder ().forUser ( user ).build ();
		model2.createUpdate ( mrc2, Path.fromString ( "/baz" ) )
			.overwrite ( new JsonObjectAccess ( new JSONObject ().put ( "helloMyNameIs", "baz" ) ) )
			.execute ()
		;
		model2.createUpdate ( mrc2, Path.fromString ( "/bee" ) )
			.overwrite ( new JsonObjectAccess ( new JSONObject ().put ( "helloMyNameIs", "bee" ) ) )
			.execute ()
		;

		// make an internal relation...
		model1.relate ( mrc1, ModelRelation.from ( Path.fromString("/bar"), "barToFoo", Path.fromString ( "/foo" ) )); 
		
		// setup the delegating model
		try ( final DelegatingModel delegatingModel = new DelegatingModel ( "test", "test", backingModel ) )
		{
			// mount the component models
			delegatingModel.mount ( new StdMountTableEntry ( kMountPoint1, model1 ) );
			delegatingModel.mount ( new StdMountTableEntry ( kMountPoint2, model2 ) );

			final ModelRequestContext dmrc = delegatingModel.getRequestContextBuilder ().forUser ( user ).build ();

			final Path m1Foo = kMountPoint1.makeChildItem ( Name.fromString ( "foo" ) );
			final Path m1Bar = kMountPoint1.makeChildItem ( Name.fromString ( "bar" ) );
			final Path m2Baz = kMountPoint2.makeChildItem ( Name.fromString ( "baz" ) );

			// make a relation that crosses models
			delegatingModel.relate ( dmrc, ModelRelation.from ( m1Bar, "barToBaz", m2Baz ) ); 

			// check for some objects...
			assertTrue ( delegatingModel.exists ( dmrc, m1Foo ) );
			assertTrue ( delegatingModel.exists ( dmrc, m2Baz ) );

			// check bar's relations
			final ModelRelationList barRelnsList = delegatingModel
				.selectRelations ( m1Bar )
				.getRelations ( dmrc )
			;
			final List<ModelRelationInstance> barRelnsWithChildren = ModelItemList.iterateIntoList ( barRelnsList );
			final List<ModelRelationInstance> barRelns = barRelnsWithChildren.stream()
				.filter ( mri -> !mri.getName ().equals ( "child" ) )
				.collect ( Collectors.toList () )
			;
			
			assertEquals ( 2, barRelns.size () );
			final ModelRelationInstance mri1 = barRelns.get ( 0 );
			assertEquals ( m1Bar, mri1.getFrom () );
			assertEquals ( m1Foo, mri1.getTo () );

			final ModelRelationInstance mri2 = barRelns.get ( 1 );
			assertEquals ( m1Bar, mri2.getFrom () );
			assertEquals ( m2Baz, mri2.getTo () );
		}
	}

	private static class TestIdentity extends CommonJsonIdentity 
	{
		public TestIdentity ( )
		{
			super ( "test", CommonJsonIdentity.initializeIdentity (), null );
		}
	}
}
