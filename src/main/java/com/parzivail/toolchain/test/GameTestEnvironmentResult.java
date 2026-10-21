package com.parzivail.toolchain.test;

import com.parzivail.toolchain.runtime.LaunchEnvironment;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * The outcome of running one GameTest environment.
 *
 * @param environment      the launch environment that ran
 * @param outcome          the overall outcome
 * @param exitCode         the process exit code, or {@code -1} when the process was killed
 * @param duration         the wall-clock duration of the launched process
 * @param cases            the cases read from the report, empty when no report was written
 * @param detail           a human-readable explanation of a non-passing outcome, empty for a pass
 * @param resultsDirectory the directory holding the reports, logs and diagnostics of the run
 */
public record GameTestEnvironmentResult(
		LaunchEnvironment environment,
		Outcome outcome,
		int exitCode,
		Duration duration,
		List<GameTestCaseResult> cases,
		String detail,
		Path resultsDirectory
)
{
	/**
	 * The overall outcome of one environment run.
	 */
	public enum Outcome
	{
		/**
		 * The process finished cleanly and no required test failed.
		 */
		PASSED,

		/**
		 * At least one required test failed, or a client run exited with an error.
		 */
		FAILED,

		/**
		 * The process exceeded its wall-clock budget and was killed.
		 */
		TIMED_OUT,

		/**
		 * The run could not produce a trustworthy result, for example the server exited before writing its
		 * report or discovered no tests at all.
		 */
		ERROR
	}

	/**
	 * Creates a result with a defensive copy of the cases.
	 *
	 * @param environment      the launch environment that ran
	 * @param outcome          the overall outcome
	 * @param exitCode         the process exit code
	 * @param duration         the wall-clock duration
	 * @param cases            the reported cases
	 * @param detail           the explanation of a non-passing outcome
	 * @param resultsDirectory the results directory
	 */
	public GameTestEnvironmentResult
	{
		cases = List.copyOf(cases);
	}

	/**
	 * Checks whether the run passed.
	 *
	 * @return {@code true} for {@link Outcome#PASSED}
	 */
	public boolean passed()
	{
		return outcome == Outcome.PASSED;
	}
}
