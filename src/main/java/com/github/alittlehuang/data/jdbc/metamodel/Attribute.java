package com.github.alittlehuang.data.jdbc.metamodel;

import javax.persistence.Column;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @param <X> The represented type that contains the attribute
 * @param <Y> The type of the represented attribute
 */
public class Attribute<X, Y> {

    private final Field field;
    private final Method getter;
    private final Method setter;

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
            return getter.getAnnotation(annotationClass);
        }
        return null;
    }

    public void setValue(X entity, Y value) {
        boolean accessible = field.isAccessible();
        try {
            if (setter != null) {
                setter.invoke(entity, value);
            } else {
                if (!accessible) {
                    field.setAccessible(true);
                }
                field.set(entity, value);
                return;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            field.setAccessible(accessible);
        }
        throw new RuntimeException();
    }

    public Y getValue(X entity) {
        boolean accessible = field.isAccessible();
        try {
            if (getter != null) {
                //noinspection unchecked
                return (Y) getter.invoke(entity);
            } else {
                if (!accessible) {
                    field.setAccessible(true);
                }
                //noinspection unchecked
                return (Y) field.get(entity);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        } finally {
            field.setAccessible(accessible);
        }
        throw new RuntimeException();
    }

    public String getColumnName() {
        Column column = getAnnotation(Column.class);
        if (column != null && column.name().length() != 0) {
            return column.name();
        } else {
            return StringUtil.toUnderline(field.getName());
        }
    }

}
