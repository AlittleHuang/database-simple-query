package com.github.alittlehuang.data.jdbc.jpasupport;

import lombok.Getter;
import lombok.experimental.Delegate;

import javax.persistence.metamodel.Type;
import java.lang.reflect.Method;

@Getter
public class SingularAttributeImpl<X, Y> extends Attribute<X, Y> {

    private boolean id;
    private boolean version;
    private boolean optional;
    private Type<Y> type;

    @Delegate
    private Bindable<Y> bindable;

    public SingularAttributeImpl(Method getter) {
        super(getter);
        // TODO
    }

}
