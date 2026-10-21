package com.parzivail.toolchain.test;

import com.parzivail.toolchain.build.CiCompilationService;
import com.parzivail.toolchain.fabric.FabricDevLaunchService;
import com.parzivail.toolchain.fabric.FabricLaunchRequest;
import com.parzivail.toolchain.fabric.GameTestLaunchOptions;
import com.parzivail.toolchain.intellij.IntelliJRunConfigurationSupport;
import com.parzivail.toolchain.model.GameTestModuleResolver;
import com.parzivail.toolchain.model.ModuleAggregationResolver;
import com.parzivail.toolchain.path.ToolchainPaths;
import com.parzivail.toolchain.project.DevelopmentService;
import com.parzivail.toolchain.project.RepositoryContext;
import com.parzivail.toolchain.runtime.LaunchEnvironment;
import com.parzivail.toolchain.runtime.LaunchHost;
import com.parzivail.toolchain.runtime.LaunchIdentity;
import com.parzivail.toolchain.runtime.VanillaLaunchConfig;
import com.parzivail.toolchain.util.HostPlatform;
import com.parzivail.toolchain.util.ToolchainLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Runs Fabric GameTests headlessly as child processes of the toolchain.
 *
 * <p>The service owns the lifecycle around a run and nothing inside it: it compiles the test source sets,
 * prepares the generated GameTest launch, resets the isolated instance, starts Minecraft, enforces a
 * wall-clock budget by killing the OS process, and interprets the exit code and reports. Ticking,
 * scheduling, retries and reporting stay entirely with Minecraft's and Fabric's GameTest runners.
 *
 * <p>Reports are discovered by file name inside the results directory:
 * <ul>
 *   <li>{@code server-report.xml} is written by Fabric's GameTest server and is required;</li>
 *   <li>{@code client-report.xml} is optional and may be written by a client test harness in the same JUnit
 *   shape, which lets a client run report individual scenarios instead of only an exit code.</li>
 * </ul>
 */
public final class GameTestRunService
{
	/**
	 * The log scope of this service.
	 */
	private static final String LOG_SCOPE = "gametest";

	/**
	 * The optional client report file name inside the results directory.
	 */
	private static final String CLIENT_REPORT_FILE_NAME = "client-report.xml";

	/**
	 * The Java launcher argument file written into the results directory.
	 */
	private static final String ARGUMENT_FILE_NAME = "launch.argfile";

	/**
	 * The Minecraft EULA acceptance file inside a game directory.
	 */
	private static final String EULA_FILE_NAME = "eula.txt";

	/**
	 * The exit code of Minecraft's GameTest server when it found no test to run.
	 */
	private static final int NO_TESTS_EXIT_CODE = -1;

	/**
	 * How long a killed process is given to disappear before the run is reported.
	 */
	private static final Duration TERMINATION_GRACE = Duration.ofSeconds(10);

	/**
	 * How long the output pump is given to drain after the process has ended.
	 */
	private static final Duration OUTPUT_DRAIN_GRACE = Duration.ofSeconds(5);

	/**
	 * Generates the launches that this service starts.
	 */
	private final FabricDevLaunchService _launchService;

	/**
	 * Creates a GameTest run service.
	 */
	public GameTestRunService()
	{
		_launchService = new FabricDevLaunchService();
	}

	/**
	 * Runs the requested GameTest environments one after another.
	 *
	 * @param request the run request
	 *
	 * @return one result per requested environment, in request order
	 *
	 * @throws IOException if compilation, launch preparation or process management fails
	 */
	public List<GameTestEnvironmentResult> run(GameTestRunRequest request) throws IOException
	{
		var repository = RepositoryContext.load();
		var graph = repository.buildGraph();
		var rootModuleId = DevelopmentService.effectiveDevelopmentModuleId(graph, request.moduleId());
		var options = createLaunchOptions(request, repository, rootModuleId);

		if (GameTestModuleResolver.launchModules(graph, rootModuleId).isEmpty())
		{
			throw new IOException("No module in the closure of '" + rootModuleId + "' declares GameTest source sets");
		}

		var outputRoot = request.useIntelliJOutputs()
		                 ? ToolchainPaths.INTELLIJ_OUTPUT_DIRECTORY
		                 : new CiCompilationService().compileTestInputs(rootModuleId, request.refresh());
		List<GameTestEnvironmentResult> results = new ArrayList<>();

		for (var environment : request.environments())
		{
			var result = runEnvironment(repository, request, rootModuleId, outputRoot, options, environment);
			logResult(result);
			results.add(result);
		}

		return List.copyOf(results);
	}

