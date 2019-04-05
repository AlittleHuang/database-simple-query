package com.github.alittlehuang.data.jdbc.metamodel;

import javax.persistence.Id;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityInfo<T, ID> {

    private Class<T> javaType;
    private Attribute<T, ID> idAttribute;
    private List<Attribute<T, ?>> attributes = new ArrayList<>();

    public EntityInfo(Class<T> javaType) {
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
            attributes.add(attribute);
        }
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

    public List<? extends Attribute<T, ?>> getAttributes() {
        return attributes;
    }

    public Class<T> getJavaType() {
        return javaType;
    }

    public Attribute<T, ID> getIdAttribute() {
        if (idAttribute == null) {
            for (Attribute<T, ?> attribute : attributes) {
                if (attribute.getAnnotation(Id.class) != null) {
                    //noinspection unchecked
                    idAttribute = (Attribute<T, ID>) attribute;
                    break;
                }
            }
        }
        return idAttribute;
    }
}
