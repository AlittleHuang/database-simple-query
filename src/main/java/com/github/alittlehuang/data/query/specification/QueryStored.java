package com.github.alittlehuang.data.query.specification;


import java.util.Collections;
import java.util.List;

import static com.github.alittlehuang.data.util.Assert.state;

public interface QueryStored<T> {

    List<T> getResultList();

    default T getSingleResult() {
        List<T> list = getResultList();
        state(list.size() <= 1, "found more than one");
        return list.isEmpty() ? null : list.get(0);
    }

    <X> List<X> getObjectList();

    default <X> X getSingleObject(){
        List<X> list = getObjectList();
        state(list.size() <= 1, "found more than one");
        return list.isEmpty() ? null : list.get(0);
    }

    Page<T> getPage(long page, long size);

    Page<T> getPage();

    long count();

    boolean exists();

    Class<T> getJavaType();

    interface Page<X> {

        List<X> getContent();

        long getTotalElement();

        static <Y> Page<Y> empty() {
            return new Page<Y>() {
                @Override
                public List<Y> getContent() {
                    return Collections.emptyList();
                }

                @Override
                public long getTotalElement() {
                    return 0;
                }
            };
        }

    }


}
