package com.parzivail.toolchain.runtime;

/**
 * Launch-time player identity used for generated development launch configs.
 *
 * @param username the development username
 * @param uuid     the development UUID
 */
public record LaunchIdentity(
		String username,
		String uuid
)
{
	/**
	 * The default development username used for generated launches.
	 */
	public static final String DEFAULT_USERNAME = "Dev";

	/**
	 * The default development UUID used for generated launches.
	 */
	public static final String DEFAULT_UUID = "00000000-0000-0000-0000-000000000000";

	/**
	 * The username used for generated GameTest launches, matching Loom's GameTest default.
	 */
	public static final String GAMETEST_USERNAME = "Player0";

	/**
	 * Creates the fixed identity of GameTest launches.
	 *
	 * @return the GameTest identity
	 */
	public static LaunchIdentity gameTest()
	{
		return new LaunchIdentity(GAMETEST_USERNAME, DEFAULT_UUID);
	}

	/**
	 * Creates the default local development identity.
	 *
	 * @return the default development identity
	 */
	public static LaunchIdentity defaults()
	{
		return new LaunchIdentity(DEFAULT_USERNAME, DEFAULT_UUID);
	}
}
