package com.github.alittlehuang.data.jdbc;

import com.github.alittlehuang.data.jdbc.sql.*;
import com.github.alittlehuang.data.metamodel.Attribute;
import com.github.alittlehuang.data.metamodel.EntityInformation;
import com.github.alittlehuang.data.query.page.Page;
import com.github.alittlehuang.data.query.specification.Selection;
import com.github.alittlehuang.data.query.support.AbstractQueryStored;
import com.github.alittlehuang.data.util.JointKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.function.Function;

public class JdbcQueryStored<T> extends AbstractQueryStored<T> {
    private static Logger logger = LoggerFactory.getLogger(JdbcQueryStored.class);

    private JdbcQueryStoredConfig config;
    private Class<T> entityType;

    public JdbcQueryStored(JdbcQueryStoredConfig config, Class<T> entityType) {
        this.config = config;
        this.entityType = entityType;
    }

    @Override
    public List<T> getResultList() {
        PrecompiledSqlForEntity<T> precompiledSql = getSqlBuilder().listResult();
        try ( Connection connection = config.getDataSource().getConnection() ) {
            ResultSet resultSet = getResultSet(connection, precompiledSql);
            return toList(resultSet, precompiledSql.getSelections());
        } catch ( SQLException e ) {
            throw new RuntimeException(e);
        }
    }

    private SqlBuilderFactory.SqlBuilder<T> getSqlBuilder() {
        return config.getSqlBuilderFactory().create(getCriteria());
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
                    if ( !fieldType.isInstance(val) ) {
                        Class<?> valType = val.getClass();
                        Function<Object, Object> function = config.getTypeConverter(valType, fieldType);
                        if ( function != null ) {
                            val = function.apply(val);
                        } else {
                            // noinspection UnnecessaryLocalVariable
                            Class targetType = fieldType;
                            if ( targetType == Byte.class )
                                val = resultSet.getByte(index);
                            else if ( targetType == Short.class )
                                val = resultSet.getShort(index);
                            else if ( targetType == Integer.class )
                                val = resultSet.getInt(index);
                            else if ( targetType == Float.class )
                                val = resultSet.getFloat(index);
                            else if ( targetType == Long.class )
                                val = resultSet.getLong(index);
                            else if ( targetType == Double.class )
                                val = resultSet.getDouble(index);
                            else if ( targetType == BigDecimal.class )
                                val = resultSet.getBigDecimal(index);
                            else if ( targetType == Boolean.class )
                                val = resultSet.getBoolean(index);
                            else if ( targetType == Date.class )
                                val = resultSet.getDate(index);
                            else if ( targetType == String.class )
                                val = resultSet.getString(index);
                            else if ( targetType == Time.class )
                                val = resultSet.getTime(index);
                            else {
                                if ( fistRow && logger.isWarnEnabled() ) {
                                    Class<Object> entityType = attribute.getEntityType();
                                    EntityInformation<Object, Object> information = EntityInformation.getInstance(entityType);
                                    Field field = attribute.getField();

                                    logger.warn("the type " + information.getTableName() + "." + attribute.getColumnName() +
                                            " in the database does not match " + field.getDeclaringClass().getTypeName() + "."
                                            + field.getName());
                                }

                                // other cast ???
                            }
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

        List<? extends Selection<T>> selections = criteria.getSelections();
        if ( selections == null || selections.isEmpty()) {
            //noinspection unchecked
            return (List<X>) getResultList();
        }

        PrecompiledSql precompiledSql = getSqlBuilder().listObjects();
        int columnsCount = selections.size();
        List<Object> result = new ArrayList<>();
        try {
            ResultSet resultSet = getResultSet(config.getDataSource().getConnection(), precompiledSql);
            while ( resultSet.next() ) {
                if ( columnsCount == 1 ) {
                    result.add(resultSet.getObject(1));
                } else {
                    Object[] row = new Object[columnsCount];
                    for ( int i = 0; i < columnsCount; i++ ) {
                        row[i] = resultSet.getObject(i + 1);
                    }
                    result.add(row);
                }
            }
        } catch ( SQLException e ) {
            throw new RuntimeException(e);
        }
        //noinspection unchecked
        return (List<X>) result;
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
        PrecompiledSql count = getSqlBuilder().count();

        try ( Connection connection = config.getDataSource().getConnection() ) {
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
        PrecompiledSql precompiledSql = getSqlBuilder().exists();
        try ( Connection connection = config.getDataSource().getConnection() ) {
            ResultSet resultSet = getResultSet(connection, precompiledSql);
            return resultSet.next();
        } catch ( SQLException e ) {
            throw new RuntimeException(e);
        }
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

            logSql(sql, args);

            return preparedStatement.executeQuery();
        } catch ( SQLException e ) {
            throw new RuntimeException(e);
        }
    }

    private void logSql(String sql, List<Object> args) {
        if ( logger.isDebugEnabled() ) {
            StringBuilder info = new StringBuilder();
            info.append("prepared sql:\n\n ").append(sql);
            if ( !args.isEmpty() ) {
                info.append("\n args: ").append(args.toString());
            }
            info.append("\n");
            logger.debug(info.toString());
        }
    }
}