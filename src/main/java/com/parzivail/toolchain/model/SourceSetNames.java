package com.parzivail.toolchain.model;

/**
 * Shared source-set names used across the authoritative graph and generated metadata.
 */
public final class SourceSetNames
{
	/**
	 * The conventional main source set.
	 */
	public static final String MAIN = "main";

	/**
	 * The conventional client-only source set.
	 */
	public static final String CLIENT = "client";

	/**
	 * The common/server GameTest source set.
	 *
	 * <p>Like {@link #MAIN}, it is compiled against the common Minecraft classes only, so a server
	 * test cannot reference client-only code by accident.
	 */
	public static final String GAMETEST = "gametest";

	/**
	 * The client GameTest source set.
	 *
	 * <p>Like {@link #CLIENT}, it is compiled against the client Minecraft classes and sees the client
	 * source sets of its dependencies.
	 */
	public static final String GAMETEST_CLIENT = "gametest_client";

	/**
	 * Prevents instantiation.
	 */
	private SourceSetNames()
	{
	}

	/**
	 * Checks whether a source set is compiled against the client Minecraft classes.
	 *
	 * @param sourceSetName the source-set name
	 *
	 * @return {@code true} for {@link #CLIENT} and {@link #GAMETEST_CLIENT}
	 */
	public static boolean isClientEnvironment(String sourceSetName)
	{
		return CLIENT.equals(sourceSetName) || GAMETEST_CLIENT.equals(sourceSetName);
	}

	/**
	 * Checks whether a source set holds test-only code that must never be packaged into a release
	 * artifact.
	 *
	 * @param sourceSetName the source-set name
	 *
	 * @return {@code true} for {@link #GAMETEST} and {@link #GAMETEST_CLIENT}
	 */
	public static boolean isGameTest(String sourceSetName)
	{
		return GAMETEST.equals(sourceSetName) || GAMETEST_CLIENT.equals(sourceSetName);
	}
}
