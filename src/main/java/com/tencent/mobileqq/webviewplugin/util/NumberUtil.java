package com.tencent.mobileqq.webviewplugin.util;

/**
 * @author weijianchen
 */
public class NumberUtil {

    private static final String TAG = "NumberUtil";

    public static boolean isValidLong(String str) {
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * @param longString 长整数字符串
     * @param defaultNum 错误时返回的默认值
     * @return parsed long or 0 when string is ill-format
     */
    public static long longValueOf(String longString, long defaultNum) {
        try {
            return Long.valueOf(longString);
        } catch (NumberFormatException e) {
            LogUtil.e(TAG, "long string is ill-format, return 0");
            return defaultNum;
        }
    }

    /**
     * @param integer in string
     * @return parsed integer or 0 when string is ill-format
     */
    public static int integerValueOf(String integer, int defaultNum) {
        try {
            return Integer.valueOf(integer);
        } catch (NumberFormatException e) {
            LogUtil.e(TAG, "integer string is ill-format, return 0");
            return defaultNum;
        }
    }

    /**
     * @param integer in string
     * @return parsed integer or 0 when string is ill-format
     */
    public static int integerValueOf(String integer) {
        try {
            return Integer.valueOf(integer);
        } catch (NumberFormatException e) {
            LogUtil.e(TAG, "integer string is ill-format, return 0");
            return 0;
        }
    }

    public static String stringValueOf(int value) {
        try {
            return String.valueOf(value);
        } catch (NumberFormatException e) {
            LogUtil.e(TAG, "int value is ill-format, return ''");
            return "";
        }
    }

    public static String stringValueOf(long value) {
        try {
            return String.valueOf(value);
        } catch (NumberFormatException e) {
            LogUtil.e(TAG, "long value is ill-format, return ''");
            return "";
        }
    }

}
