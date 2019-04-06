package com.github.alittlehuang.data.jdbc.metamodel;

class StringUtil {

    /**
     * 驼峰转下划线
     */
     static String toUnderline(String str) {
        if (str == null) return null;
        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
