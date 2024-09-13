package io.continual.flowcontrol.impl.controller.k8s;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.continual.builder.Builder.BuildFailure;
import io.continual.flowcontrol.FlowControlCallContext;
import io.continual.flowcontrol.impl.jobdb.test.DummyJobDb;
import io.continual.flowcontrol.model.FlowControlDeployment;
import io.continual.flowcontrol.services.deployer.FlowControlDeploymentService.ServiceException;
import io.continual.iam.identity.Identity;
import io.continual.services.ServiceContainer;
import io.continual.util.console.CmdLineParser;
import io.continual.util.console.CmdLinePrefs;
import io.continual.util.console.ConfiguredConsole;
import io.continual.util.nv.NvReadable;
import io.continual.util.nv.NvReadable.InvalidSettingValueException;
import io.continual.util.nv.NvReadable.MissingReqdSettingException;

public class K8sClientAuthTester extends ConfiguredConsole
{
	private static final String kContext = "context";
	private static final String kNamespace = "namespace";
	
	protected ConfiguredConsole setupOptions ( CmdLineParser p )
	{
		super.setupOptions ( p );

		p.registerOptionWithValue ( kContext );
		p.registerOptionWithValue ( kNamespace );

		return this;
	}

	protected Looper init ( NvReadable p, CmdLinePrefs clp ) throws MissingReqdSettingException, InvalidSettingValueException, StartupFailureException
	{
		try
		{
			final ServiceContainer sc = new ServiceContainer ();
			sc
				.add ( "jobDb", new DummyJobDb () )
			;

			log.info ( "Building Kubernetes controller config..." );
			final JSONObject config = new JSONObject ()
				.put ( K8sController.kSetting_ConfigTransfer, "configTransfer" )
			;

			final String context = clp.getString ( kContext, null );
			if ( context != null )
			{
				log.info ( "    context: " + context );
				config.put ( K8sController.kSetting_k8sContext, context );
			}

			final String namespace = clp.getString ( kNamespace, "default" );
			if ( namespace != null )
			{
				log.info ( "    namespace: " + namespace );
				config.put ( K8sController.kSetting_Namespace, namespace );
			}
			log.info ( "... complete" );

			log.info ( "Creating Kubernetes controller service object..." );
			final K8sController ctrl = new K8sController ( sc, config );
			log.info ( "... complete" );

			final FlowControlCallContext fccc = new FlowControlCallContext ()
			{
				@Override
				public Identity getUser () { return null; }
			};

			log.info ( "Requesting deployments..." );
			for ( final FlowControlDeployment d : ctrl.getDeployments ( fccc ) )
			{
				log.info ( "... job: " + d.getId () );
			}
			log.info ( "... complete" );
		}
		catch ( BuildFailure | ServiceException e )
		{
			log.warn ( e.getMessage (), e );
		}

		return null;
	}

	public static void main ( String[] args ) throws Exception
	{
		final K8sClientAuthTester tester = new K8sClientAuthTester ();
		tester.runFromMain ( args );
	}

	private static final Logger log = LoggerFactory.getLogger ( K8sClientAuthTester.class );
}
