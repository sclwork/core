package com.scliang.core.base;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;
import android.widget.EditText;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/9/28.
 *
 * [C/C++函数库] 引用入口
 *
 */
public final class NUtils {
    private NUtils() { }

    // 引入[C/C++函数库]
    static {
        System.loadLibrary("jcore");
    }

    /**
     * 获得 [C/C++函数库] 的当前版本
     * @return 当前版本名称
     */
    public static native String getNativeVersion();

    /**
     * 对给定的字符串做MD5
     * @param input 待签名字符串
     * @return MD5
     */
    public static String md5(String input) {
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        try {
            byte[] btInput = input.getBytes("UTF-8");
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            // 使用指定的字节更新摘要
            mdInst.update(btInput);
            // 获得密文
            byte[] md = mdInst.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return "";
        }
    }

    public interface OnEditTextInputFullListener {
        void onEditTextInputFulled(EditText editText);
    }

    /**
     * 限定最大输入长度
     * @param editText EditText
     * @param maxLength MaxLength
     */
    public static void setTrimInflate(EditText editText, final int maxLength) {
        setTrimInflate(editText, maxLength, true, true, true, null);
    }

    /**
     * 限定最大输入长度
     * @param editText EditText
     * @param maxLength MaxLength
     */
    public static void setTrimInflate(EditText editText, final int maxLength,
                                      OnEditTextInputFullListener listener) {
        setTrimInflate(editText, maxLength, true, true, true, listener);
    }

    /**
     * 限定最大输入长度
     * @param editText EditText
     * @param maxLength MaxLength
     * @param space 是否可以输入空格 : false:禁止输入空格
     * @param enter 是否可以输入回车 : false:禁止输入回车
     * @param emoji 是否可以输入Emoji表情 : false:禁止输入Emoji表情
     */
    public static void setTrimInflate(EditText editText, final int maxLength,
                                      final boolean space, final boolean enter, final boolean emoji) {
        setTrimInflate(editText, maxLength, space, enter, emoji, null);
    }

    /**
     * 限定最大输入长度
     * @param editText EditText
     * @param maxLength MaxLength
     * @param space 是否可以输入空格 : false:禁止输入空格
     * @param enter 是否可以输入回车 : false:禁止输入回车
     * @param emoji 是否可以输入Emoji表情 : false:禁止输入Emoji表情
     */
    public static void setTrimInflate(final EditText editText, final int maxLength,
                                      final boolean space, final boolean enter, final boolean emoji,
                                      final OnEditTextInputFullListener listener) {
        if (null == editText) {
            return;
        }

        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                // 判断是否可以输入空格
                if (!space && source.equals(" ")) {
                    return "";
                }

                // 判断是否可以输入回车
                if (!enter && (source.equals("\n") || source.equals("\r"))) {
                    return "";
                }

                // 判断是否可以输入Emoji表情
                String srcStr = source.toString();
                if (!emoji && (/*containsEmoji(srcStr) || */containsEmoji2(srcStr) || containsEmoji3(srcStr))) {
                    return "";
                }

                // 判断长度
                if (maxLength > 0) {
                    String src = space ? source.toString() : source.toString().replaceAll(" ", "");
                    src = enter ? src : src.replaceAll("\n", "");
                    src = enter ? src : src.replaceAll("\r", "");
                    if (null != dest && dest.length() >= maxLength) {
                        if (TextUtils.isEmpty(src)) {
                            return null;
                        } else {
                            if (listener != null) {
                                listener.onEditTextInputFulled(editText);
                            }
                            return dest.subSequence(dstart, dend);
                        }
                    } else if (dest != null && dstart >= 0 && dstart < maxLength) {
                        int has = dest.length() - (dend - dstart);
                        int sub = maxLength - has;
                        if (sub > 0) {
                            if (sub >= src.length()) {
                                return null;
                            } else {
                                if (listener != null) {
                                    listener.onEditTextInputFulled(editText);
                                }
                                return src.substring(0, sub);
                            }
                        } else {
                            if (listener != null) {
                                listener.onEditTextInputFulled(editText);
                            }
                            return "";
                        }
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            }
        };
        editText.setFilters(new InputFilter[]{filter});
    }

//    private static boolean containsEmoji(String source) {
//        return sFilterSet.contains(source);
//    }

//    private static void addUnicodeRangeToSet(Set<String> set, int start, int end) {
//        if (set == null) {
//            return;
//        }
//
//        if (start > end) {
//            return;
//        }
//
//        for (int i = start; i <= end; i++) {
//            sFilterSet.add(new String(new int[] {
//                    i
//            }, 0, 1));
//        }
//    }

