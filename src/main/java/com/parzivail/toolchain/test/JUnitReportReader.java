package com.parzivail.toolchain.test;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the JUnit-style XML reports written by Minecraft's GameTest reporter and by test harnesses that
 * follow the same shape.
 *
 * <p>Only {@code testcase} elements are interpreted: a {@code failure} below a case marks a failed required
 * test, a {@code skipped} below it marks a failed optional test, and a case with neither passed.
 */
public final class JUnitReportReader
{
	/**
	 * Prevents construction.
	 */
	private JUnitReportReader()
	{
	}

	/**
	 * Reads every test case from one report.
	 *
	 * @param report the report file
	 *
	 * @return the reported cases in document order
	 *
	 * @throws IOException if the report cannot be read or is not well-formed XML
	 */
	public static List<GameTestCaseResult> read(Path report) throws IOException
	{
		try (var input = Files.newInputStream(report))
		{
			var document = createBuilderFactory().newDocumentBuilder().parse(input);
			var testCases = document.getElementsByTagName("testcase");
			List<GameTestCaseResult> results = new ArrayList<>();

			for (var index = 0; index < testCases.getLength(); index++)
			{
				results.add(readCase((Element)testCases.item(index)));
			}

			return List.copyOf(results);
		}
		catch (ParserConfigurationException | SAXException exception)
		{
			throw new IOException("Failed to parse test report " + report, exception);
		}
	}

	/**
	 * Interprets one {@code testcase} element.
	 *
	 * @param testCase the element
	 *
	 * @return the case result
	 */
	private static GameTestCaseResult readCase(Element testCase)
	{
		var name = testCase.getAttribute("name");
		var failure = firstDescendant(testCase, "failure");

		if (failure != null)
		{
			return new GameTestCaseResult(name, GameTestCaseResult.Status.FAILED, failure.getAttribute("message"));
		}

		var skipped = firstDescendant(testCase, "skipped");

		if (skipped != null)
		{
			return new GameTestCaseResult(name, GameTestCaseResult.Status.SKIPPED, skipped.getAttribute("message"));
		}

		return new GameTestCaseResult(name, GameTestCaseResult.Status.PASSED, "");
	}

	/**
	 * Finds the first descendant element with a tag name.
	 *
	 * @param parent  the element to search below
	 * @param tagName the tag name
	 *
	 * @return the first matching element in document order, or {@code null}
	 */
	private static Element firstDescendant(Element parent, String tagName)
	{
		var children = parent.getElementsByTagName(tagName);
		return children.getLength() == 0 ? null : (Element)children.item(0);
	}

	/**
	 * Creates a document builder factory that refuses external entities and DTDs.
	 *
	 * @return the hardened factory
	 *
	 * @throws ParserConfigurationException if the parser does not support the required security features
	 */
	private static DocumentBuilderFactory createBuilderFactory() throws ParserConfigurationException
	{
		var factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		return factory;
	}
}
