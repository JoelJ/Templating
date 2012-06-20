package com.attask.jenkins;

import java.util.HashMap;
import java.util.Map;

/**
 * User: joeljohnson
 * Date: 3/19/12
 * Time: 10:55 AM
 */
public class CollectionUtils {
	/**
	 * Expands a string formatted as a properties file to a map.
	 * @param parameters String formatted as a properties file. Use # as comments
	 * @return
	 */
	public static Map<String, String> expandToMap(String parameters) {
		Map<String, String> result = new HashMap<String, String>();
		String[] split = parameters.split("\n");
		for (String s : split) {
			if (s.contains("#")) {
				s = s.substring(0, s.indexOf("#")).trim();
			}
			String[] keyValue = s.split("=", 2);
			if (keyValue.length == 2) {
				result.put(keyValue[0], keyValue[1]);
			}
		}
		return result;
	}
}
