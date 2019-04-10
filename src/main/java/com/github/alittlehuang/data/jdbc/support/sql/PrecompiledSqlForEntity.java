package com.github.alittlehuang.data.jdbc.support.sql;

import java.util.List;

public class PrecompiledSqlForEntity<T> extends PrecompiledSql {
    List<SelectedAttribute> selectedAttributes;

    public PrecompiledSqlForEntity(String sql, List<Object> args, List<SelectedAttribute> selectedAttributes) {
        super(sql, args);
        this.selectedAttributes = selectedAttributes;
    }

    public List<SelectedAttribute> getSelections() {
        return selectedAttributes;
    }

}
