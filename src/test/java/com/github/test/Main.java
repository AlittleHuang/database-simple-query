package com.github.test;

import com.github.alittlehuang.data.jdbc.JdbcQueryStored;
import com.github.alittlehuang.data.jdbc.JdbcQueryStoredConfig;
import com.github.alittlehuang.data.query.specification.Query;
import com.github.alittlehuang.data.query.support.QueryImpl;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;


public class Main {

    @Entity
    @Getter
    @Setter
    public static class User {

        @Id
        @Column(name = "id")
        private String userId;
        @Column(name = "username")
        private String name;
        private String password;

        @ManyToOne
        @JoinColumn(name = "pid")
        private User puser;

    }


    public static void main(String[] args) {

//        System.out.println(SqlBuider.selectFrom(User.class));

//        Mysql57SqlBuilder mysql57SqlBuilder = new Mysql57SqlBuilder();
//        JdbcQueryStoredConfig config = new JdbcQueryStoredConfig(null);
//        JdbcQueryStored<User> stored = new JdbcQueryStored<>(config, User.class);
//        Query<User> query = new QueryImpl<>(stored);
//
//        query.addSelect(User::getPuser).getResultList();

        System.out.println(int.class);
        int x = 0;
        System.out.println(((Object) x).getClass().isPrimitive());

    }

}
