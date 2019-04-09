package com.github.alittlehuang.data.jdbc.support.sql;

import java.util.List;

public interface PrecompiledSqlForEntity extends PrecompiledSql {

    List<SelectedAttrbute> getSelections();

}
