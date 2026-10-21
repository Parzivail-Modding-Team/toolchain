package com.parzivail.toolchain.path;

import java.nio.file.Path;

/**
 * Rooted path builder for a single module.
 */
public final class ModulePaths
{
	/**
	 * The module root path.
	 */
	private final Path _root;

	/**
	 * Creates a rooted module path helper.
	 *
	 * @param root the module root path string
	 */
	public ModulePaths(String root)
	{
		_root = Path.of(root);
	}

	/**
	 * Gets the module root path.
	 *
	 * @return the module root path
	 */
	public Path root()
	{
		return _root;
	}

	/**
	 * Gets the main Java source root.
	 *
	 * @return the main Java source root
	 */
	public Path mainJava()
	{
		return resolve("src/main/java");
	}

	/**
	 * Gets the client Java source root.
	 *
	 * @return the client Java source root
	 */
	public Path clientJava()
	{
		return resolve("src/client/java");
	}

	/**
	 * Gets the main resource root.
	 *
	 * @return the main resource root
	 */
	public Path mainResources()
	{
		return resolve("src/main/resources");
	}

	/**
	 * Gets the client resource root.
	 *
	 * @return the client resource root
	 */
	public Path clientResources()
	{
		return resolve("src/client/resources");
	}

	/**
	 * Gets the common/server GameTest Java source root.
	 *
	 * @return the GameTest Java source root
	 */
	public Path gametestJava()
	{
		return resolve("src/gametest/java");
	}

	/**
	 * Gets the common/server GameTest resource root.
	 *
	 * <p>This root also holds the test-only {@code fabric.mod.json}, test data packs and
	 * authored GameTest structures.
	 *
	 * @return the GameTest resource root
	 */
	public Path gametestResources()
	{
		return resolve("src/gametest/resources");
	}

	/**
	 * Gets the client GameTest Java source root.
	 *
	 * @return the client GameTest Java source root
	 */
	public Path gametestClientJava()
	{
		return resolve("src/gametest_client/java");
	}

	/**
	 * Gets the client GameTest resource root.
	 *
	 * @return the client GameTest resource root
	 */
	public Path gametestClientResources()
	{
		return resolve("src/gametest_client/resources");
	}

	/**
	 * Gets the standard main annotation processor output root.
	 *
	 * @return the generated source root
	 */
	public Path generatedAnnotationProcessorMain()
	{
		return resolve("build/generated/sources/annotationProcessor/java/main");
	}

	/**
	 * Gets the standard client annotation processor output root.
	 *
	 * @return the generated client source root
	 */
	public Path generatedAnnotationProcessorClient()
	{
		return resolve("build/generated/sources/annotationProcessor/java/client");
	}

	/**
	 * Gets the standard checked-in datagen output root.
	 *
	 * @return the datagen output root
	 */
	public Path generatedDatagen()
	{
		return resolve("src/main/generated");
	}

	/**
	 * Resolves a file under the main resources root.
	 *
	 * @param fileName the file name
	 *
	 * @return the resolved file path
	 */
	public Path mainResource(String fileName)
	{
		return mainResources().resolve(fileName);
	}

	/**
	 * Resolves a file under the GameTest resources root.
	 *
	 * @param fileName the file name
	 *
	 * @return the resolved file path
	 */
	public Path gametestResource(String fileName)
	{
		return gametestResources().resolve(fileName);
	}

	/**
	 * Resolves a file under the client resources root.
	 *
	 * @param fileName the file name
	 *
	 * @return the resolved file path
	 */
	public Path clientResource(String fileName)
	{
		return clientResources().resolve(fileName);
	}

	/**
	 * Resolves a relative path under the module root.
	 *
	 * @param relativePath the relative path
	 *
	 * @return the resolved path
	 */
	public Path resolve(String relativePath)
	{
		return _root.resolve(relativePath);
	}
}
