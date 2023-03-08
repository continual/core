package io.continual.services.model.impl.session;

import org.json.JSONObject;

import io.continual.services.model.core.Model;
import io.continual.services.model.impl.delegator.ModelMount;
import io.continual.util.naming.Path;

public class StdMountTableEntry implements ModelMount
{
	public StdMountTableEntry ( Path mountPoint, Model m )
	{
		fMountPoint = mountPoint;
		fModel = m;
	}

	public boolean contains ( Path path )
	{
		return path.startsWith ( fMountPoint );
	}

	@Override
	public Path getMountPoint ()
	{
		return fMountPoint;
	}

	@Override
	public Model getModel ()
	{
		return fModel;
	}

	@Override
	public Path getPathWithinModel ( Path absolutePath )
	{
		return absolutePath.makePathWithinParent ( fMountPoint );
	}

	@Override
	public Path getGlobalPath ( Path from )
	{
		return fMountPoint.makeChildPath ( from );
	}

	@Override
	public JSONObject toJson ()
	{
		return new JSONObject ()
			.put ( "path", fMountPoint.toString () )
			.put ( "model", new JSONObject ()
				.put ( "acctId", fModel.getAcctId () )
				.put ( "id", fModel.getId () )
			)
		;
	}

	@Override
	public String toString ()
	{
		return getModel().toString ()  + " @ " + getMountPoint().toString ();
	}

	final Path fMountPoint;
	private final Model fModel;
}
