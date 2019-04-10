package com.github.alittlehuang.data.jpa.support;

import com.github.alittlehuang.data.jdbc.metamodel.EntityInformation;
import com.github.alittlehuang.data.query.page.Page;
import com.github.alittlehuang.data.query.specification.Criteria;
import com.github.alittlehuang.data.query.support.AbstractQueryStored;

import javax.persistence.EntityManager;

public abstract class AbstractJpaStored<T> extends AbstractQueryStored<T> {
    protected static final int DEFAULT_PAGE_SIZE = 10;

    protected EntityManager entityManager;
    protected Class<T> type;

    public AbstractJpaStored(EntityManager entityManager, Class<T> type) {
        this.entityManager = entityManager;
        this.type = type;
    }

    protected EntityInformation<T, ?> getJpaEntityInformation() {
        return EntityInformation.getInstance(type);
    }

    @Override
    public Class<T> getJavaType() {
        return type;
    }

    @Override
    public Page<T> getPage() {
        Criteria<T> criteria = getCriteria();
        Long offset = criteria.getOffset();
        offset = offset == null ? 0 : offset;
        Long maxResults = criteria.getMaxResults();
        maxResults = maxResults == null ? DEFAULT_PAGE_SIZE : maxResults;
        return getPage(( offset / maxResults ), maxResults);
    }
}
