package com.github.alittlehuang.data.jdbc.support;

import com.github.alittlehuang.data.jdbc.sql.SqlBuilder;
import com.github.alittlehuang.data.util.JointKey;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Getter
@Setter
public class DataBasesConfig {

    private SqlBuilder sqlBuilder;
    private DataSource dataSource;
    private TypeConverterSet typeConverterSet = new TypeConverterSet();

    public DataBasesConfig(SqlBuilder sqlBuilder, DataSource dataSource) {
        this.sqlBuilder = sqlBuilder;
        this.dataSource = dataSource;
    }

    @Data
    private class TypeConverter<T, U> {
        Class<T> srcType;
        Class<U> resType;

        public TypeConverter(Class<T> srcType, Class<U> resType) {
            this.srcType = srcType;
            this.resType = resType;
        }
    }

    class TypeConverterSet {

        private Map<JointKey, Function<Object, Object>> map = new ConcurrentHashMap<>();

        public <T, U> void put(Class<T> srcType, Class<U> targetType, Function<T, U> converter) {
            //noinspection unchecked
            map.put(new JointKey(srcType, targetType), (Function<Object, Object>) converter);
        }

        public Function<Object, Object> get(Class<?> srcType, Class<?> targetType) {
            return map.get(new JointKey(srcType, targetType));
        }

    }


}
