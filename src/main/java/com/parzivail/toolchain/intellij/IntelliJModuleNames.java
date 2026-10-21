package com.parzivail.toolchain.intellij;

import com.parzivail.toolchain.runtime.LaunchEnvironment;
import com.parzivail.toolchain.runtime.LaunchKind;

import java.util.Locale;

/**
 * Shared IntelliJ module and metadata naming conventions used by the toolchain.
 */
public final class IntelliJModuleNames
{
	/**
	 * Prevents construction.
	 */
	private IntelliJModuleNames()
	{
	}

	/**
	 * Builds the IntelliJ source-set module name for a module.
	 *
	 * @param projectName   the IntelliJ project name
	 * @param moduleId      the logical module identifier
	 * @param sourceSetName the source-set name
	 *
	 * @return the IntelliJ module name
	 */
	public static String sourceSetModuleName(String projectName, String moduleId, String sourceSetName)
	{
		return projectName + ".projects." + moduleId + "." + sourceSetName;
	}

	/**
	 * Builds the IntelliJ module file name for a source-set module.
	 *
	 * @param projectName   the IntelliJ project name
	 * @param moduleId      the logical module identifier
	 * @param sourceSetName the source-set name
	 *
	 * @return the `.iml` file name
	 */
	public static String sourceSetModuleFileName(String projectName, String moduleId, String sourceSetName)
	{
		return sourceSetModuleName(projectName, moduleId, sourceSetName) + ".iml";
	}

	/**
	 * Builds the IntelliJ module name for a generated Fabric launch classpath module.
	 *
	 * @param projectName   the IntelliJ project name
	 * @param environmentId the launch environment identifier
	 * @param platformId    the target platform identifier
	 *
	 * @return the launch module name
	 */
	public static String fabricLaunchModuleName(String projectName, String environmentId, String platformId)
	{
		return projectName + ".launch.fabric." + environmentId + "." + platformId;
	}

	/**
	 * Builds the IntelliJ module file name for a generated Fabric launch classpath module.
	 *
	 * @param projectName   the IntelliJ project name
	 * @param environmentId the launch environment identifier
	 * @param platformId    the target platform identifier
	 *
	 * @return the launch module file name
	 */
	public static String fabricLaunchModuleFileName(String projectName, String environmentId, String platformId)
	{
		return fabricLaunchModuleName(projectName, environmentId, platformId) + ".iml";
	}

	/**
	 * Builds the IntelliJ module name for a generated Fabric launch classpath module of one launch kind.
	 *
	 * @param projectName the IntelliJ project name
	 * @param kind        the launch kind
	 * @param environment the launch environment
	 * @param platformId  the target platform identifier
	 *
	 * @return the launch module name
	 */
	public static String fabricLaunchModuleName(String projectName, LaunchKind kind, LaunchEnvironment environment, String platformId)
	{
		return projectName + ".launch.fabric." + kind.scopedEnvironmentId(environment) + "." + platformId;
	}

	/**
	 * Builds the IntelliJ module file name for a generated Fabric launch classpath module of one launch
	 * kind.
	 *
	 * @param projectName the IntelliJ project name
	 * @param kind        the launch kind
	 * @param environment the launch environment
	 * @param platformId  the target platform identifier
	 *
	 * @return the launch module file name
	 */
	public static String fabricLaunchModuleFileName(String projectName, LaunchKind kind, LaunchEnvironment environment, String platformId)
	{
		return fabricLaunchModuleName(projectName, kind, environment, platformId) + ".iml";
	}

	/**
	 * Builds the generated IntelliJ Fabric run-configuration file name for one launch kind, environment
	 * and platform.
	 *
	 * @param kind        the launch kind
	 * @param environment the launch environment
	 * @param platformId  the target platform identifier
	 *
	 * @return the run-configuration file name
	 */
	public static String fabricRunConfigurationFileName(LaunchKind kind, LaunchEnvironment environment, String platformId)
	{
		if (kind == LaunchKind.DEVELOPMENT)
		{
			return fabricRunConfigurationFileName(environment.id(), platformId);
		}

		return "Fabric_GameTest_" + environment.id().substring(0, 1).toUpperCase(Locale.ROOT)
		       + environment.id().substring(1).toLowerCase(Locale.ROOT)
		       + "_" + platformId.toUpperCase(Locale.ROOT) + ".xml";
	}

	/**
	 * Builds the project-library name prefix of the generated launch libraries of one launch kind.
	 *
	 * @param kind        the launch kind
	 * @param environment the launch environment
	 * @param platformId  the target platform identifier
	 *
	 * @return the library name prefix, which ends with a dash
	 */
	public static String fabricLaunchLibraryPrefix(LaunchKind kind, LaunchEnvironment environment, String platformId)
	{
		return "fabric-launch-" + kind.scopedEnvironmentId(environment) + "-" + platformId + "-";
	}

	/**
	 * Builds the IntelliJ module name for the aggregate Fabric datagen launch classpath module.
	 *
	 * @param projectName the IntelliJ project name
	 * @param platformId  the target platform identifier
	 *
	 * @return the datagen launch module name
	 */
	public static String fabricDatagenLaunchModuleName(String projectName, String platformId)
	{
		return projectName + ".launch.fabric.datagen." + platformId;
	}

	/**
	 * Builds the IntelliJ module file name for the aggregate Fabric datagen launch classpath module.
	 *
	 * @param projectName the IntelliJ project name
	 * @param platformId  the target platform identifier
	 *
	 * @return the datagen launch module file name
	 */
	public static String fabricDatagenLaunchModuleFileName(String projectName, String platformId)
	{
		return fabricDatagenLaunchModuleName(projectName, platformId) + ".iml";
	}

	/**
	 * Builds the generated IntelliJ Fabric run-configuration file name for one environment and
	 * platform.
	 *
	 * @param environmentId the launch environment identifier
	 * @param platformId    the target platform identifier
	 *
	 * @return the run-configuration file name
	 */
	public static String fabricRunConfigurationFileName(String environmentId, String platformId)
	{
		return "Fabric_" + environmentId.substring(0, 1).toUpperCase(Locale.ROOT)
		       + environmentId.substring(1).toLowerCase(Locale.ROOT)
		       + "_" + platformId.toUpperCase(Locale.ROOT) + ".xml";
	}

	/**
	 * Builds the generated IntelliJ Fabric datagen run-configuration file name for one module and platform.
	 *
	 * @param moduleId   the logical module identifier
	 * @param platformId the target platform identifier
	 *
	 * @return the datagen run-configuration file name
	 */
	public static String fabricDatagenRunConfigurationFileName(String moduleId, String platformId)
	{
		return "Fabric_Datagen_" + moduleId + "_" + platformId.toUpperCase(Locale.ROOT) + ".xml";
	}
}
