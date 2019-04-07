package com.github.alittlehuang.data.jdbc.support;

import com.github.alittlehuang.data.query.support.AbstractQueryStored;
import org.springframework.data.domain.Page;

import java.util.List;

public class JdbcQueryStored<T> extends AbstractQueryStored<T> {

    private DataBasesConfig dataBasesConfig;
    private Class<T> entityType;

    @Override
    public List<T> getResultList() {
        return null;
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
        return 0;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public Class<T> getJavaType() {
        return entityType;
    }
}
