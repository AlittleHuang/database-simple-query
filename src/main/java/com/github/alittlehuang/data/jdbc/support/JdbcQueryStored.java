package com.github.alittlehuang.data.jdbc.support;

import com.alibaba.fastjson.util.TypeUtils;
import com.github.alittlehuang.data.jdbc.metamodel.Attribute;
import com.github.alittlehuang.data.jdbc.metamodel.EntityInformation;
import com.github.alittlehuang.data.jdbc.sql.SqlBuilder;
import com.github.alittlehuang.data.query.support.AbstractQueryStored;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class JdbcQueryStored<T> extends AbstractQueryStored<T> {
    Logger logger = LoggerFactory.getLogger(getClass());

    private DataBasesConfig dataBasesConfig;
    private Class<T> entityType;

    public JdbcQueryStored(DataBasesConfig dataBasesConfig, Class<T> entityType) {
        this.dataBasesConfig = dataBasesConfig;
        this.entityType = entityType;
    }

    @Override
    public List<T> getResultList() {
        SqlBuilder.PrecompiledSql count = dataBasesConfig.getSqlBuilder().listResult(getCriteria());
        try ( Connection connection = dataBasesConfig.getDataSource().getConnection() ) {
            ResultSet resultSet = getResultSet(connection, count);
            return toList(resultSet);
        } catch ( SQLException e ) {
            throw new RuntimeException(e);
        }
    }

    private List<T> toList(ResultSet resultSet) throws SQLException {
        List<T> results = new ArrayList<>();
        List<? extends Attribute<T, Object>> allAttributes = EntityInformation.getInstance(entityType).getAllAttributes();
        boolean fistRow = true;
        while ( resultSet.next() ) {
            T entity;
            try {
                entity = entityType.newInstance();
            } catch ( InstantiationException | IllegalAccessException e ) {
                throw new RuntimeException(e);
            }
            results.add(entity);
            int index = 0;
            for ( Attribute<T, Object> attribute : allAttributes ) {
                Object val = resultSet.getObject(++index);
                Class<Object> fieldType = attribute.getFieldType();
                if ( val != null ) {
                    Class<?> valType = val.getClass();
                    if ( !fieldType.isAssignableFrom(valType) ) {
                        Function<Object, Object> function = dataBasesConfig.getTypeConverterSet().get(valType, fieldType);
                        if ( function != null ) {
                            val = function.apply(val);
                        } else {
                            Class<T> entityType = attribute.getEntityType();
                            EntityInformation<T, Object> information = EntityInformation.getInstance(entityType);
                            Field field = attribute.getField();
                            if ( fistRow && logger.isWarnEnabled() ) {
                                logger.warn("the type " + information.getTableName() + "." + attribute.getColumnName() +
                                        " in the database does not match " + field.getDeclaringClass().getTypeName() + "."
                                        + field.getName());
                            }
                            // logger.warn("missing converter from" + valType + " to " + fieldType);
                            val = TypeUtils.cast(val, fieldType, null);
                        }
                    }
                }
                attribute.setValue(entity, val);
            }
            fistRow = false;
        }
        return results;
    }

    @Override
    public <X> List<X> getObjectList() {
        return null;
    }

    @Override
    public Page<T> getPage(long page, long size) {
        return null;
    }

    @Override
    public Page<T> getPage() {
        return null;
    }

    @Override
    public long count() {
        SqlBuilder.PrecompiledSql count = dataBasesConfig.getSqlBuilder().count(getCriteria());

        try ( Connection connection = dataBasesConfig.getDataSource().getConnection() ) {
            ResultSet resultSet = getResultSet(connection, count);
            if ( resultSet.next() ) {
                return resultSet.getLong(1);
            } else {
                return 0L;
            }
        } catch ( SQLException e ) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public Class<T> getJavaType() {
        return entityType;
    }

    private ResultSet getResultSet(Connection connection, SqlBuilder.PrecompiledSql precompiledSql) {
        String sql = precompiledSql.getSql();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            int i = 0;
            List<Object> args = precompiledSql.getArgs();
            for ( Object arg : args ) {
                preparedStatement.setObject(++i, arg);
            }

            if ( logger.isDebugEnabled() ) {
                logger.debug("prepared sql: " + sql);
                logger.debug(args.toString());
            }

            return preparedStatement.executeQuery();
        } catch ( SQLException e ) {
            throw new RuntimeException(e);
        }
    }
}