package com.github.alittlehuang.data.jdbc.metamodel;

import javax.persistence.*;
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
    private final Class<X> entityType;
    private final ManyToOne manyToOne;
    private final OneToMany oneToMany;
    private final JoinColumn joinColumn;
    private final Version version;
    private final Column column;
    private final ManyToMany manyToMany;
    private final OneToOne oneToOne;

    public Attribute(Field field, Method getter, Method setter, Class<X> entityType) {
        this.field = field;
        this.getter = getter;
        this.setter = setter;
        this.entityType = entityType;
        this.manyToOne = getAnnotation(ManyToOne.class);
        this.oneToMany = getAnnotation(OneToMany.class);
        this.joinColumn = getAnnotation(JoinColumn.class);
        this.version = getAnnotation(Version.class);
        this.column = getAnnotation(Column.class);
        this.manyToMany = getAnnotation(ManyToMany.class);
        this.oneToOne = getAnnotation(OneToOne.class);
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
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        } finally {
            field.setAccessible(accessible);
        }
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
            return field.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        }
    }

    public String getFieldName() {
        return field.getName();
    }

    public Class<Y> getFieldType() {
        //noinspection unchecked
        return (Class<Y>) field.getType();
    }

    public Class<X> getEntityType() {
        return entityType;
    }

    public Field getField() {
        return field;
    }

    public ManyToOne getManyToOne() {
        return manyToOne;
    }

    public OneToMany getOneToMany() {
        return oneToMany;
    }

    public Column getColumn() {
        return column;
    }

    public Version getVersion() {
        return version;
    }

    public JoinColumn getJoinColumn() {
        return joinColumn;
    }

    public ManyToMany getManyToMany() {
        return manyToMany;
    }

    public OneToOne getOneToOne() {
        return oneToOne;
    }

    public boolean isEntityType() {
        return getFieldType().getAnnotation(Entity.class) != null;
    }
}
