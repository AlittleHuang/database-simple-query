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

        private Map<JointKey, Function<?, ?>> map = new ConcurrentHashMap<>();

        public <T, U> void put(Class<T> srcType, Class<U> targetType, Function<T, U> converter) {
            map.put(new JointKey(srcType, targetType), converter);
        }

        public <T, U> Function<T, U> get(Class<T> srcType, Class<U> targetType) {
            //noinspection unchecked
            return (Function<T, U>) map.get(new JointKey(srcType, targetType));
        }

    }


}
