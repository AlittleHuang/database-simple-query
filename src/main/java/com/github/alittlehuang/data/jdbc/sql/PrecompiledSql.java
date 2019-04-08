package com.github.alittlehuang.data.jdbc.sql;

import java.util.List;

public interface PrecompiledSql {

    String getSql();

    List<Object> getArgs();

}
