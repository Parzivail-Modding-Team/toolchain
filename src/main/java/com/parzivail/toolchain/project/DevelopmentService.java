package com.parzivail.toolchain.project;

import com.parzivail.toolchain.fabric.FabricDataGenerationService;
import com.parzivail.toolchain.fabric.FabricDevLaunchService;
import com.parzivail.toolchain.fabric.FabricLaunchRequest;
import com.parzivail.toolchain.fabric.GameTestLaunchOptions;
import com.parzivail.toolchain.intellij.IntelliJProjectSyncService;
import com.parzivail.toolchain.intellij.IntelliJModuleNames;
import com.parzivail.toolchain.model.BuildGraph;
import com.parzivail.toolchain.model.GameTestModuleResolver;
import com.parzivail.toolchain.path.ToolchainPaths;
import com.parzivail.toolchain.runtime.LaunchEnvironment;
import com.parzivail.toolchain.runtime.LaunchHost;
import com.parzivail.toolchain.runtime.LaunchIdentity;
import com.parzivail.toolchain.runtime.LaunchKind;
import com.parzivail.toolchain.runtime.VanillaLaunchConfig;
import com.parzivail.toolchain.util.HostPlatform;
import com.parzivail.toolchain.util.ToolchainLog;

import java.io.IOException;
import java.nio.file.Files;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * High-level developer workflow helpers for the supported IntelliJ setup path.
 *
 * <p>This service keeps fresh-clone setup and day-to-day maintenance behind one obvious command:
 * synchronize IntelliJ metadata, then refresh the generated Fabric development launches.
 */
public final class DevelopmentService
{
	/**
	 * The combined result of the supported IntelliJ setup workflow.
	 *
	 * @param launches              the prepared client and server launches
	 * @param gameTestLaunches      the prepared client and server GameTest launches, empty when no module in
	 *                              the development closure declares GameTest source sets
	 * @param datagenConfigurations the prepared datagen configurations
	 */
	public record SetupResult(
			Map<LaunchEnvironment, VanillaLaunchConfig> launches,
			Map<LaunchEnvironment, VanillaLaunchConfig> gameTestLaunches,
			List<FabricDataGenerationService.DatagenConfiguration> datagenConfigurations
	)
	{
		/**
		 * Gets the prepared launch for one environment.
		 *
		 * @param environment the launch environment
		 *
		 * @return the prepared launch
		 */
		public VanillaLaunchConfig launch(LaunchEnvironment environment)
		{
			var launch = launches.get(environment);

			if (launch == null)
			{
				throw new IllegalArgumentException("Missing prepared launch for " + environment.id());
			}

			return launch;
		}
	}

	/**
	 * Runs the supported IntelliJ-first setup workflow for the requested injected module.
	 *
	 * @param refresh  whether external metadata and cached artifacts should be refreshed
	 * @param moduleId the optional requested injected module id
	 * @param identity the launch identity to embed in the generated run configurations
	 *
	 * @return the prepared launch and datagen configurations
	 *
	 * @throws IOException if setup fails
	 */
	public SetupResult setupIntelliJDevelopment(
			boolean refresh,
			String moduleId,
			LaunchIdentity identity
	) throws IOException
	{
		var repository = RepositoryContext.load();
		return setupIntelliJDevelopment(repository, refresh, moduleId, identity);
	}

