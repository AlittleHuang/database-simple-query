package com.github.alittlehuang.data.jdbc.sql;

import com.github.alittlehuang.data.query.specification.Criteria;

import java.util.List;
import java.util.function.BiConsumer;

public interface SqlBuilder {

     PrecompiledSql listResult(Criteria<?> query);

    PrecompiledSql ListObjects(Criteria<?> query);

    PrecompiledSql count(Criteria<?> query);

    PrecompiledSql exists(Criteria<?> query);

    interface PrecompiledSql {

        String getSql();

        List<Object> getArgs();

    }

}