    private static boolean containsEmoji2(String source) {
        if (TextUtils.isEmpty(source)) {
            return false;
        }

        boolean res = false;

        char hs = source.charAt(0);
        if (0xd800 <= hs && hs <= 0xdbff) {
            if (source.length() > 1) {
                char ls = source.charAt(1);
                int uc = ((hs - 0xd800) * 0x400) + (ls - 0xdc00) + 0x10000;
                if (0x1d000 <= uc && uc <= 0x1f77f) {
                    res = true;
                }
            }
        } else if (source.length() > 1) {
            char ls = source.charAt(1);
            if (ls == 0x20e3) {
                res = true;
            }
        } else {
            // non surrogate
            if (0x2100 <= hs && hs <= 0x215f) {
                res = true;
            } else if (0x216c <= hs && hs <= 0x27ff) {
                res = true;
            } else if (0x2B05 <= hs && hs <= 0x2b07) {
                res = true;
            } else if (0x2934 <= hs && hs <= 0x2935) {
                res = true;
            } else if (0x3297 <= hs && hs <= 0x3299) {
                res = true;
            } else if (hs == 0xa9   || hs == 0xae ||
                       hs == 0x303d || hs == 0x3030 ||
                       hs == 0x2b55 || hs == 0x2b1c ||
                       hs == 0x2b1b || hs == 0x2b50) {
                res = true;
            }
        }

        return res;
    }

    private static boolean containsEmoji3(String source) {
        if (TextUtils.isEmpty(source)) {
            return false;
        }

        String sb = "[^" +
            "\\u03B1-\\u03C9" +
            "\\u0020-\\u007E" +
            "\\u00A0-\\u00BE" +
            "\\u2E80-\\uA4CF" +
            "\\uF900-\\uFAFF" +
            "\\uFE30-\\uFE4F" +
            "\\uFF00-\\uFFEF" +
            "\\u2000-\\u201f" +
            "\\u2160-\\u216b" +
            "\r\n]";
        Pattern p = Pattern.compile(sb, Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(source);
        return m.find();
    }

//    private static Set<String> sFilterSet = new HashSet<>();
//    static {
//        // See http://apps.timwhitlock.info/emoji/tables/unicode
//
//        // 1F601 - 1F64F
//        addUnicodeRangeToSet(sFilterSet, 0x1F601, 0X1F64F);
//
//        // 2702 - 27B0
//        addUnicodeRangeToSet(sFilterSet, 0x2702, 0X27B0);
//
//        // 1F680 - 1F6C0
//        addUnicodeRangeToSet(sFilterSet, 0X1F680, 0X1F6C0);
//
//        // 24C2 - 1F251
//        addUnicodeRangeToSet(sFilterSet, 0X24C2, 0X1F251);
//
//        // 1F600 - 1F636
//        addUnicodeRangeToSet(sFilterSet, 0X1F600, 0X1F636);
//
//        // 1F681 - 1F6C5
//        addUnicodeRangeToSet(sFilterSet, 0X1F681, 0X1F6C5);
//
//        // 1F30D - 1F567
//        addUnicodeRangeToSet(sFilterSet, 0X1F30D, 0X1F567);
//
//        // not included 5. Uncategorized
//    }

    /**
     * 匹配格式： 11位手机号码 3-4位区号，7-8位直播号码，1－4位分机号
     * 如：12345678901、1234-12345678-1234
     */
    public static boolean validatePhoneNumber(String phoneString) {
//        String format = "(^1[3|4|5|7|8][0-9]{9}$)";
        String format = "(^1[0-9]{10}$)";
        return phoneString.matches(format);
    }
}