	/**
	 * Builds the Fabric GameTest options implied by a run request.
	 *
	 * @param request       the run request
	 * @param repository    the discovered repository context
	 * @param rootModuleId  the launch root module identifier
	 *
	 * @return the GameTest launch options
	 */
	private GameTestLaunchOptions createLaunchOptions(
			GameTestRunRequest request,
			RepositoryContext repository,
			String rootModuleId
	)
	{
		Path templatesRoot = null;

		if (request.recordTemplatesModuleId() != null)
		{
			var module = ModuleAggregationResolver.requireModule(repository.buildGraph(), request.recordTemplatesModuleId());

			if (module.gametestResources().isEmpty())
			{
				throw new IllegalArgumentException("Module '" + module.id() + "' has no GameTest resource root to record templates into");
			}

			templatesRoot = ToolchainPaths.PROJECT_ROOT.resolve(module.gametestResources().getFirst());
		}

		return new GameTestLaunchOptions(request.serverFilter(), request.clientModIds(), templatesRoot);
	}

	/**
	 * Prepares, starts and interprets one environment's GameTest process.
	 *
	 * @param repository   the discovered repository context
	 * @param request      the run request
	 * @param rootModuleId the launch root module identifier
	 * @param outputRoot   the compiled output tree to launch from
	 * @param options      the GameTest launch options
	 * @param environment  the environment to run
	 *
	 * @return the environment result
	 *
	 * @throws IOException if preparation or process management fails
	 */
	private GameTestEnvironmentResult runEnvironment(
			RepositoryContext repository,
			GameTestRunRequest request,
			String rootModuleId,
			Path outputRoot,
			GameTestLaunchOptions options,
			LaunchEnvironment environment
	) throws IOException
	{
		ToolchainLog.info(LOG_SCOPE, "Preparing " + environment.id() + " GameTest launch for " + rootModuleId);
		var launch = _launchService.prepareLaunch(
				FabricLaunchRequest.gameTest(
						repository.minecraftVersion(),
						request.refresh(),
						rootModuleId,
						environment,
						LaunchIdentity.gameTest(),
						outputRoot,
						LaunchHost.PROCESS,
						options
				)
		);
		var resultsDirectory = ToolchainPaths.getGameTestResultsDirectory(
				repository.minecraftVersion(),
				environment,
				HostPlatform.current()
		);

		resetRunState(launch.workingDirectory(), resultsDirectory);

		if (request.acceptEula())
		{
			writeEulaAcceptance(launch.workingDirectory());
		}

		var argumentFile = writeArgumentFile(launch, resultsDirectory);
		var timeout = request.timeout(environment);

		ToolchainLog.info(
				LOG_SCOPE,
				"Starting " + environment.id() + " GameTest process (timeout " + timeout.toMinutes() + " min, results " + resultsDirectory + ")"
		);

		var processResult = runProcess(launch, argumentFile, resultsDirectory, environment, timeout, request.echoOutput());

		return interpret(environment, processResult, resultsDirectory);
	}

	/**
	 * Clears the previous run's state so that every run starts from the same empty game directory.
	 *
	 * <p>Both directories are always inside the toolchain's instance root; anything else is refused before a
	 * single file is deleted. The EULA acceptance file is the one thing deliberately preserved.
	 *
	 * @param gameDirectory    the instance's game directory
	 * @param resultsDirectory the instance's results directory
	 *
	 * @throws IOException if a directory is outside the instance root or cannot be cleared
	 */
	private void resetRunState(Path gameDirectory, Path resultsDirectory) throws IOException
	{
		requireInsideInstanceRoot(gameDirectory);
		requireInsideInstanceRoot(resultsDirectory);

		clearDirectory(gameDirectory, Set.of(EULA_FILE_NAME));
		clearDirectory(resultsDirectory, Set.of());
		Files.createDirectories(gameDirectory);
		Files.createDirectories(resultsDirectory);
	}

