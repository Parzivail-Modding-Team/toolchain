package com.parzivail.toolchain.fabric;

import com.parzivail.toolchain.intellij.IntelliJModuleNames;
import com.parzivail.toolchain.model.ModuleSpec;
import com.parzivail.toolchain.model.SourceSetLayout;
import com.parzivail.toolchain.model.SourceSetNames;
import com.parzivail.toolchain.path.ToolchainPaths;
import com.parzivail.toolchain.util.ToolchainLog;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves compiled output roots for modeled modules participating in Fabric workflows.
 *
 * <p>Outputs come from IntelliJ's {@code out/production} tree by default. The headless GameTest runner
 * compiles into the toolchain-owned CI tree instead and passes that root explicitly.
 */
public final class FabricModuleOutputResolver
{
	/**
	 * Prevents construction.
	 */
	private FabricModuleOutputResolver()
	{
	}

	/**
	 * Resolves the IntelliJ output roots for one module.
	 *
	 * @param projectName   the IntelliJ project name
	 * @param module        the module specification
	 * @param includeClient whether client outputs should be included
	 * @param logCategory   the toolchain log category
	 * @param logMissing    whether missing outputs should be logged
	 *
	 * @return the ordered output roots
	 */
	public static List<Path> resolveOutputRoots(
			String projectName,
			ModuleSpec module,
			boolean includeClient,
			String logCategory,
			boolean logMissing
	)
	{
		return resolveOutputRoots(
				ToolchainPaths.INTELLIJ_OUTPUT_DIRECTORY,
				projectName,
				module,
				includeClient,
				logCategory,
				logMissing
		);
	}

	/**
	 * Resolves the output roots for one module from an explicit compiled output tree.
	 *
	 * @param outputRoot    the compiled output tree that holds one directory per module source set
	 * @param projectName   the IntelliJ project name
	 * @param module        the module specification
	 * @param includeClient whether client outputs should be included
	 * @param logCategory   the toolchain log category
	 * @param logMissing    whether missing outputs should be logged
	 *
	 * @return the ordered output roots
	 */
	public static List<Path> resolveOutputRoots(
			Path outputRoot,
			String projectName,
			ModuleSpec module,
			boolean includeClient,
			String logCategory,
			boolean logMissing
	)
	{
		List<Path> roots = new ArrayList<>(resolveSourceSetOutputRoots(outputRoot, projectName, module, SourceSetNames.MAIN, logCategory, logMissing));

		if (includeClient)
		{
			roots.addAll(resolveSourceSetOutputRoots(outputRoot, projectName, module, SourceSetNames.CLIENT, logCategory, logMissing));
		}

		return roots.stream().distinct().toList();
	}

	/**
	 * Resolves the GameTest output roots for one module.
	 *
	 * <p>The common GameTest output is always included and the client GameTest output only for a client
	 * launch, mirroring how production outputs are split.
	 *
	 * @param outputRoot    the compiled output tree that holds one directory per module source set
	 * @param projectName   the IntelliJ project name
	 * @param module        the module specification
	 * @param includeClient whether client GameTest outputs should be included
	 * @param logCategory   the toolchain log category
	 * @param logMissing    whether missing outputs should be logged
	 *
	 * @return the ordered GameTest output roots
	 */
	public static List<Path> resolveGameTestOutputRoots(
			Path outputRoot,
			String projectName,
			ModuleSpec module,
			boolean includeClient,
			String logCategory,
			boolean logMissing
	)
	{
		List<Path> roots = new ArrayList<>();

		if (SourceSetLayout.hasGameTestSourceSet(module))
		{
			roots.addAll(resolveSourceSetOutputRoots(outputRoot, projectName, module, SourceSetNames.GAMETEST, logCategory, logMissing));
		}

		if (includeClient && SourceSetLayout.hasGameTestClientSourceSet(module))
		{
			roots.addAll(resolveSourceSetOutputRoots(outputRoot, projectName, module, SourceSetNames.GAMETEST_CLIENT, logCategory, logMissing));
		}

		return roots;
	}

	/**
	 * Resolves the IntelliJ output root for one module source set.
	 *
	 * @param projectName   the IntelliJ project name
	 * @param module        the module specification
	 * @param sourceSetName the source set name
	 * @param logCategory   the toolchain log category
	 * @param logMissing    whether missing outputs should be logged
	 *
	 * @return the ordered output roots
	 */
	public static List<Path> resolveSourceSetOutputRoots(
			String projectName,
			ModuleSpec module,
			String sourceSetName,
			String logCategory,
			boolean logMissing
	)
	{
		return resolveSourceSetOutputRoots(
				ToolchainPaths.INTELLIJ_OUTPUT_DIRECTORY,
				projectName,
				module,
				sourceSetName,
				logCategory,
				logMissing
		);
	}

	/**
	 * Resolves the output root for one module source set from an explicit compiled output tree.
	 *
	 * @param outputRoot    the compiled output tree that holds one directory per module source set
	 * @param projectName   the IntelliJ project name
	 * @param module        the module specification
	 * @param sourceSetName the source set name
	 * @param logCategory   the toolchain log category
	 * @param logMissing    whether missing outputs should be logged
	 *
	 * @return the ordered output roots
	 */
	public static List<Path> resolveSourceSetOutputRoots(
			Path outputRoot,
			String projectName,
			ModuleSpec module,
			String sourceSetName,
			String logCategory,
			boolean logMissing
	)
	{
		var moduleName = IntelliJModuleNames.sourceSetModuleName(projectName, module.id(), sourceSetName);
		var moduleOutput = outputRoot.resolve(moduleName);

		if (Files.isDirectory(moduleOutput))
		{
			ToolchainLog.info(logCategory, "Using compiled output for " + module.id() + "." + sourceSetName + ": " + moduleOutput);
			return List.of(moduleOutput);
		}

		if (logMissing)
		{
			ToolchainLog.info(
					logCategory,
					"No compiled output found for " + module.id() + "." + sourceSetName + " under " + outputRoot + ". Build the generated Fabric development configuration in IntelliJ first."
			);
		}

		return List.of();
	}
}
