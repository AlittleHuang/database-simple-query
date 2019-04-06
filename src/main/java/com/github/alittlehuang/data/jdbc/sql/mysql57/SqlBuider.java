package com.github.alittlehuang.data.jdbc.sql.mysql57;

import com.github.alittlehuang.data.jdbc.metamodel.Attribute;
import com.github.alittlehuang.data.jdbc.metamodel.EntityInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SqlBuider {

    private static Map<Class, String> SELECT_FROM = new ConcurrentHashMap<>();

    public static String selectFrom(Class<?> clazz) {

        return SELECT_FROM.computeIfAbsent(clazz, javaType -> {
            StringBuilder builder = new StringBuilder("SELECT ");
            EntityInfo<?, Object> entityInfo = EntityInfo.getInstance(clazz);

            boolean first = true;
            for (Attribute<?, ?> attribute : entityInfo.getAllAttributes()) {
                if (first) {
                    first = false;
                } else {
                    builder.append(", ");
                }
                builder.append("`").append(attribute.getColumnName()).append("`");
            }
            builder.append(" FROM ")
                    .append("`")
                    .append(entityInfo.getTableName())
                    .append("`")
                    .append("\n");

            return builder.toString();
        });


    }

}
