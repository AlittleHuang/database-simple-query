package com.github.alittlehuang.data.jdbc;

import com.github.alittlehuang.data.jdbc.sql.SqlBuilderFactory;
import com.github.alittlehuang.data.jdbc.sql.mysql57.Mysql57SqlBuilderFactory;
import com.github.alittlehuang.data.util.JointKey;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class JdbcQueryStoredConfig {

    private SqlBuilderFactory sqlBuilderFactory;
    private DataSource dataSource;
    private Map<JointKey, Function<Object, Object>> typeConverterSet = new ConcurrentHashMap<>();

    public JdbcQueryStoredConfig(SqlBuilderFactory sqlBuilderFactory, DataSource dataSource) {
        this.sqlBuilderFactory = sqlBuilderFactory;
        this.dataSource = dataSource;
    }

    public JdbcQueryStoredConfig(DataSource dataSource) {
        this(new Mysql57SqlBuilderFactory(), dataSource);
    }

    Function<Object, Object> getTypeConverter(Class<?> srcType, Class<?> targetType) {
        return typeConverterSet.get(new JointKey(srcType, targetType));
    }

    public <T, U> void register(Class<T> srcType, Class<U> targetType, Function<T, U> converter) {
        //noinspection unchecked
        typeConverterSet.put(new JointKey(srcType, targetType), (Function<Object, Object>) converter);
    }

    public SqlBuilderFactory getSqlBuilderFactory() {
        return sqlBuilderFactory;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

}
