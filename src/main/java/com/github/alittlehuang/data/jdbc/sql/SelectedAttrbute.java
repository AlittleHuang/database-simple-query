package com.github.alittlehuang.data.jdbc.sql;

import com.github.alittlehuang.data.jdbc.metamodel.Attribute;

import java.util.List;

public interface SelectedAttrbute {
    Attribute getAttribute();
    List<SelectedAttrbute> getSubSelections();
}
