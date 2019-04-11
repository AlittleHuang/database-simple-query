package com.github.alittlehuang.data.jdbc.sql;

import com.github.alittlehuang.data.metamodel.Attribute;

public class SelectedAttribute {

    private Attribute attribute;
    private SelectedAttribute parent;

    public SelectedAttribute(Attribute<?, ?> attribute) {
        this.attribute = attribute;
    }

    public SelectedAttribute(Attribute attribute, SelectedAttribute parent) {
        this.attribute = attribute;
        this.parent = parent;
    }

    public Attribute<Object, Object> getAttribute() {
        //noinspection unchecked
        return attribute;
    }

    public SelectedAttribute getParent() {
        return parent;
    }

    public Attribute<Object, Object> getParentAttribute() {
        return parent == null ? null : parent.getAttribute();
    }
}
