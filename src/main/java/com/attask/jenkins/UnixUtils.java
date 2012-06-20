package com.attask.jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnixUtils {
	/**
	 * Reads the inputStream to the end, replacing any occurrence of the given
	 * pattern with the given replacement and writing that result to the given outputStream.
	 * Similar to the Unix tool 'sed'.
	 * @param inputStream The stream to read from. The stream is not closed.
	 * @param outputStream The stream to write to. The steram is not closed or flushed.
	 * @param patternReplacements A collection of replacements. These are applied line by line in the order the iterator gives them.
	 * @throws java.io.IOException
	 */
	public static void sed(InputStream inputStream, OutputStream outputStream, Map<Pattern, String> patternReplacements) throws IOException {
		if(patternReplacements == null || patternReplacements.isEmpty()) {
			return;
		}

		Scanner scanner = new Scanner(inputStream).useDelimiter(Pattern.compile("\n"));
		while(scanner.hasNext()) {
			String line = scanner.next();
			String next = null;
			for (Map.Entry<Pattern, String> patternReplacement : patternReplacements.entrySet()) {
				Matcher matcher;
				if(next == null) {
					matcher = patternReplacement.getKey().matcher(line);
				} else {
					matcher = patternReplacement.getKey().matcher(next);
				}
				next = matcher.replaceAll(patternReplacement.getValue());
			}
			if(next == null) {
				throw new RuntimeException("Woh nelly! This is bad. There is no reason this should ever happen!");
			}
			next += "\n";

			outputStream.write(next.getBytes());
		}
	}
}