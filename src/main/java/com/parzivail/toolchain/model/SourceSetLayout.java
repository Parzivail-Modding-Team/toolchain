package com.parzivail.toolchain.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared source-set layout helpers used by generated IntelliJ metadata and bespoke compilation.
 */
public final class SourceSetLayout
{
	/**
	 * Prevents construction.
	 */
	private SourceSetLayout()
	{
	}

	/**
	 * Gets the Java source roots for one module source set.
	 *
	 * @param module        the module specification
	 * @param sourceSetName the source-set name
	 *
	 * @return the Java source roots
	 */
	public static List<Path> sourceRoots(ModuleSpec module, String sourceSetName)
	{
		return switch (sourceSetName)
		{
			case SourceSetNames.CLIENT -> module.clientSources();
			case SourceSetNames.GAMETEST -> module.gametestSources();
			case SourceSetNames.GAMETEST_CLIENT -> module.gametestClientSources();
			default -> module.mainSources();
		};
	}

	/**
	 * Gets the resource roots for one module source set.
	 *
	 * @param module        the module specification
	 * @param sourceSetName the source-set name
	 *
	 * @return the resource roots
	 */
	public static List<Path> resourceRoots(ModuleSpec module, String sourceSetName)
	{
		return switch (sourceSetName)
		{
			case SourceSetNames.CLIENT -> module.clientResources();
			case SourceSetNames.GAMETEST -> module.gametestResources();
			case SourceSetNames.GAMETEST_CLIENT -> module.gametestClientResources();
			default -> mainResourceRoots(module);
		};
	}

	/**
	 * Gets the main resource roots, including the checked-in datagen output when one is declared.
	 *
	 * @param module the module specification
	 *
	 * @return the main resource roots
	 */
	private static List<Path> mainResourceRoots(ModuleSpec module)
	{
		List<Path> roots = new ArrayList<>(module.mainResources());

		if (module.datagenOutput() != null && !roots.contains(module.datagenOutput()))
		{
			roots.add(module.datagenOutput());
		}

		return roots;
	}

	/**
	 * Gets the generated Java roots for one module source set.
	 *
	 * <p>GameTest source sets are compiled without annotation processing and therefore never own
	 * generated roots.
	 *
	 * @param module        the module specification
	 * @param sourceSetName the source-set name
	 *
	 * @return the generated source roots
	 */
	public static List<Path> generatedRoots(ModuleSpec module, String sourceSetName)
	{
		if (SourceSetNames.isGameTest(sourceSetName))
		{
			return List.of();
		}

		if (SourceSetNames.CLIENT.equals(sourceSetName))
		{
			return module.generatedClientSources();
		}

		return module.generatedSources();
	}

	/**
	 * Checks whether one module materially has a client source set.
	 *
	 * @param module the module specification
	 *
	 * @return whether the module has client sources, resources, or generated client roots
	 */
	public static boolean hasClientSourceSet(ModuleSpec module)
	{
		return !module.clientSources().isEmpty()
		       || !module.clientResources().isEmpty()
		       || !module.generatedClientSources().isEmpty();
	}

	/**
	 * Checks whether one module materially has a common/server GameTest source set.
	 *
	 * @param module the module specification
	 *
	 * @return whether the module has GameTest sources or resources
	 */
	public static boolean hasGameTestSourceSet(ModuleSpec module)
	{
		return !module.gametestSources().isEmpty() || !module.gametestResources().isEmpty();
	}

	/**
	 * Checks whether one module materially has a client GameTest source set.
	 *
	 * @param module the module specification
	 *
	 * @return whether the module has client GameTest sources or resources
	 */
	public static boolean hasGameTestClientSourceSet(ModuleSpec module)
	{
		return !module.gametestClientSources().isEmpty() || !module.gametestClientResources().isEmpty();
	}

	/**
	 * Lists the GameTest source sets that one module materially has, common set first.
	 *
	 * @param module the module specification
	 *
	 * @return the GameTest source-set names, possibly empty
	 */
	public static List<String> gameTestSourceSets(ModuleSpec module)
	{
		List<String> names = new ArrayList<>();

		if (hasGameTestSourceSet(module))
		{
			names.add(SourceSetNames.GAMETEST);
		}

		if (hasGameTestClientSourceSet(module))
		{
			names.add(SourceSetNames.GAMETEST_CLIENT);
		}

		return names;
	}

	/**
	 * Checks whether one module has any GameTest source set.
	 *
	 * @param module the module specification
	 *
	 * @return whether the module has a common or client GameTest source set
	 */
	public static boolean hasGameTests(ModuleSpec module)
	{
		return hasGameTestSourceSet(module) || hasGameTestClientSourceSet(module);
	}
}
