package vn.com.vndirect.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigUtilsTest {
    @Test
    void mergeArray() {
        assertNull(ConfigUtils.mergeArray(null, null));
        assertNull(ConfigUtils.mergeArray(new Object[0], null));
        assertArrayEquals(ConfigUtils.mergeArray(null, new Object[0]), new Object[0]);
        assertArrayEquals(ConfigUtils.mergeArray(new String[]{"abc"}, new String[]{"1", "2"}), new String[]{"abc", "1", "2"});
    }

}