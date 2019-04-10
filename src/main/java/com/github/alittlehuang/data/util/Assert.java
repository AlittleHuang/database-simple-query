package com.github.alittlehuang.data.util;

public class Assert {
    private Assert() {
    }

    public static void state(boolean b, String info) {

    }

    public static void notNull(Object o, String s) {
        state(o != null, s);
    }
}
