package com.github.alittlehuang.data.jdbc.metamodel;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @param <X> The represented type that contains the attribute
 * @param <Y> The type of the represented attribute
 */
public class Attribute<X, Y> {

    final Field field;
    final Method getter;
    final Method setter;

    public Attribute(Field field, Method getter, Method setter) {
        this.field = field;
        this.getter = getter;
        this.setter = setter;
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (field != null) {
            T annotation = field.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        if (getter != null) {
            T annotation = getter.getAnnotation(annotationClass);
            if (annotation != null) {
                return annotation;
            }
        }
        return null;
    }

}
