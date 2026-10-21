package com.parzivail.toolchain.artifact;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Proves that a packaged artifact contains no GameTest content.
 *
 * <p>GameTest source sets are modeled as separate modules that the artifact assembly never reads, so
 * leaking them requires a misconfiguration. The audit turns that misconfiguration into a hard failure of
 * the assembly instead of a surprise in a release:
 * <ul>
 *   <li>a packaged {@code fabric.mod.json} must not declare a GameTest entrypoint, because Fabric would
 *   then register the tests of a player's game;</li>
 *   <li>no packaged entry may occupy the same path as a file of one of the module's GameTest outputs.</li>
 * </ul>
 */
final class GameTestArtifactAudit
{
	/**
	 * The descriptor entry that Fabric reads entrypoints from.
	 */
	private static final String DESCRIPTOR_ENTRY = "fabric.mod.json";

	/**
	 * The Fabric entrypoint keys that only GameTest launches may use.
	 */
	private static final Set<String> GAMETEST_ENTRYPOINT_KEYS = Set.of("fabric-gametest", "fabric-client-gametest");

	/**
	 * Prevents construction.
	 */
	private GameTestArtifactAudit()
	{
	}

	/**
	 * Verifies one artifact's entries.
	 *
	 * @param artifactId      the artifact identifier, used in failure messages
	 * @param entries         the jar entries about to be packaged
	 * @param gameTestOutputs the compiled GameTest output roots of the module, which may not exist
	 * @param mapper          the shared JSON reader
	 *
	 * @throws IOException if the artifact contains GameTest content or a file cannot be read
	 */
	static void verify(
			String artifactId,
			Map<String, byte[]> entries,
			List<Path> gameTestOutputs,
			ObjectMapper mapper
	) throws IOException
	{
		verifyNoGameTestEntrypoints(artifactId, entries, mapper);

		for (var gameTestOutput : gameTestOutputs)
		{
			verifyNoSharedPaths(artifactId, entries, gameTestOutput);
		}
	}

	/**
	 * Rejects a descriptor that declares a GameTest entrypoint.
	 *
	 * @param artifactId the artifact identifier
	 * @param entries    the jar entries
	 * @param mapper     the shared JSON reader
	 *
	 * @throws IOException if a GameTest entrypoint is declared or the descriptor cannot be parsed
	 */
	private static void verifyNoGameTestEntrypoints(
			String artifactId,
			Map<String, byte[]> entries,
			ObjectMapper mapper
	) throws IOException
	{
		var descriptorBytes = entries.get(DESCRIPTOR_ENTRY);

		if (descriptorBytes == null)
		{
			return;
		}

		var entrypoints = mapper.readTree(descriptorBytes).path("entrypoints");

		for (var key : GAMETEST_ENTRYPOINT_KEYS)
		{
			if (entrypoints.has(key))
			{
				throw new IOException("Artifact '" + artifactId + "' declares the '" + key + "' entrypoint; GameTest content must never be packaged");
			}
		}
	}

	/**
	 * Rejects an artifact that packages a file from a GameTest output.
	 *
	 * @param artifactId     the artifact identifier
	 * @param entries        the jar entries
	 * @param gameTestOutput the compiled GameTest output root
	 *
	 * @throws IOException if a path is shared or the output cannot be walked
	 */
	private static void verifyNoSharedPaths(
			String artifactId,
			Map<String, byte[]> entries,
			Path gameTestOutput
	) throws IOException
	{
		if (!Files.isDirectory(gameTestOutput))
		{
			return;
		}

		try (var paths = Files.walk(gameTestOutput))
		{
			for (var path : paths.filter(Files::isRegularFile).toList())
			{
				var relativePath = gameTestOutput.relativize(path).toString().replace('\\', '/');

				if (!DESCRIPTOR_ENTRY.equals(relativePath) && entries.containsKey(relativePath))
				{
					throw new IOException("Artifact '" + artifactId + "' packages '" + relativePath + "', which belongs to the GameTest output " + gameTestOutput);
				}
			}
		}
	}
}
