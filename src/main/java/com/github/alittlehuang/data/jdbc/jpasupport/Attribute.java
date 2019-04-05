package com.github.alittlehuang.data.jdbc.jpasupport;

import lombok.Getter;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * @param <X> The represented type that contains the attribute
 * @param <Y> The type of the represented attribute
 */
@Getter
public class Attribute<X, Y> {

    public enum PersistentAttributeType {
        /**
         * Many-to-one association
         */
        MANY_TO_ONE,

        /**
         * One-to-one association
         */
        ONE_TO_ONE,

        /**
         * Basic attribute
         */
        BASIC,

        /**
         * Embeddable class attribute
         */
        EMBEDDED,

        /**
         * Many-to-many association
         */
        MANY_TO_MANY,

        /**
         * One-to-many association
         */
        ONE_TO_MANY,

        /**
         * Element collection
         */
        ELEMENT_COLLECTION
    }

    public enum PersistenceType {
        /**
         * Entity
         */
        ENTITY,

        /**
         * Embeddable class
         */
        EMBEDDABLE,

        /**
         * Mapped superclass
         */
        MAPPED_SUPERCLASS,

        /**
         * Basic type
         */
        BASIC
    }

    private String name;
    private PersistentAttributeType persistentAttributeType;
    private Class<Y> javaType;
    private Member javaMember;
    private boolean association;
    private boolean collection;
    private PersistenceType persistenceType;

    public Attribute(Method getter) {
        // TODO
    }

}
