package com.github.alittlehuang.data.jdbc.sql;

import com.github.alittlehuang.data.query.specification.Criteria;

public interface SqlBuilderFactory {

    <T> SqlBuilder<T> create(Criteria<T> criteria);

    interface SqlBuilder<T> {

        PrecompiledSqlForEntity<T> listEntityResult();

        PrecompiledSql listObjectResult();

        PrecompiledSql count();

        PrecompiledSql exists();

    }

}
