package com.parzivail.toolchain.runtime;

/**
 * Where a prepared launch is going to be started.
 */
public enum LaunchHost
{
	/**
	 * An IntelliJ run configuration.
	 *
	 * <p>The toolchain writes IntelliJ launch modules and run configurations, and compiled outputs may
	 * not exist yet because IntelliJ builds them right before the launch.
	 */
	INTELLIJ,

	/**
	 * A headless child process started by the toolchain itself.
	 *
	 * <p>No IntelliJ metadata is written and every compiled output the launch needs must already exist.
	 */
	PROCESS
}
