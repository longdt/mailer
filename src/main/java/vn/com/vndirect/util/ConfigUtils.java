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
}