	/**
	 * Refuses directories outside the toolchain's instance root.
	 *
	 * @param directory the directory about to be cleared
	 *
	 * @throws IOException if the directory is not below the instance root
	 */
	private void requireInsideInstanceRoot(Path directory) throws IOException
	{
		var normalized = directory.toAbsolutePath().normalize();
		var instanceRoot = ToolchainPaths.INSTANCE_ROOT.toAbsolutePath().normalize();

		if (!normalized.startsWith(instanceRoot) || normalized.equals(instanceRoot))
		{
			throw new IOException("Refusing to clear a directory outside the instance root: " + normalized);
		}
	}

	/**
	 * Deletes the contents of one directory.
	 *
	 * @param directory the directory to clear
	 * @param keep      the direct child names that are preserved
	 *
	 * @throws IOException if deletion fails
	 */
	private void clearDirectory(Path directory, Set<String> keep) throws IOException
	{
		if (!Files.isDirectory(directory))
		{
			return;
		}

		List<Path> children;

		try (var entries = Files.list(directory))
		{
			children = entries.filter(entry -> !keep.contains(entry.getFileName().toString())).toList();
		}

		for (var child : children)
		{
			deleteTree(child);
		}
	}

	/**
	 * Deletes a file or directory tree.
	 *
	 * @param root the root to delete
	 *
	 * @throws IOException if deletion fails
	 */
	private void deleteTree(Path root) throws IOException
	{
		try (var paths = Files.walk(root))
		{
			for (var path : paths.sorted(Comparator.reverseOrder()).toList())
			{
				Files.deleteIfExists(path);
			}
		}
	}

	/**
	 * Writes the explicit EULA acceptance that the user asked for.
	 *
	 * @param gameDirectory the game directory
	 *
	 * @throws IOException if the file cannot be written
	 */
	private void writeEulaAcceptance(Path gameDirectory) throws IOException
	{
		Files.writeString(
				gameDirectory.resolve(EULA_FILE_NAME),
				"# Written by the toolchain because --accept-eula was passed.\n"
				+ "# By passing it you agree to the Minecraft EULA: https://aka.ms/MinecraftEULA\n"
				+ "eula=true\n",
				StandardCharsets.UTF_8
		);
		ToolchainLog.info(LOG_SCOPE, "Wrote eula=true because --accept-eula was passed");
	}

	/**
	 * Writes the complete Java command line as a launcher argument file.
	 *
	 * <p>A modern dev classpath easily exceeds the command-line length limits of the host, so the process is
	 * started with a single {@code @argfile} argument.
	 *
	 * @param launch           the prepared GameTest launch
	 * @param resultsDirectory the results directory that receives the file
	 *
	 * @return the argument file
	 *
	 * @throws IOException if the file cannot be written
	 */
	private Path writeArgumentFile(VanillaLaunchConfig launch, Path resultsDirectory) throws IOException
	{
		List<String> tokens = new ArrayList<>();
		var jvmArgs = launch.jvmArgs();

		for (var index = 0; index < jvmArgs.size(); index++)
		{
			var argument = jvmArgs.get(index);

			if ("-cp".equals(argument) || "-classpath".equals(argument))
			{
				index++;
				continue;
			}

			tokens.add(argument);
		}

		tokens.add("-cp");
		tokens.add(
				IntelliJRunConfigurationSupport.effectiveRuntimeClasspath(launch)
				                               .stream()
				                               .map(entry -> entry.toAbsolutePath().toString())
				                               .collect(Collectors.joining(File.pathSeparator))
		);
		tokens.add(launch.mainClass());
		tokens.addAll(launch.gameArgs());

		var argumentFile = resultsDirectory.resolve(ARGUMENT_FILE_NAME);
		Files.write(
				argumentFile,
				tokens.stream().map(GameTestRunService::quoteArgumentFileToken).toList(),
				StandardCharsets.UTF_8
		);
		return argumentFile;
	}

