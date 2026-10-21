package com.parzivail.toolchain.fabric;

import java.nio.file.Path;
import java.util.List;

/**
 * Options that only apply to a Fabric GameTest launch.
 *
 * <p>Every field is optional. An empty option leaves the native Fabric behavior untouched, which means
 * "run every discovered test".
 *
 * @param serverFilter           the vanilla test selector restricting the server GameTest run, such as
 *                               {@code examplemod:*}, or {@code null} to run every test
 * @param clientModIds           the test-mod identifiers restricting the client GameTest run, or an empty
 *                               list to run every client test
 * @param recordedTemplatesRoot  the resource root that receives screenshot templates which do not exist yet,
 *                               or {@code null} so that a missing template fails the test instead
 */
public record GameTestLaunchOptions(
		String serverFilter,
		List<String> clientModIds,
		Path recordedTemplatesRoot
)
{
	/**
	 * Creates the options with defensive copies.
	 *
	 * @param serverFilter          the optional server test selector
	 * @param clientModIds          the client test-mod identifiers
	 * @param recordedTemplatesRoot the optional screenshot template output root
	 */
	public GameTestLaunchOptions
	{
		clientModIds = List.copyOf(clientModIds);
	}

	/**
	 * Gets the options that run every discovered test and record nothing.
	 *
	 * @return the unrestricted options
	 */
	public static GameTestLaunchOptions unrestricted()
	{
		return new GameTestLaunchOptions(null, List.of(), null);
	}
}
