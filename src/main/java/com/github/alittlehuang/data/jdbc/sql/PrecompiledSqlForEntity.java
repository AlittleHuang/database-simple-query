package com.github.alittlehuang.data.jdbc.sql;

import java.util.List;

public interface PrecompiledSqlForEntity extends PrecompiledSql {

    List<SelectedAttrbute> getSelections();

}
