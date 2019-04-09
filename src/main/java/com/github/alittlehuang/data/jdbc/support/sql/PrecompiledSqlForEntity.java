package com.github.alittlehuang.data.jdbc.support.sql;

import java.util.List;

public class PrecompiledSqlForEntity<T> extends PrecompiledSql {
    List<SelectedAttribute<T, Object>> selectedAttributes;

    public PrecompiledSqlForEntity(String sql, List<Object> args, List<SelectedAttribute<T, Object>> selectedAttributes) {
        super(sql, args);
        this.selectedAttributes = selectedAttributes;
    }

    public List<SelectedAttribute<T, Object>> getSelections() {
        return selectedAttributes;
    }

}
