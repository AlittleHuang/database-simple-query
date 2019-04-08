package com.github.alittlehuang.data.jdbc.support.sql;

import com.github.alittlehuang.data.jdbc.metamodel.Attribute;

import java.util.List;

public interface SelectedAttrbute {
    Attribute getAttribute();
    List<SelectedAttrbute> getSubSelections();
}
