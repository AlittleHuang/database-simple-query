package com.github.alittlehuang.data.jdbc.metamodel;

import lombok.Getter;
import lombok.experimental.Delegate;

import javax.persistence.metamodel.Type;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Getter
public class SingularAttribute<X, Y> extends Attribute<X, Y> {

    private boolean id;
    private boolean version;
    private boolean optional;
    private Type<Y> type;

    @Delegate
    private Bindable<Y> bindable;


    public SingularAttribute(Field field, Method getter, Method setter) {
        super(field, getter, setter);
    }
}
