package io.continual.flowcontrol.model;

import java.util.List;

/**
 * For deployment environments that support it (e.g. k8s), resource request and limits. 
 */
public interface FlowControlResourceSpecs
{
	/**
	 * Create an empty resource specification
	 * @return an empty resource spec
	 */
	public static FlowControlResourceSpecs emptySpec () { return new FlowControlResourceSpecs() {}; }

	default String cpuRequest () { return null; }
	default String cpuLimit () { return null; }
	default String memLimit () { return null; }
	default String persistDiskSize () { return null; }
	default String logDiskSize () { return null; }

	/**
	 * For deployment environments that support it, a toleration. This is modeled after
	 * the Kubernetes concept of the same name.
	 */
	public interface Toleration
	{
		default String effect () { return null; }
		default String key () { return null; }
		default String operator () { return null; }
		default Long seconds () { return null; }
		default String value () { return null; }
	}

	default List<Toleration> tolerations () { return null; }
}
