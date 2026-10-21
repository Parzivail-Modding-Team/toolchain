package com.parzivail.toolchain.fabric;

import com.parzivail.toolchain.path.ToolchainPaths;
import com.parzivail.toolchain.runtime.LaunchEnvironment;
import com.parzivail.toolchain.runtime.LaunchHost;
import com.parzivail.toolchain.runtime.LaunchIdentity;
import com.parzivail.toolchain.runtime.LaunchKind;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Describes one Fabric launch preparation.
 *
 * @param versionId             the Minecraft version identifier
 * @param refresh               whether to revalidate cached runtime artifacts before launch preparation
 * @param moduleId              the optional module identifier to inject
 * @param environment           the launch environment
 * @param identity              the launch-time player identity for client launches
 * @param kind                  the launch kind
 * @param outputRoot            the compiled output tree that holds one directory per module source set
 * @param host                  where the launch will be started
 * @param gameTest              the GameTest options, present exactly for {@link LaunchKind#GAMETEST}
 */
public record FabricLaunchRequest(
		String versionId,
		boolean refresh,
		String moduleId,
		LaunchEnvironment environment,
		LaunchIdentity identity,
		LaunchKind kind,
		Path outputRoot,
		LaunchHost host,
		GameTestLaunchOptions gameTest
)
{
	/**
	 * Validates the request shape.
	 *
	 * @param versionId             the Minecraft version identifier
	 * @param refresh               whether to revalidate cached runtime artifacts
	 * @param moduleId              the optional module identifier to inject
	 * @param environment           the launch environment
	 * @param identity              the launch-time player identity
	 * @param kind                  the launch kind
	 * @param outputRoot            the compiled output tree
	 * @param host                  where the launch will be started
	 * @param gameTest              the GameTest options
	 */
	public FabricLaunchRequest
	{
		Objects.requireNonNull(versionId, "versionId");
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(identity, "identity");
		Objects.requireNonNull(kind, "kind");
		Objects.requireNonNull(outputRoot, "outputRoot");
		Objects.requireNonNull(host, "host");

		if (kind.includesGameTests() != (gameTest != null))
		{
			throw new IllegalArgumentException("GameTest options are required for, and only allowed with, GameTest launches");
		}
	}

	/**
	 * Creates the request of an ordinary IntelliJ-backed development launch.
	 *
	 * @param versionId   the Minecraft version identifier
	 * @param refresh     whether to revalidate cached runtime artifacts
	 * @param moduleId    the optional module identifier to inject
	 * @param environment the launch environment
	 * @param identity    the launch-time player identity
	 *
	 * @return the development launch request
	 */
	public static FabricLaunchRequest development(
			String versionId,
			boolean refresh,
			String moduleId,
			LaunchEnvironment environment,
			LaunchIdentity identity
	)
	{
		return new FabricLaunchRequest(
				versionId,
				refresh,
				moduleId,
				environment,
				identity,
				LaunchKind.DEVELOPMENT,
				ToolchainPaths.INTELLIJ_OUTPUT_DIRECTORY,
				LaunchHost.INTELLIJ,
				null
		);
	}

	/**
	 * Creates the request of a GameTest launch.
	 *
	 * @param versionId             the Minecraft version identifier
	 * @param refresh               whether to revalidate cached runtime artifacts
	 * @param moduleId              the module identifier to launch
	 * @param environment           the launch environment
	 * @param identity              the launch-time player identity
	 * @param outputRoot            the compiled output tree that holds the module and GameTest outputs
	 * @param host                  where the launch will be started
	 * @param options               the GameTest options
	 *
	 * @return the GameTest launch request
	 */
	public static FabricLaunchRequest gameTest(
			String versionId,
			boolean refresh,
			String moduleId,
			LaunchEnvironment environment,
			LaunchIdentity identity,
			Path outputRoot,
			LaunchHost host,
			GameTestLaunchOptions options
	)
	{
		return new FabricLaunchRequest(
				versionId,
				refresh,
				moduleId,
				environment,
				identity,
				LaunchKind.GAMETEST,
				outputRoot,
				host,
				options
		);
	}
}
