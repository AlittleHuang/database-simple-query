package com.github.alittlehuang.data.jdbc;

import com.github.alittlehuang.data.jdbc.sql.EntityInformationFactory;
import com.github.alittlehuang.data.jdbc.sql.SqlBuilderFactory;
import com.github.alittlehuang.data.jdbc.sql.mysql57.Mysql57SqlBuilderFactory;
import com.github.alittlehuang.data.metamodel.EntityInformation;
import com.github.alittlehuang.data.util.JointKey;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class JdbcQueryStoredConfig {

    private DataSource dataSource;
    private SqlBuilderFactory sqlBuilderFactory;
    private Map<JointKey, Function<Object, Object>> typeConverterSet = new ConcurrentHashMap<>();
    private EntityInformationFactory entityInformationFactory;

    public JdbcQueryStoredConfig() {
    }

    public JdbcQueryStoredConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    Function<Object, Object> getTypeConverter(Class<?> srcType, Class<?> targetType) {
        return typeConverterSet.get(new JointKey(srcType, targetType));
    }

    public <T, U> void registerTypeConverter(Class<T> srcType, Class<U> targetType, Function<T, U> converter) {
        //noinspection unchecked
        typeConverterSet.put(new JointKey(srcType, targetType), (Function<Object, Object>) converter);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public SqlBuilderFactory getSqlBuilderFactory() {
        if (sqlBuilderFactory == null) {
            sqlBuilderFactory = new Mysql57SqlBuilderFactory(this);
        }
        return sqlBuilderFactory;
    }

    public void setSqlBuilderFactory(SqlBuilderFactory sqlBuilderFactory) {
        this.sqlBuilderFactory = sqlBuilderFactory;
    }

    public EntityInformationFactory getEntityInformationFactory() {
        if (entityInformationFactory == null) {
            entityInformationFactory = EntityInformation::getInstance;
        }
        return entityInformationFactory;
    }

    public void setEntityInformationFactory(EntityInformationFactory entityInformationFactory) {
        this.entityInformationFactory = entityInformationFactory;
    }
}
