package vn.com.vndirect.util;

import java.util.Properties;

/**
 * Created by naruto on 6/9/17.
 */
public class ConfigUtils {
    public static int getInt(Properties conf, String key, int defaultVal) {
        try {
            return Integer.parseInt(conf.getProperty(key));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    public static <T> T[] mergeArray(T[] a, T[] b) {
        if (a == null || a.length == 0) {
            return b;
        } else if (b == null || b.length ==0) {
            return a;
        } else {
            Object[] result = new Object[a.length + b.length];
            System.arraycopy(a, 0, result, 0, a.length);
            System.arraycopy(b, 0, result, a.length, b.length);
            return (T[]) result;
        }
    }
}
