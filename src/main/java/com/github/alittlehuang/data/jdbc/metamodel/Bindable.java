package com.github.alittlehuang.data.jdbc.metamodel;

import lombok.Getter;

import java.lang.reflect.Member;

@Getter
public class Bindable<T> {

    public enum BindableType {
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

    public Bindable(Member member) {
    }
}
