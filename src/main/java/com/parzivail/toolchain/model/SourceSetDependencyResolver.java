package com.parzivail.toolchain.model;

/**
 * Resolves which source set of one module should be visible to another.
 */
public final class SourceSetDependencyResolver
{
	/**
	 * Prevents construction.
	 */
	private SourceSetDependencyResolver()
	{
	}

	/**
	 * Resolves which source set of a dependency should be visible to a consuming source set.
	 *
	 * <p>Client source sets, including the client GameTest source set, must see client-only API from
	 * their dependencies, not just common code. Every other consumer, including the common GameTest
	 * source set, only sees common code so it cannot reference client classes by accident.
	 *
	 * @param graph                 the authoritative build graph
	 * @param dependencyId          the dependency module identifier
	 * @param consumerSourceSetName the consuming source-set name
	 *
	 * @return the source-set name to depend on
	 */
	public static String dependencySourceSetName(
			BuildGraph graph,
			String dependencyId,
			String consumerSourceSetName
	)
	{
		if (!SourceSetNames.isClientEnvironment(consumerSourceSetName))
		{
			return SourceSetNames.MAIN;
		}

		var dependency = ModuleAggregationResolver.requireModule(graph, dependencyId);

		return SourceSetLayout.hasClientSourceSet(dependency) ? SourceSetNames.CLIENT : SourceSetNames.MAIN;
	}
}
