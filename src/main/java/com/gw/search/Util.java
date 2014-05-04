package com.gw.search;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

	private static Logger logger = LoggerFactory.getLogger(Util.class);
	private static final String inputFormat = "yyyyMMddHHmmss";
	private static final String outputFormat = "yyyy-MM-dd HH:mm:ss";
	private static SimpleDateFormat inputSdf = new SimpleDateFormat(inputFormat);
	private static SimpleDateFormat outputSdf = new SimpleDateFormat(
			outputFormat);

	public static String convertDateFormat(String inputString) {
		Date pdate = null;
		try {
			pdate = inputSdf.parse(inputString);
		} catch (Exception e) {
			logger.error("parse date error:\n", e);

		}
		String finalDate = outputSdf.format(pdate);
		return finalDate;
	}

	public static String getHighlightSummary(int summaryLength, String orignString) {

		String outputString = null;
		// System.out.println("nContentArrayeeee:\n" +
		// orignString);
		/**
		 * "(?<=。)"保留分割后的句号
		 */
		String[] orignStringArray = orignString.split("(?<=。)|(?<=，)|(?<=,)|(?<=\n)|(?<=？)|(?<=、)");
		for (String temp : orignStringArray) {
			logger.debug("in array\n" + temp);
		}

		/**
		 * 找到第一个包含高亮标签的句子，下标保存在firstMatchingString中
		 */
		String regEx = "</em>";
		Pattern pat = null;
		Matcher mat = null;
		int firstMatchingString = -1;

		for (int i = 0; i < orignStringArray.length; i++) {
			pat = Pattern.compile(regEx);
			mat = pat.matcher(orignStringArray[i]);
			if (mat.find()) {
				firstMatchingString = i;
				break;
			}

		}
		String noTagString = null;
		/**
		 * 拼接出从第一个包含高亮标签的句子开始的字符串，firstMatchingString应该不可能为-1
		 */
		String newContentString = null;
		if (firstMatchingString != -1) {
			StringBuffer newContentBuffer = new StringBuffer();
			for (int i = firstMatchingString; i < orignStringArray.length; i++) {
				newContentBuffer.append(orignStringArray[i]);
			}
			newContentString = newContentBuffer.toString();
			orignStringArray = newContentString.split("(?<=。)|(?<=，)|(?<=,)|(?<=\n)|(?<=？)|(?<=、)");
			noTagString = newContentString.replaceAll("\\<.*?>", "");

		} else {
			noTagString = orignString.replaceAll("\\<.*?>", "");
		}

		/**
		 * "(?<=。)"保留分割后的句号
		 */
		String[] noTagStringArray = noTagString.split("(?<=。)|(?<=，)|(?<=,)|(?<=\n)|(?<=？)|(?<=、)");
		long lengthSofar = 0;
		int handleIndex = -1;
		for (int i = 0; i < noTagStringArray.length; i++) {
			lengthSofar += noTagStringArray[i].length();
			if (lengthSofar > summaryLength) {
				handleIndex = i;
				break;
			}
		}
		/**
		 * 高亮出现在第一个句子之中，如果这个句子长度小于等于需要返回的长度，就直接返回这个句子。
		 * 如果大于需要返回的长度，就将这个句子使用逗号问号和顿号进行切分，从用高亮标签的那一段开始累加， 一直累加到需要的字数为止。
		 */
		if (handleIndex == 0) {
			logger.debug("高亮字段出现在第一个句子之中");
			logger.debug(orignStringArray[0]);
			if (noTagStringArray[0].length() <= summaryLength) {

				outputString = orignStringArray[0];
				outputString = outputString.replace((char) 12288, ' ');

				return outputString.trim();
			} else {
				String noTagsString = orignStringArray[0].replaceAll("\\<.*?>",
						"");
				String[] splitNoTagsStringArray = noTagsString
						.split("(?<=。)|(?<=，)|(?<=,)|(?<=\n)|(?<=？)|(?<=、)");
				String[] splitWithTagStringArray = orignStringArray[0]
						.split("(?<=。)|(?<=，)|(?<=,)|(?<=\n)|(?<=？)|(?<=、)");
				String reg = "</em>";
				Pattern pattern = null;
				Matcher matcher = null;
				int firstMatchedStringIndex = -1;
				for (int i = 0; i < splitWithTagStringArray.length; i++) {
					pattern = Pattern.compile(regEx);
					matcher = pat.matcher(splitWithTagStringArray[i]);
					if (matcher.find()) {
						firstMatchedStringIndex = i;
						break;
					}
				}
				long lengthSee = 0;
				int endIndex = -1;
				for (int j = firstMatchedStringIndex; j < splitNoTagsStringArray.length; j++) {
					lengthSee += splitNoTagsStringArray[j].length();
					if (lengthSee > summaryLength
							&& j > firstMatchedStringIndex) {
						endIndex = j - 1;
						break;
					}
				}
				if (lengthSee <= summaryLength) {
					endIndex = splitNoTagsStringArray.length - 1;
				} else if (endIndex == -1) {
					endIndex = firstMatchedStringIndex;
				}

				StringBuffer buffer = new StringBuffer();
				for (int k = firstMatchedStringIndex; k <= endIndex; k++) {
					buffer.append(splitWithTagStringArray[k]);
				}
				String finalString = buffer.toString();
				logger.debug("finalString is:\n " + finalString);
				outputString = finalString;
				outputString = outputString.replace((char) 12288, ' ');
				
//				if(outputString.length()  > summaryLength){
//                    int beginIndex = outputString.indexOf("<em");
//                    int end = 0;
//                    //27是高亮标签所占用的字符数
//                    if(beginIndex + summaryLength + 27 >= outputString.length()){
//                        end = outputString.length();
//                    }else{
//                        end = beginIndex + summaryLength + 27;
//                    }
//                    outputString = outputString.substring(beginIndex, end);
//                    outputString = "..." + outputString + "...";
//                }

				return  outputString.trim();

			}

		}
		/**
		 * 找到了超出指定返回数目边界的段25
		 */
		if (handleIndex != -1) {
			int exceedNumber = (int) (lengthSofar - summaryLength);
			String targetString = orignStringArray[handleIndex];
			// System.out.println("targetString is:\n" +
			// targetString);
			logger.debug("targetString is:\n" + targetString);
			int lastLeft = -1;
			for (int i = targetString.length() - 1; i >= 0;) {
				/**
				 * 在这里判断可能有问题，‘>’也可能是大于号，所以还需要改
				 */
				if (targetString.charAt(i) == '>'
						&& targetString.charAt(i - 1) == 'm'
						&& targetString.charAt(i - 2) == 'e'
						|| targetString.charAt(i) == '>'
						&& targetString.charAt(i - 1) == '"'
						&& targetString.charAt(i - 2) == 't') {
					// System.out
					// .println("tags found in target string\n"
					// + targetString);
					logger.debug("tags found in target string\n" + targetString);
					i--;
					while (targetString.charAt(i) != '<') {
						i--;
					}
					lastLeft = i;
				}
				i--;
				exceedNumber -= 1;
				/**
				 * 找到了超出字符的边界
				 */
				if (exceedNumber == 0) {
					/**
					 * 判断这个字符边界是否是被高亮标签包围
					 */
					StringBuffer tempBuffer = new StringBuffer(targetString);
					if (targetString.charAt(lastLeft + 1) == '/') {
						/**
						 * 删除被高亮包围的多余的字符
						 */

						tempBuffer.delete(i + 1, lastLeft);

					} else {
						/**
						 * 目标字符不被高亮包围，直接删除目标字符之后的字符
						 */
						tempBuffer.delete(i + 1, targetString.length());
					}
					orignStringArray[handleIndex] = tempBuffer.toString();
					// System.out.println("chunked string\n"
					// + orignStringArray[handleIndex]);
					logger.debug("chunked string\n"
							+ orignStringArray[handleIndex]);
					break;
				}

			}
			/**
			 * 取出orignStringArray中从开始到handleIndex中的String数组。
			 */
			StringBuffer result = new StringBuffer();
			for (int i = 0; i <= handleIndex; i++) {
				result.append(orignStringArray[i]);
			}
			/**
			 * 设置改变后的内容到json中
			 * 
			 */
			// System.out.println("final length:\n" +
			// result.toString().length());
			String finalString = result.toString();
			int lastIndex = finalString.length() - 1;
			if (finalString.charAt(lastIndex) != '。'
					|| finalString.charAt(lastIndex) != '!'
					|| finalString.charAt(lastIndex) != '?') {
				finalString = finalString + "...";
			}

			logger.debug("final length:\n" + finalString);
			logger.debug("terminate length:\n"
					+ finalString.replaceAll("\\<.*?>", "").length());
			// nContentArray.set(0, finalString);

			outputString = finalString;
			outputString = outputString.replace((char) 12288, ' ');

			return outputString.trim();
		} else {
			/**
			 * newContentString在这里应该不可能为空
			 */
			if (newContentString != null) {
				outputString = newContentString;
				outputString = outputString.replace((char) 12288, ' ');

				return outputString.trim();
			}
		}
		
		return outputString;
	}
	
	// private void getHighlightSummary(int summaryLength, JSONObject hit) {
		//
		// JSONObject highListObject = (JSONObject) hit.get("highlight");
		// JSONObject sourceObject = (JSONObject) hit.get("_source");
		//
		// JSONArray nContentArray = (JSONArray) highListObject.get("nContent");
		// if (nContentArray != null) {
		// String orignString = (String) nContentArray.get(0);
		// // System.out.println("nContentArrayeeee:\n" +
		// // orignString);
		// /**
		// * "(?<=。)"保留分割后的句号
		// */
		// String[] orignStringArray = orignString.split("(?<=，)");
		// for (String temp : orignStringArray) {
		// logger.debug("in array\n" + temp);
		// }
		//
		// /**
		// * 找到第一个包含高亮标签的句子，下标保存在firstMatchingString中
		// */
		// String regEx = "</em>";
		// Pattern pat = null;
		// Matcher mat = null;
		// int firstMatchingString = -1;
		//
		// for (int i = 0; i < orignStringArray.length; i++) {
		// pat = Pattern.compile(regEx);
		// mat = pat.matcher(orignStringArray[i]);
		// if (mat.find()) {
		// firstMatchingString = i;
		// break;
		// }
		//
		// }
		// String noTagString = null;
		// /**
		// * 拼接出从第一个包含高亮标签的句子开始的字符串，firstMatchingString应该不可能为-1
		// */
		// String newContentString = null;
		// if (firstMatchingString != -1) {
		// StringBuffer newContentBuffer = new StringBuffer();
		// for (int i = firstMatchingString; i < orignStringArray.length; i++) {
		// newContentBuffer.append(orignStringArray[i]);
		// }
		// newContentString = newContentBuffer.toString();
		// orignStringArray = newContentString.split("(?<=，)");
		// noTagString = newContentString.replaceAll("\\<.*?>", "");
		//
		// } else {
		// noTagString = orignString.replaceAll("\\<.*?>", "");
		// }
		//
		// /**
		// * "(?<=。)"保留分割后的句号
		// */
		// String[] noTagStringArray = noTagString.split("(?<=，)");
		// long lengthSofar = 0;
		// int handleIndex = -1;
		// for (int i = 0; i < noTagStringArray.length; i++) {
		// lengthSofar += noTagStringArray[i].length();
		// if (lengthSofar > summaryLength) {
		// handleIndex = i;
		// break;
		// }
		// }
		// /**
		// * 高亮出现在第一个句子之中，如果这个句子长度小于等于需要返回的长度，就直接返回这个句子。
		// * 如果大于需要返回的长度，就将这个句子中的逗号，换行和问号换成句号，然后用句号进行分割，
		// * 找到包含高亮标签的那一段直接返回。
		// */
		// if (handleIndex == 0) {
		// System.out.println("高亮字段出现在第一个句子之中");
		// logger.debug("高亮字段出现在第一个句子之中");
		// logger.debug(orignStringArray[0]);
		// if (noTagStringArray[0].length() <= summaryLength) {
		// sourceObject.put("reducedSummary", orignStringArray[0]);
		// return;
		// } else {
		// String changedString = orignStringArray[0].replaceAll("[，\n？]", "。");
		// String[] changedStringArray = changedString.split("(?<=。)");
		// String reg = "</em>";
		// Pattern pattern = null;
		// Matcher matcher = null;
		// int firstMatchedString = -1;
		// for(int i = 0; i < changedStringArray.length; i++){
		// pattern = Pattern.compile(regEx);
		// matcher = pat.matcher(changedStringArray[i]);
		// if (matcher.find()) {
		// firstMatchedString = i;
		// break;
		// }
		// }
		// sourceObject.put("reducedSummary",
		// changedStringArray[firstMatchedString]);
		// return;
		//
		// }
		//
		// }
		// /**
		// * 找到了超出指定返回数目边界的段25
		// */
		// if (handleIndex != -1) {
		// int exceedNumber = (int) (lengthSofar - summaryLength);
		// String targetString = orignStringArray[handleIndex];
		// // System.out.println("targetString is:\n" +
		// // targetString);
		// logger.debug("targetString is:\n" + targetString);
		// int lastLeft = -1;
		// for (int i = targetString.length() - 1; i >= 0;) {
		// /**
		// * 在这里判断可能有问题，‘>’也可能是大于号，所以还需要改
		// */
		// if (targetString.charAt(i) == '>'
		// && targetString.charAt(i - 1) == 'm'
		// && targetString.charAt(i - 2) == 'e'
		// || targetString.charAt(i) == '>'
		// && targetString.charAt(i - 1) == '"'
		// && targetString.charAt(i - 2) == 't') {
		// // System.out
		// // .println("tags found in target string\n"
		// // + targetString);
		// logger.debug("tags found in target string\n"
		// + targetString);
		// i--;
		// while (targetString.charAt(i) != '<') {
		// i--;
		// }
		// lastLeft = i;
		// }
		// i--;
		// exceedNumber -= 1;
		// /**
		// * 找到了超出字符的边界
		// */
		// if (exceedNumber == 0) {
		// /**
		// * 判断这个字符边界是否是被高亮标签包围
		// */
		// StringBuffer tempBuffer = new StringBuffer(targetString);
		// if (targetString.charAt(lastLeft + 1) == '/') {
		// /**
		// * 删除被高亮包围的多余的字符
		// */
		//
		// tempBuffer.delete(i + 1, lastLeft);
		//
		// } else {
		// /**
		// * 目标字符不被高亮包围，直接删除目标字符之后的字符
		// */
		// tempBuffer.delete(i + 1, targetString.length());
		// }
		// orignStringArray[handleIndex] = tempBuffer.toString();
		// // System.out.println("chunked string\n"
		// // + orignStringArray[handleIndex]);
		// logger.debug("chunked string\n"
		// + orignStringArray[handleIndex]);
		// break;
		// }
		//
		// }
		// /**
		// * 取出orignStringArray中从开始到handleIndex中的String数组。
		// */
		// StringBuffer result = new StringBuffer();
		// for (int i = 0; i <= handleIndex; i++) {
		// result.append(orignStringArray[i]);
		// }
		// /**
		// * 设置改变后的内容到json中
		// *
		// */
		// // System.out.println("final length:\n" +
		// // result.toString().length());
		// String finalString = result.toString();
		// int lastIndex = finalString.length() - 1;
		// if (finalString.charAt(lastIndex) != '。'
		// || finalString.charAt(lastIndex) != '!'
		// || finalString.charAt(lastIndex) != '?') {
		// finalString = finalString + "...";
		// }
		//
		// logger.debug("final length:\n" + finalString);
		// logger.debug("terminate length:\n"
		// + finalString.replaceAll("\\<.*?>", "").length());
		// // nContentArray.set(0, finalString);
		// sourceObject.put("reducedSummary", finalString);
		// } else {
		// /**
		// * newContentString在这里应该不可能为空
		// */
		// if (newContentString != null) {
		// sourceObject.put("reducedSummary", newContentString);
		// }
		// }
		//
		// } else {
		// logger.debug("nContent is not in highliget filed");
		// String tempString = (String) sourceObject.get("Summary");
		// if (tempString != null) {
		//
		// int stringLength = tempString.length();
		// if (stringLength == 0) {
		// sourceObject.put("reducedSummary", "");
		// } else {
		//
		// int minLength = stringLength < summaryLength ? stringLength
		// : summaryLength;
		// sourceObject.put("reducedSummary",
		// tempString.substring(0, minLength - 1));
		// }
		// }
		//
		// }
		//
		// }
}
