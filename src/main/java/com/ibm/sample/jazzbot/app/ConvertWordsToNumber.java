package com.ibm.sample.jazzbot.app;

import java.util.HashMap;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

public class ConvertWordsToNumber {
	
	protected static HashMap<String, Integer> numbers;
	
	static {
		numbers = new HashMap<String, Integer>();
		
		numbers.put("one", 1);
		numbers.put("two", 2);
		numbers.put("three", 3);
		numbers.put("four", 4);
		numbers.put("five", 5);
		numbers.put("six", 6);
		numbers.put("seven", 7);
		numbers.put("eight", 8);
		numbers.put("nine", 9);
		numbers.put("ten", 10);
		numbers.put("eleven", 11);
		numbers.put("twelve", 12);
		numbers.put("thirteen", 13);
		numbers.put("fourteen", 14);
		numbers.put("fifteen", 15);
		numbers.put("sixteen", 16);
		numbers.put("seventeen", 17);
		numbers.put("eighteen", 18);
		numbers.put("nineteen", 19);
		numbers.put("twenty", 20);
//		numbers.put("twenty one", 21);
//		numbers.put("twenty two", 22);
//		numbers.put("twenty three", 23);
//		numbers.put("twenty four", 24);
//		numbers.put("twenty five", 25);
//		numbers.put("twenty six", 26);
//		numbers.put("twenty seven", 27);
//		numbers.put("twenty eight", 28);
//		numbers.put("twenty nine", 29);
//		numbers.put("thirty", 30);
	}
	
	private static Integer convertWords(String numberInWord) {
		Integer num = numbers.get(numberInWord);
		
		return num;
	}

	/**
	 * convert only 1 - 20
	 * @param numberStr word to be converted to number
	 * @return Integer format of the words, or null if not match found
	 */
	protected static Integer convert(String numberStr) {
		int userOptionPos = -1;
		if(StringUtils.isNumeric(numberStr))
			userOptionPos = Integer.parseInt(numberStr);
		else {
			String[] strArray = numberStr.split("\\s+");
			if(strArray.length >= 2 && numberStr.toLowerCase().startsWith("number")) {
				numberStr = strArray[1];
			}
			if(convertWords(numberStr) != null)
				userOptionPos = convertWords(numberStr); 
		}
		
		return userOptionPos;
	}

}
