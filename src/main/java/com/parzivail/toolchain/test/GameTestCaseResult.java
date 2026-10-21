package com.parzivail.toolchain.test;

/**
 * The result of one GameTest case read back from a JUnit XML report.
 *
 * @param name    the test identifier
 * @param status  the case status
 * @param message the failure or skip message, empty for a passed case
 */
public record GameTestCaseResult(
		String name,
		Status status,
		String message
)
{
	/**
	 * The status of one reported case.
	 */
	public enum Status
	{
		/**
		 * The test passed.
		 */
		PASSED,

		/**
		 * A required test failed.
		 */
		FAILED,

		/**
		 * An optional test failed. Vanilla reports optional failures as skipped cases, and they never fail
		 * a run.
		 */
		SKIPPED
	}
}
