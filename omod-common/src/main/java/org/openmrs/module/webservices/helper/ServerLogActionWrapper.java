/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.webservices.helper;

import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.util.MemoryAppender;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ServerLogActionWrapper used to serve the Server logs
 */
public abstract class ServerLogActionWrapper {

	/**
	 * Pre-compiled log-line parser for lines of the form
	 * {@code LEVEL - logger |timestamp| message}.
	 * <p>
	 * Every quantifier is linear and non-overlapping, so the pattern cannot backtrack
	 * catastrophically (ReDoS, CWE-1333): the two pipe-delimited fields are matched with a greedy
	 * {@code [^|]*} that is hard-bounded by the next {@code |} (it can never cross one, so there is
	 * no choice point to re-explore), and the trailing message is a single terminal {@code [\s\S]*}
	 * group with nothing after it to force backtracking. Crucially, no two adjacent quantifiers
	 * share a character: the previous {@code [^|]*?\s*} pair did ({@code \s} is a subset of
	 * {@code [^|]}), which is what let both the original {@code (.*\n*)+} pattern and the first
	 * rewrite run in polynomial time. The leading range is {@code [A-Za-z]} (was the overly
	 * permissive {@code [A-z]}, CWE-20). Trailing whitespace on the logger field is stripped by the
	 * caller. Compiled once and reused for every log line (previously recompiled per line).
	 */
	private static final Pattern LOG_LINE_PATTERN = Pattern
	        .compile("(INFO|ERROR|WARN|DEBUG)\\s+-\\s+([A-Za-z][^|]*)\\|([^|]*)\\|\\s?([\\s\\S]*)");

	public List<String[]> serverLog;
	
	public void setServerLog(List<String[]> serverLog) {
		this.serverLog = serverLog;
	}
	
	public List<String[]> getServerLog() {
		return serverLog;
	}
	
	/**
	 * Get server logs
	 * 
	 * @return List of last hundred server logs
	 */
	public List<String[]> getServerLogs() {
		// Check the GET_SERVER_LOGS privilege to serve the server logs
		Context.requirePrivilege(RestConstants.PRIV_GET_SERVER_LOGS);
		// Use the Memory Appender to retrieve the logs
		MemoryAppender memoryAppender = getMemoryAppender();

		if (memoryAppender == null) {
			return Collections.emptyList();
		}

		List<String> logLines = memoryAppender.getLogLines();
		List<String[]> finalOutput = new ArrayList<String[]>();
		for (String logLine : logLines) {
			String[] logElements = logLinePatternMatcher(logLine);
			finalOutput.add(logElements);
		}
		return finalOutput;
	}
	
	/**
	 * Match and find the patterns for log line
	 * 
	 * @param logLine Log lines from the terminal
	 * @return Array of matched patterns
	 */
	public String[] logLinePatternMatcher(String logLine) {
		String[] logElements = new String[4];
		Matcher matcher = LOG_LINE_PATTERN.matcher(logLine);
		if (matcher.find()) {
			// If pattern matches to the message
			logElements[0] = matcher.group(1);
			// group(2) greedily includes any whitespace before the '|'; trim it here instead of in
			// the regex (a trailing \s* in the pattern would overlap [^|]* and reintroduce ReDoS).
			logElements[1] = matcher.group(2).trim();
			logElements[2] = matcher.group(3);
			logElements[3] = matcher.group(4);
		}
		return logElements;
	}

	public abstract MemoryAppender getMemoryAppender();
}
