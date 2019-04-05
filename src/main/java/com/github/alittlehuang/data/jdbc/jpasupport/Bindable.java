package com.github.alittlehuang.data.jdbc.jpasupport;

import lombok.Getter;

@Getter
public class Bindable<T> {

    public static enum BindableType {
        /**
         * Single-valued attribute type
         */
        SINGULAR_ATTRIBUTE,

        /**
         * Multi-valued attribute type
         */
        PLURAL_ATTRIBUTE,

        /**
         * Entity type
         */
        ENTITY_TYPE
    }

    private BindableType bindableType;
    private Class<T> bindableJavaType;

}
