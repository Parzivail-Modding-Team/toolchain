package com.parzivail.toolchain.runtime;

/**
 * The purpose of one generated Fabric launch.
 *
 * <p>Every kind prepares its own isolated instance directory under {@code toolchain/work/instances},
 * so a GameTest run never touches the game directory, worlds or options of a development launch.
 */
public enum LaunchKind
{
	/**
	 * The ordinary interactive development launch.
	 */
	DEVELOPMENT("dev", "Fabric", ""),

	/**
	 * A Fabric GameTest launch.
	 *
	 * <p>The server environment runs Minecraft's headless GameTest server and the client environment runs
	 * Fabric's client GameTest runner. Both add the test-only source-set outputs of the launched modules.
	 */
	GAMETEST("gametest", "Fabric GameTest", "gametest-");

	/**
	 * The stable kind identifier used in generated metadata and diagnostics.
	 */
	private final String _id;

	/**
	 * The prefix of generated IntelliJ run-configuration display names.
	 */
	private final String _displayPrefix;

	/**
	 * The prefix this kind adds to generated, kind-scoped identifiers such as library names.
	 */
	private final String _scopePrefix;

	/**
	 * Creates a launch kind.
	 *
	 * @param id            the stable kind identifier
	 * @param displayPrefix the run-configuration display-name prefix
	 * @param scopePrefix   the prefix added to kind-scoped identifiers, empty for the default kind
	 */
	LaunchKind(String id, String displayPrefix, String scopePrefix)
	{
		_id = id;
		_displayPrefix = displayPrefix;
		_scopePrefix = scopePrefix;
	}

	/**
	 * Gets the stable kind identifier.
	 *
	 * @return the identifier
	 */
	public String id()
	{
		return _id;
	}

	/**
	 * Gets the prefix of generated IntelliJ run-configuration display names.
	 *
	 * @return the display-name prefix
	 */
	public String displayPrefix()
	{
		return _displayPrefix;
	}

	/**
	 * Scopes an environment identifier to this kind.
	 *
	 * <p>The development kind keeps plain environment identifiers so that existing generated names do not
	 * change; other kinds prepend their scope prefix.
	 *
	 * @param environment the launch environment
	 *
	 * @return the kind-scoped identifier, for example {@code client} or {@code gametest-client}
	 */
	public String scopedEnvironmentId(LaunchEnvironment environment)
	{
		return _scopePrefix + environment.id();
	}

	/**
	 * Gets the instance-directory name of this kind for one environment.
	 *
	 * @param environment the launch environment
	 *
	 * @return the instance-directory name
	 */
	public String instanceDirectoryName(LaunchEnvironment environment)
	{
		return "fabric-" + scopedEnvironmentId(environment);
	}

	/**
	 * Checks whether this kind launches the test-only source-set outputs.
	 *
	 * @return {@code true} for GameTest launches
	 */
	public boolean includesGameTests()
	{
		return this == GAMETEST;
	}
}
