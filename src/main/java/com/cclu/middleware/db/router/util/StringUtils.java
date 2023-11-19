package com.cclu.middleware.db.router.util;

/**
 * @author ChangCheng Lu
 * @date 2023/11/16 12:27
 * @description
 * @copyright ChangChengLu
 */
public class StringUtils {

    /**
     * 将中划线字符串转驼峰式字符串
     * @param input 中划线字符串
     * @return 驼峰式字符串
     */
    public static String middleScoreToCamelCase(String input) {
        StringBuilder result = new StringBuilder();
        boolean nextUpperCase = false;
        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);

            if (currentChar == '-') {
                nextUpperCase = true;
            } else {
                if (nextUpperCase) {
                    result.append(Character.toUpperCase(currentChar));
                    nextUpperCase = false;
                } else {
                    result.append(currentChar);
                }
            }
        }
        return result.toString();
    }

}
