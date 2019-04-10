package com.github.alittlehuang.data.jdbc.support;

import com.alibaba.fastjson.util.TypeUtils;
import com.github.alittlehuang.data.jdbc.metamodel.Attribute;
import com.github.alittlehuang.data.jdbc.metamodel.EntityInformation;
import com.github.alittlehuang.data.jdbc.support.sql.PrecompiledSql;
import com.github.alittlehuang.data.jdbc.support.sql.PrecompiledSqlForEntity;
import com.github.alittlehuang.data.jdbc.support.sql.SelectedAttribute;
import com.github.alittlehuang.data.query.support.AbstractQueryStored;
import com.github.alittlehuang.data.util.JointKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class JdbcQueryStored<T> extends AbstractQueryStored<T> {
    private static Logger logger = LoggerFactory.getLogger(JdbcQueryStored.class);

    private DataBasesConfig dataBasesConfig;
    private Class<T> entityType;

    public JdbcQueryStored(DataBasesConfig dataBasesConfig, Class<T> entityType) {
        this.dataBasesConfig = dataBasesConfig;
        this.entityType = entityType;
    }

    @Override
    public List<T> getResultList() {
        PrecompiledSqlForEntity<T> precompiledSql = dataBasesConfig.getSqlBuilder().listResult(getCriteria());
        try ( Connection connection = dataBasesConfig.getDataSource().getConnection() ) {
            ResultSet resultSet = getResultSet(connection, precompiledSql);
            return toList(resultSet, precompiledSql.getSelections());
        } catch ( SQLException e ) {
            throw new RuntimeException(e);
        }
    }

    private List<T> toList(ResultSet resultSet, List<SelectedAttribute> selectedAttributes) throws SQLException {
        List<T> results = new ArrayList<>();
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
            HashMap<JointKey, Object> instanceMap = new HashMap<>();
            instanceMap.put(asKey(null), entity);
            for ( SelectedAttribute selectedAttribute : selectedAttributes ) {
                Object val = resultSet.getObject(++index);
                Attribute<Object, Object> attribute = selectedAttribute.getAttribute();
                Class<Object> fieldType = attribute.getFieldType();
                if ( val != null ) {
                    Class<?> valType = val.getClass();
                    if ( !fieldType.isAssignableFrom(valType) ) {
                        Function<Object, Object> function = dataBasesConfig.getTypeConverterSet().get(valType, fieldType);
                        if ( function != null ) {
                            val = function.apply(val);
                        } else {
                            Class<Object> entityType = attribute.getEntityType();
                            EntityInformation<Object, Object> information = EntityInformation.getInstance(entityType);
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
                    Object entityAttr = getInstance(instanceMap, selectedAttribute.getParent());
                    attribute.setValue(entityAttr, val);
                }
            }
            fistRow = false;
        }
        return results;
    }

    private Object getInstance(Map<JointKey, Object> map, SelectedAttribute selected) {

        JointKey key = asKey(selected);
        if ( map.containsKey(key) ) {
            return map.get(key);
        }

        try {
            Object parentInstance = getInstance(map, selected.getParent());
            Attribute<Object, Object> attribute = selected.getAttribute();
            Object val = attribute.getFieldType().newInstance();
            attribute.setValue(parentInstance, val);
            map.put(key, val);
            return val;
        } catch ( InstantiationException | IllegalAccessException e ) {
            throw new RuntimeException(e);
        }

    }

    private JointKey asKey(SelectedAttribute selected) {
        return selected == null ? null : new JointKey(selected.getAttribute(), selected.getParentAttribute());
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
        PrecompiledSql count = dataBasesConfig.getSqlBuilder().count(getCriteria());

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

    private ResultSet getResultSet(Connection connection, PrecompiledSql precompiledSql) {
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