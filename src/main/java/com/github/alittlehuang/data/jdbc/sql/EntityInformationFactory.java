package com.github.alittlehuang.data.jdbc.sql;

import com.github.alittlehuang.data.metamodel.EntityInformation;

public interface EntityInformationFactory {

    <T> EntityInformation<T, ?> get(Class<T> entityType);

}
