package com.parzivail.toolchain.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves which modules contribute GameTest source sets to a launch.
 *
 * <p>A GameTest launch runs the ordinary aggregated module closure of its root module and adds the
 * test-only roots of every module in that closure that declares GameTest source sets, together with
 * the GameTest source sets those modules declared as dependencies.
 */
public final class GameTestModuleResolver
{
	/**
	 * Prevents construction.
	 */
	private GameTestModuleResolver()
	{
	}

	/**
	 * Resolves the modules whose GameTest source sets take part in a launch of one root module.
	 *
	 * @param graph        the authoritative build graph
	 * @param rootModuleId the launch root module identifier
	 *
	 * @return the modules with GameTest source sets, dependencies before dependents
	 */
	public static List<ModuleSpec> launchModules(BuildGraph graph, String rootModuleId)
	{
		Set<ModuleSpec> ordered = new LinkedHashSet<>();

		for (var module : ModuleAggregationResolver.aggregatedModules(graph, rootModuleId))
		{
			if (SourceSetLayout.hasGameTests(module))
			{
				collect(graph, module, ordered, new LinkedHashSet<>());
			}
		}

		return List.copyOf(ordered);
	}

	/**
	 * Resolves the transitive GameTest dependencies of one module.
	 *
	 * @param graph  the authoritative build graph
	 * @param module the consuming module
	 *
	 * @return the modules whose GameTest source sets the module can see, dependencies first and
	 * excluding the module itself
	 */
	public static List<ModuleSpec> gameTestDependencies(BuildGraph graph, ModuleSpec module)
	{
		Set<ModuleSpec> ordered = new LinkedHashSet<>();
		collect(graph, module, ordered, new LinkedHashSet<>());
		ordered.remove(module);
		return new ArrayList<>(ordered);
	}

	/**
	 * Collects one module after its own GameTest dependencies.
	 *
	 * @param graph    the authoritative build graph
	 * @param module   the module to collect
	 * @param ordered  the accumulated dependency-first module set
	 * @param visiting the modules on the current dependency path, used to reject cycles
	 */
	private static void collect(BuildGraph graph, ModuleSpec module, Set<ModuleSpec> ordered, Set<ModuleSpec> visiting)
	{
		if (ordered.contains(module))
		{
			return;
		}

		if (!visiting.add(module))
		{
			throw new IllegalArgumentException("Module '" + module.id() + "' is part of a gametest_dependencies cycle");
		}

		for (var dependencyId : module.gametestDependencies())
		{
			collect(graph, ModuleAggregationResolver.requireModule(graph, dependencyId), ordered, visiting);
		}

		visiting.remove(module);
		ordered.add(module);
	}
}