	/**
	 * Quotes one token for a Java launcher argument file.
	 *
	 * @param token the raw token
	 *
	 * @return the quoted token, with backslashes and quotes escaped
	 */
	private static String quoteArgumentFileToken(String token)
	{
		var builder = new StringBuilder("\"");

		for (var character : token.toCharArray())
		{
			if (character == '\\' || character == '"')
			{
				builder.append('\\');
			}

			builder.append(character);
		}

		return builder.append('"').toString();
	}

	/**
	 * The outcome of the child process itself, before any report interpretation.
	 *
	 * @param exitCode the exit code, or {@code -1} when the process was killed
	 * @param timedOut whether the wall-clock budget expired
	 * @param duration the wall-clock duration
	 */
	private record ProcessResult(
			int exitCode,
			boolean timedOut,
			Duration duration
	)
	{
	}

	/**
	 * Starts the game and waits for it, killing the OS process when the wall-clock budget expires.
	 *
	 * <p>The budget is enforced from outside the game on purpose: no game thread is ever interrupted or
	 * stopped, so a hung test can never leave world or network state half-mutated inside a live process.
	 *
	 * @param launch           the prepared GameTest launch
	 * @param argumentFile     the Java launcher argument file
	 * @param resultsDirectory the results directory
	 * @param environment      the launch environment
	 * @param timeout          the wall-clock budget
	 * @param echoOutput       whether the process output is echoed to the toolchain console
	 *
	 * @return the process outcome
	 *
	 * @throws IOException if the process cannot be started or the wait is interrupted
	 */
	private ProcessResult runProcess(
			VanillaLaunchConfig launch,
			Path argumentFile,
			Path resultsDirectory,
			LaunchEnvironment environment,
			Duration timeout,
			boolean echoOutput
	) throws IOException
	{
		var builder = new ProcessBuilder(launch.javaExecutable(), "@" + argumentFile.toAbsolutePath());
		builder.directory(launch.workingDirectory().toFile());
		builder.redirectErrorStream(true);

		var started = Instant.now();
		var process = builder.start();
		var outputLog = resultsDirectory.resolve(environment.id() + "-output.log");
		var outputPump = Thread.ofPlatform()
		                       .daemon()
		                       .name("gametest-" + environment.id() + "-output")
		                       .start(() -> pumpOutput(process, outputLog, echoOutput));

		try
		{
			var finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

			if (!finished)
			{
				ToolchainLog.info(LOG_SCOPE, "The " + environment.id() + " process exceeded its budget and is being killed");
				terminate(process);
			}

			outputPump.join(OUTPUT_DRAIN_GRACE.toMillis());

			return new ProcessResult(
					finished ? process.exitValue() : -1,
					!finished,
					Duration.between(started, Instant.now())
			);
		}
		catch (InterruptedException exception)
		{
			Thread.currentThread().interrupt();
			terminate(process);
			throw new IOException("Interrupted while waiting for the " + environment.id() + " GameTest process", exception);
		}
	}

