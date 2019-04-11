package com.github.alittlehuang.data.jdbc.sql.mysql57;

import com.github.alittlehuang.data.jdbc.sql.AbstractSqlBuilder;
import com.github.alittlehuang.data.jdbc.sql.SqlBuilderFactory;
import com.github.alittlehuang.data.query.specification.Criteria;

public class Mysql57SqlBuilderFactory implements SqlBuilderFactory {

    @Override
    public <T> SqlBuilder<T> create(Criteria<T> criteria) {
        return new Builder<>(criteria);
    }

    static class Builder<T> extends AbstractSqlBuilder<T> {
        Builder(Criteria<T> criteria) {
            super(criteria);
        }
    }
}