	/**
	 * Runs the supported IntelliJ-first setup workflow for the requested injected module.
	 *
	 * @param repository the discovered repository context
	 * @param refresh    whether external metadata and cached artifacts should be refreshed
	 * @param moduleId   the optional requested injected module id
	 * @param identity   the launch identity to embed in the generated run configurations
	 *
	 * @return the prepared launch and datagen configurations
	 *
	 * @throws IOException if setup fails
	 */
	private SetupResult setupIntelliJDevelopment(
			RepositoryContext repository,
			boolean refresh,
			String moduleId,
			LaunchIdentity identity
	) throws IOException
	{
		var effectiveModuleId = effectiveDevelopmentModuleId(repository.buildGraph(), moduleId);
		ToolchainLog.info("dev", "Synchronizing IntelliJ metadata for " + repository.projectName());
		new IntelliJProjectSyncService().syncProject(refresh);
		var launchService = new FabricDevLaunchService();
		var datagenService = new FabricDataGenerationService();
		Map<LaunchEnvironment, VanillaLaunchConfig> launches = new EnumMap<>(LaunchEnvironment.class);

		for (var environment : LaunchEnvironment.values())
		{
			ToolchainLog.info(
					"dev",
					"Preparing Fabric " + environment.displayName().toLowerCase() + " launch for " + effectiveModuleId + " on Minecraft " + repository.minecraftVersion()
			);
			launches.put(
					environment,
					launchService.prepareLaunch(
							repository.minecraftVersion(),
							refresh,
							effectiveModuleId,
							environment,
							environment.effectiveIdentity(identity)
					)
			);
		}

		var gameTestLaunches = prepareGameTestLaunches(repository, launchService, refresh, effectiveModuleId);

		ToolchainLog.info("dev", "Preparing Fabric datagen launches");
		var datagenConfigurations = datagenService.prepareRunConfigurations(
				repository.minecraftVersion(),
				refresh,
				null,
				identity
		);

		return new SetupResult(Map.copyOf(launches), Map.copyOf(gameTestLaunches), List.copyOf(datagenConfigurations));
	}

	/**
	 * Prepares the IntelliJ GameTest launches when the development closure declares GameTest source sets,
	 * and removes the generated GameTest run configurations when it does not.
	 *
	 * @param repository      the discovered repository context
	 * @param launchService   the Fabric launch service
	 * @param refresh         whether cached runtime artifacts should be revalidated
	 * @param effectiveModuleId the injected development module id
	 *
	 * @return the prepared GameTest launches by environment
	 *
	 * @throws IOException if preparation or cleanup fails
	 */
	private Map<LaunchEnvironment, VanillaLaunchConfig> prepareGameTestLaunches(
			RepositoryContext repository,
			FabricDevLaunchService launchService,
			boolean refresh,
			String effectiveModuleId
	) throws IOException
	{
		Map<LaunchEnvironment, VanillaLaunchConfig> launches = new EnumMap<>(LaunchEnvironment.class);

		if (GameTestModuleResolver.launchModules(repository.buildGraph(), effectiveModuleId).isEmpty())
		{
			deleteGameTestRunConfigurations();
			return launches;
		}

		for (var environment : LaunchEnvironment.values())
		{
			ToolchainLog.info(
					"dev",
					"Preparing Fabric GameTest " + environment.displayName().toLowerCase() + " launch for " + effectiveModuleId
			);
			launches.put(
					environment,
					launchService.prepareLaunch(
							FabricLaunchRequest.gameTest(
									repository.minecraftVersion(),
									refresh,
									effectiveModuleId,
									environment,
									LaunchIdentity.gameTest(),
									ToolchainPaths.INTELLIJ_OUTPUT_DIRECTORY,
									LaunchHost.INTELLIJ,
									GameTestLaunchOptions.unrestricted()
							)
					)
			);
		}

		return launches;
	}

	/**
	 * Deletes the generated GameTest run configurations of the current platform.
	 *
	 * @throws IOException if a stale run configuration cannot be removed
	 */
	private void deleteGameTestRunConfigurations() throws IOException
	{
		var platformId = HostPlatform.current().id();

		for (var environment : LaunchEnvironment.values())
		{
			Files.deleteIfExists(
					ToolchainPaths.INTELLIJ_RUN_CONFIGS_DIRECTORY
							.resolve(IntelliJModuleNames.fabricRunConfigurationFileName(LaunchKind.GAMETEST, environment, platformId))
			);
		}
	}

	/**
	 * Resolves the effective module id for the supported development workflow.
	 *
	 * @param graph    the authoritative build graph
	 * @param moduleId the optional requested module id
	 *
	 * @return the effective module id
	 */
	public static String effectiveDevelopmentModuleId(
			BuildGraph graph,
			String moduleId
	)
	{
		if (moduleId == null || moduleId.isBlank())
		{
			return graph.developmentModuleId();
		}

		return moduleId;
	}
}
