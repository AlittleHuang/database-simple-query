package com.github.alittlehuang.data.jdbc.metamodel;

import org.springframework.util.Assert;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EntityInfo<T, ID> {

    private static final Map<Class<?>, EntityInfo<?, ?>> MAP = new ConcurrentHashMap<>();

    private Class<T> javaType;
    private final Attribute<T, ID> idAttribute;
    private final List<Attribute<T, ?>> attributes;
    private final String tableName;

    private EntityInfo(Class<T> javaType) {
        this.attributes = initAttributes(javaType);
        this.idAttribute = initIdAttribute();
        this.tableName = initTableName();
    }

    public static <X, Y> EntityInfo<X, Y> getInstance(Class<X> clazz) {
        Assert.notNull(clazz.getAnnotation(Entity.class), "the calss must be a entity");
        //noinspection unchecked
        return (EntityInfo<X, Y>) MAP.computeIfAbsent(clazz, EntityInfo::new);
    }

    public String getTableName() {
        return tableName;
    }

    public List<? extends Attribute<T, ?>> getAllAttributes() {
        return attributes;
    }

    public Class<T> getJavaType() {
        return javaType;
    }

    public Attribute<T, ID> getIdAttribute() {
        return idAttribute;
    }

    private List<Attribute<T, ?>> initAttributes(Class<T> javaType) {
        List<Attribute<T, ?>> attributes = new ArrayList<>();
        this.javaType = javaType;
        Field[] fields = javaType.getDeclaredFields();
        Map<Field, Method> readerMap = new HashMap<>();
        Map<Field, Method> writeMap = new HashMap<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(javaType);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor descriptor : propertyDescriptors) {
                Field field = getDeclaredField(javaType, descriptor.getName());
                if (field == null) continue;
                readerMap.put(field, descriptor.getReadMethod());
                writeMap.put(field, descriptor.getWriteMethod());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (Field field : fields) {
            if (!readerMap.containsKey(field)) {
                readerMap.put(field, null);
            }
            if (!writeMap.containsKey(field)) {
                writeMap.put(field, null);
            }
        }

        for (Field field : writeMap.keySet()) {
            Attribute<T, ?> attribute = new Attribute<>(field, readerMap.get(field), writeMap.get(field));
            if (attribute.getAnnotation(Transient.class) == null) {
                attributes.add(attribute);
            }
        }
        return Collections.unmodifiableList(attributes);
    }

    private Attribute<T, ID> initIdAttribute() {
        for (Attribute<T, ?> attribute : attributes) {
            if (attribute.getAnnotation(Id.class) != null) {
                //noinspection unchecked
                return (Attribute<T, ID>) attribute;
            }
        }
        return null;
    }

    private String initTableName() {
        Entity entity = javaType.getAnnotation(Entity.class);
        if (entity != null && entity.name().length() > 0) {
            return entity.name();
        }
        Table table = javaType.getAnnotation(Table.class);
        if (table != null && table.name().length() > 0) {
            return table.name();
        }
        return StringUtil.toUnderline(javaType.getSimpleName());
    }

    private static Field getDeclaredField(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> superclass = clazz.getSuperclass();
            if (superclass != null) {
                return getDeclaredField(superclass, name);
            }
        }
        return null;
    }
}
