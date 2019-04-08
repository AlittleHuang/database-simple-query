package com.github.alittlehuang.data.jdbc.support;

import com.github.alittlehuang.data.jdbc.metamodel.Attribute;
import com.github.alittlehuang.data.jdbc.metamodel.EntityInformation;
import com.github.alittlehuang.data.jdbc.sql.SqlBuilder;
import com.github.alittlehuang.data.query.support.AbstractQueryStored;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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
                attribute.setValue(entity, resultSet.getObject(++index));
            }
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
        logger.debug(sql);
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            int i = 0;
            for ( Object arg : precompiledSql.getArgs() ) {
                preparedStatement.setObject(++i, arg);
            }
            logger.debug(preparedStatement.toString());
            return preparedStatement.executeQuery();
        } catch ( SQLException e ) {
            throw new RuntimeException(e);
        }
    }
}
