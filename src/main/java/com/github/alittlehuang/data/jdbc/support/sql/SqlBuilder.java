package com.github.alittlehuang.data.jdbc.support.sql;

import com.github.alittlehuang.data.query.specification.Criteria;

public interface SqlBuilder {

    PrecompiledSqlForEntity listResult(Criteria<?> query);

    PrecompiledSql ListObjects(Criteria<?> query);

    PrecompiledSql count(Criteria<?> query);

    PrecompiledSql exists(Criteria<?> query);

}
