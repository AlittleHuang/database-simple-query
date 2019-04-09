package com.github.alittlehuang.data.jdbc.support.sql;

import com.github.alittlehuang.data.jdbc.metamodel.Attribute;

public class SelectedAttribute<X, Y> {

    private Attribute<X, Y> attribute;
    private SelectedAttribute<?, X> parent;

    public SelectedAttribute(Attribute<X, Y> attribute) {
        this.attribute = attribute;
    }

    public SelectedAttribute(Attribute<X, Y> attribute, SelectedAttribute<?, X> parent) {
        this.attribute = attribute;
        this.parent = parent;
    }

    public Attribute<X,Y> getAttribute() {
        return attribute;
    }

    public SelectedAttribute getParent() {
        return parent;
    }
}
