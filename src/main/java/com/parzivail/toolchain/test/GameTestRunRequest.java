package com.parzivail.toolchain.test;

import com.parzivail.toolchain.runtime.LaunchEnvironment;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Describes one headless GameTest run.
 *
 * @param environments           the environments to run, in order
 * @param moduleId               the launch root module, or {@code null} for the graph's development module
 * @param serverFilter           the vanilla test selector restricting the server run, or {@code null}
 * @param clientModIds           the test-mod identifiers restricting the client run, or an empty list
 * @param serverTimeout          the wall-clock budget of the server process
 * @param clientTimeout          the wall-clock budget of the client process
 * @param refresh                whether cached runtime artifacts are revalidated
 * @param useIntelliJOutputs     whether the run uses IntelliJ's compiled outputs instead of compiling a fresh
 *                               toolchain-owned tree first
 * @param acceptEula             whether the run writes {@code eula=true} for the Minecraft EULA; this is only
 *                               ever done when the user explicitly asked for it
 * @param recordTemplatesModuleId the module whose GameTest resources receive screenshot templates that do not
 *                               exist yet, or {@code null} so that a missing template fails the test
 * @param echoOutput             whether the game's console output is echoed to the toolchain console
 */
public record GameTestRunRequest(
		List<LaunchEnvironment> environments,
		String moduleId,
		String serverFilter,
		List<String> clientModIds,
		Duration serverTimeout,
		Duration clientTimeout,
		boolean refresh,
		boolean useIntelliJOutputs,
		boolean acceptEula,
		String recordTemplatesModuleId,
		boolean echoOutput
)
{
	/**
	 * The default wall-clock budget of the server GameTest process.
	 */
	public static final Duration DEFAULT_SERVER_TIMEOUT = Duration.ofMinutes(10);

	/**
	 * The default wall-clock budget of the client GameTest process.
	 */
	public static final Duration DEFAULT_CLIENT_TIMEOUT = Duration.ofMinutes(20);

	/**
	 * Validates the request and copies its lists.
	 *
	 * @param environments            the environments to run
	 * @param moduleId                the launch root module
	 * @param serverFilter            the server test selector
	 * @param clientModIds            the client test-mod identifiers
	 * @param serverTimeout           the server wall-clock budget
	 * @param clientTimeout           the client wall-clock budget
	 * @param refresh                 whether cached runtime artifacts are revalidated
	 * @param useIntelliJOutputs      whether IntelliJ's outputs are used
	 * @param acceptEula              whether the EULA file is written
	 * @param recordTemplatesModuleId the screenshot template recording module
	 * @param echoOutput              whether the game's output is echoed
	 */
	public GameTestRunRequest
	{
		environments = List.copyOf(Objects.requireNonNull(environments, "environments"));
		clientModIds = List.copyOf(Objects.requireNonNull(clientModIds, "clientModIds"));
		Objects.requireNonNull(serverTimeout, "serverTimeout");
		Objects.requireNonNull(clientTimeout, "clientTimeout");

		if (environments.isEmpty())
		{
			throw new IllegalArgumentException("A GameTest run needs at least one environment");
		}
	}

	/**
	 * Gets the wall-clock budget of one environment.
	 *
	 * @param environment the launch environment
	 *
	 * @return the budget
	 */
	public Duration timeout(LaunchEnvironment environment)
	{
		return environment.isClient() ? clientTimeout : serverTimeout;
	}
}