	/**
	 * Kills a process and everything it spawned.
	 *
	 * <p>An interruption while waiting for the process to disappear is not swallowed: the thread's interrupt
	 * flag is restored so the caller observes it at its next blocking call.
	 *
	 * @param process the process to kill
	 */
	private void terminate(Process process)
	{
		process.descendants().forEach(ProcessHandle::destroyForcibly);
		process.destroyForcibly();

		try
		{
			process.waitFor(TERMINATION_GRACE.toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException exception)
		{
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Copies the process output into the results directory, optionally echoing it.
	 *
	 * @param process    the running process
	 * @param outputLog  the log file that receives the complete output
	 * @param echoOutput whether to echo each line to the toolchain console
	 */
	private void pumpOutput(Process process, Path outputLog, boolean echoOutput)
	{
		try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
		     var writer = Files.newBufferedWriter(outputLog, StandardCharsets.UTF_8))
		{
			String line;

			while ((line = reader.readLine()) != null)
			{
				writer.write(line);
				writer.newLine();
				writer.flush();

				if (echoOutput)
				{
					System.out.println(line);
				}
			}
		}
		catch (IOException exception)
		{
			ToolchainLog.info(LOG_SCOPE, "Stopped copying process output: " + exception.getMessage());
		}
	}

	/**
	 * Combines the process outcome with the written reports into an environment result.
	 *
	 * @param environment      the launch environment
	 * @param processResult    the process outcome
	 * @param resultsDirectory the results directory
	 *
	 * @return the environment result
	 *
	 * @throws IOException if a present report cannot be parsed
	 */
	private GameTestEnvironmentResult interpret(
			LaunchEnvironment environment,
			ProcessResult processResult,
			Path resultsDirectory
	) throws IOException
	{
		var reportName = environment.isClient() ? CLIENT_REPORT_FILE_NAME : FabricDevLaunchService.SERVER_REPORT_FILE_NAME;
		var report = resultsDirectory.resolve(reportName);
		List<GameTestCaseResult> cases = Files.isRegularFile(report) ? JUnitReportReader.read(report) : List.of();
		var failedCases = cases.stream().filter(testCase -> testCase.status() == GameTestCaseResult.Status.FAILED).count();

		GameTestEnvironmentResult.Outcome outcome;
		String detail;

		if (processResult.timedOut())
		{
			outcome = GameTestEnvironmentResult.Outcome.TIMED_OUT;
			detail = "The process was killed after exceeding its wall-clock budget.";
		}
		else if (environment.isServer() && !Files.isRegularFile(report))
		{
			outcome = GameTestEnvironmentResult.Outcome.ERROR;
			detail = "The server exited with code " + processResult.exitCode() + " before writing " + reportName
			         + (processResult.exitCode() == NO_TESTS_EXIT_CODE ? ", which Minecraft does when no test matched --filter or the server failed to start" : "")
			         + ". See " + environment.id() + "-output.log.";
		}
		else if (environment.isServer() && cases.isEmpty())
		{
			outcome = GameTestEnvironmentResult.Outcome.ERROR;
			detail = "The server report contains no test cases, so nothing was verified.";
		}
		else if (failedCases > 0 || processResult.exitCode() != 0)
		{
			outcome = GameTestEnvironmentResult.Outcome.FAILED;
			detail = failedCases > 0
			         ? failedCases + " required test(s) failed."
			         : "The process exited with code " + processResult.exitCode() + ". See " + environment.id() + "-output.log.";
		}
		else
		{
			outcome = GameTestEnvironmentResult.Outcome.PASSED;
			detail = "";
		}

		return new GameTestEnvironmentResult(
				environment,
				outcome,
				processResult.exitCode(),
				processResult.duration(),
				cases,
				detail,
				resultsDirectory
		);
	}

	/**
	 * Prints one environment result.
	 *
	 * @param result the result to print
	 */
	private void logResult(GameTestEnvironmentResult result)
	{
		var cases = result.cases();
		var passed = cases.stream().filter(testCase -> testCase.status() == GameTestCaseResult.Status.PASSED).count();
		var failed = cases.stream().filter(testCase -> testCase.status() == GameTestCaseResult.Status.FAILED).count();
		var skipped = cases.stream().filter(testCase -> testCase.status() == GameTestCaseResult.Status.SKIPPED).count();

		ToolchainLog.info(
				LOG_SCOPE,
				result.environment().id() + ": " + result.outcome() + " in " + result.duration().toSeconds() + "s ("
				+ cases.size() + " cases, " + passed + " passed, " + failed + " failed, " + skipped + " optional failures)"
		);

		for (var testCase : cases)
		{
			if (testCase.status() != GameTestCaseResult.Status.PASSED)
			{
				ToolchainLog.info(LOG_SCOPE, "  " + testCase.status() + " " + testCase.name() + ": " + testCase.message());
			}
		}

		if (!result.detail().isEmpty())
		{
			ToolchainLog.info(LOG_SCOPE, "  " + result.detail());
		}

		ToolchainLog.info(LOG_SCOPE, "  results: " + result.resultsDirectory());
	}
}
