package com.github.alittlehuang.data.query.specification;

public interface Attribute<T> {

    String[] getNames(Class<? extends T> cls);

}
