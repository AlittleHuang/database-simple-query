package com.github.alittlehuang.data.jdbc.support.sql;

import com.github.alittlehuang.data.query.specification.Criteria;

public interface SqlBuilder {

    <T> PrecompiledSqlForEntity<T> listResult(Criteria<T> query);

    PrecompiledSql ListObjects(Criteria<?> query);

    PrecompiledSql count(Criteria<?> query);

    PrecompiledSql exists(Criteria<?> query);

}
