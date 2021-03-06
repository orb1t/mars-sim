/**
 * Mars Simulation Project
 * Conversion.java
 * @version 3.1.0 2017-03-31
 * @author Manny Kung
 */

package org.mars_sim.msp.core.tool;

public class Conversion {

	public Conversion() {

	}

//	public void capitalize(String nameStr) {
//
//		// convert nameStr down into an array
//		// create new String at each whitespace
//
//		// at each word, do
//
//		String word = null;
//		word = word.substring(0,1).toUpperCase()+ word.substring(1).toLowerCase();
//
//		// convert the array back to one single String
//
//	}

	/**
	 * Checks if the initial of the string is a vowel
	 * 
	 * @param word
	 * @return true/false
	 */
	public static boolean isVowel(String word) {
		if (word.toLowerCase().startsWith("a") || word.toLowerCase().startsWith("e")
				|| word.toLowerCase().startsWith("i") || word.toLowerCase().startsWith("o")
				|| word.toLowerCase().startsWith("u"))
			return true;
		else
			return false;
	}

	/**
	 * Sets the first word to lower case
	 * 
	 * @param input the word
	 * @return modified word
	 */
	public static String setFirstWordLowercase(String input) {
		if (input != null) {
			StringBuilder titleCase = new StringBuilder();
			boolean nextTitleCase = true;

			for (char c : input.toCharArray()) {
				if (Character.isSpaceChar(c) || c == '/') {
					nextTitleCase = true;
				} else if (nextTitleCase) {
					c = Character.toLowerCase(c);
					nextTitleCase = false;
				}

				titleCase.append(c);
			}

			return titleCase.toString().replaceAll("eVA", "EVA");
		} else
			return null;
//		
//		StringBuilder s = new StringBuilder();
//		if (!input.substring(0, 3).equals("EVA")) {
//			s.append(input.substring(0, 1).toLowerCase());
//			s.append(input.substring(1, input.length()));
//		}
//
//		return s.toString();
	}

	/**
	 * Capitalizes the input word
	 * 
	 * @param input
	 * @return the modified word
	 */
	public static String capitalize(String input) {
		if (input != null) {
			StringBuilder titleCase = new StringBuilder();
			boolean nextTitleCase = true;

			for (char c : input.toCharArray()) {
				if (Character.isSpaceChar(c) || c == '(') {
					nextTitleCase = true;
				} else if (nextTitleCase) {
					c = Character.toTitleCase(c);
					nextTitleCase = false;
				}

				titleCase.append(c);
			}

			return titleCase.toString();
		} else
			return null;
	}

	public static boolean isInteger(String s, int radix) {
	    if(s.isEmpty()) return false;
	    for(int i = 0; i < s.length(); i++) {
	        if(i == 0 && s.charAt(i) == '-') {
	            if(s.length() == 1) return false;
	            else continue;
	        }
	        if(Character.digit(s.charAt(i),radix) < 0) return false;
	    }
	    return true;
	}
	
	/**
	 * <p>
	 * Checks if a String is whitespace, empty ("") or null.
	 * </p>
	 *
	 * <pre>
	 * StringUtils.isBlank(null)      = true
	 * StringUtils.isBlank("")        = true
	 * StringUtils.isBlank(" ")       = true
	 * StringUtils.isBlank("bob")     = false
	 * StringUtils.isBlank("  bob  ") = false
	 * </pre>
	 *
	 * @param str the String to check, may be null
	 * @return <code>true</code> if the String is null, empty or whitespace
	 * @since 2.0
	 * @author commons.apache.org
	 */
	public static boolean isBlank(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if (!(Character.isWhitespace(str.charAt(i)))) {
				return false;
			}
		}
		return true;
	}

	
}
