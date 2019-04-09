package com.github.alittlehuang.data.jdbc.support.sql;

import com.github.alittlehuang.data.jdbc.metamodel.Attribute;

import java.util.List;

public class SelectedAttribute<X, Y> {

    private Attribute<X, Y> attribute;
    List<SelectedAttribute<Y, Object>> selectedAttributes;

    public SelectedAttribute(Attribute<X, Y> attribute) {
        this.attribute = attribute;
    }

    public SelectedAttribute(Attribute<X, Y> attribute, List<SelectedAttribute<Y, Object>> selectedAttributes) {
        this.attribute = attribute;
        this.selectedAttributes = selectedAttributes;
    }

    public Attribute<X,Y> getAttribute() {
        return attribute;
    }

    public List<SelectedAttribute<Y, Object>> getSubSelections() {
        return selectedAttributes;
    }
}
