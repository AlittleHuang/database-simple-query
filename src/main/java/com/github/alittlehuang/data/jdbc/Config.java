package com.github.alittlehuang.data.jdbc;

import com.github.alittlehuang.data.jdbc.sql.SqlBuilder;
import com.github.alittlehuang.data.util.JointKey;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Config {

    private SqlBuilder sqlBuilder;
    private DataSource dataSource;
    private Map<JointKey, Function<Object, Object>> typeConverterSet = new ConcurrentHashMap<>();


    public Config(SqlBuilder sqlBuilder, DataSource dataSource) {
        this.sqlBuilder = sqlBuilder;
        this.dataSource = dataSource;
    }

    Function<Object, Object> getTypeConverter(Class<?> srcType, Class<?> targetType) {
        return typeConverterSet.get(new JointKey(srcType, targetType));
    }

    public <T, U> void register(Class<T> srcType, Class<U> targetType, Function<T, U> converter) {
        //noinspection unchecked
        typeConverterSet.put(new JointKey(srcType, targetType), (Function<Object, Object>) converter);
    }

    public SqlBuilder getSqlBuilder() {
        return sqlBuilder;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

}
